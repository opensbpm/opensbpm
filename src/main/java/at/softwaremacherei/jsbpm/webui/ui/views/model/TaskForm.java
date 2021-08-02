package at.softwaremacherei.jsbpm.webui.ui.views.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.Binder.Binding;
import com.vaadin.flow.data.binder.Binder.BindingBuilder;
import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.Setter;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;
import com.vaadin.flow.shared.Registration;

import at.softwaremacherei.jsbpm.engine.api.instance.AttributeSchema;
import at.softwaremacherei.jsbpm.engine.api.instance.ObjectData;
import at.softwaremacherei.jsbpm.engine.api.instance.ObjectSchema;
import at.softwaremacherei.jsbpm.engine.api.instance.Task;
import at.softwaremacherei.jsbpm.engine.api.instance.Task.AttributeBean;
import at.softwaremacherei.jsbpm.engine.api.instance.TaskRequest;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.select.Select;
import java.util.List;
import java.util.stream.Collectors;

public class TaskForm extends FormLayout {

    private HorizontalLayout toolbar = new HorizontalLayout();
    private TextField processName = new TextField("ProcessName");
    private TextArea stateName = new TextArea("StateName");

    private Button start = new Button("Start");
    private Button close = new Button("Cancel");

    private Binder<Task> binder = new BeanValidationBinder<>(Task.class);
    private Task task;

    private final Collection<Binder<ObjectData>> binders = new ArrayList<>();
    public TaskForm() {
        addClassName("contact-form");

        binder.bindInstanceFields(this);

        add(
                processName,
                stateName,
                toolbar);
    }

    public void setTask(Task task) {
        this.task = task;

        if (Optional.ofNullable(task).isPresent()) {
            for (ObjectSchema objectSchema : task.getTaskDocument().getSchemas()) {
                Binder<ObjectData> binder = new Binder<>();
                for (AttributeSchema attributeSchema : objectSchema.getAttributes()) {
                    AttributeBean attributeBean = task.getTaskDocument().getAttribute(objectSchema, attributeSchema);
                    AbstractField<?, ?> field = createField(binder, attributeBean);
                    add(field);
                }
                binder.setBean(task.getTaskDocument().getData(objectSchema));
                
                binders.add(binder);
            }

            task.getNextStates().stream()
                    .map(nextState -> {
                        Button nextStateButton = new Button(nextState.getName());
                        nextStateButton.addClickListener(click -> {
                            List<Binder<ObjectData>> errorBinders = binders.stream()
                                .filter((Binder<ObjectData> binder) -> binder.validate().hasErrors())
                                .collect(Collectors.toList());
                            if (errorBinders.isEmpty()) {
                                fireEvent(new SaveEvent(this, task.createTaskRequest(nextState)));
                            }
                        });
                        return nextStateButton;
            })
                    .forEach(nextStateButton -> toolbar.add(nextStateButton));
        }
        //binder.readBean(task);
    }

    private AbstractField<?, ?> createField(Binder<ObjectData> binder, AttributeBean attributeBean) throws UnsupportedOperationException {
        final BindingBuilder<ObjectData, ?> bindingBuilder;
        switch (attributeBean.getFieldType()) {
            case STRING:
                bindingBuilder = binder.forField(new TextField(attributeBean.getName()));
                break;
            case NUMBER:
                bindingBuilder = binder.forField(new NumberField(attributeBean.getName()))
                        .withConverter(new Converter<Double, Integer>() {

                            @Override
                            public Result<Integer> convertToModel(Double value, ValueContext context) {
                                return Result.ok(value == null ? null : value.intValue());
                            }

                            @Override
                            public Double convertToPresentation(Integer value, ValueContext context) {
                                return value == null ? null : value.doubleValue();
                            }
                        });
                break;
            case DECIMAL:
                bindingBuilder = binder.forField(new NumberField(attributeBean.getName()))
                        .withConverter(new Converter<Double, BigDecimal>() {

                            @Override
                            public Result<BigDecimal> convertToModel(Double value, ValueContext context) {
                                return Result.ok(value == null ? null : BigDecimal.valueOf(value));
                            }

                            @Override
                            public Double convertToPresentation(BigDecimal value, ValueContext context) {
                                return value == null ? null : value.doubleValue();
                            }
                        });
                break;
            case DATE:
                bindingBuilder = binder.forField(new DatePicker(attributeBean.getName()));
                break;
            case TIME:
                bindingBuilder = binder.forField(new TimePicker(attributeBean.getName()));
                break;
            case BOOLEAN:
                bindingBuilder = binder.forField(new Checkbox(attributeBean.getName()));
                break;
            case LIST:
                bindingBuilder = binder.forField(new Select<>(attributeBean.getName()));
                break;
            default:
                throw new UnsupportedOperationException("no component binding for " + attributeBean.getFieldType());
        }
        if (attributeBean.isRequired()) {
            bindingBuilder.asRequired();
        }
        Binding<ObjectData, ?> binding = bind(attributeBean, bindingBuilder);
        final AbstractField<?, ?> field = (AbstractField<?, ?>) binding.getField();
        field.setId(String.valueOf(attributeBean.getAttributeSchema().getId()));
        return field;
    }

    @SuppressWarnings("unchecked")
    private <T> Binding<ObjectData, T> bind(AttributeBean attributeBean, BindingBuilder<ObjectData, T> bindingBuilder) {
        Setter<ObjectData, T> setter = null;
        if (!attributeBean.isReadonly()) {
            setter = (ObjectData bean, T fieldvalue) -> attributeBean.setValue((Serializable) fieldvalue);
        }
        return bindingBuilder.bind((ObjectData bean) -> (T) attributeBean.getValue(), setter);
    }
    
    
    private Component createButtonsLayout() {
        start.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        close.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        start.addClickShortcut(Key.ENTER);
        close.addClickShortcut(Key.ESCAPE);

        //start.addClickListener(click -> validateAndSave());
        // delete.addClickListener(click -> fireEvent(new DeleteEvent(this, modelInfo)));
        close.addClickListener(click -> fireEvent(new CloseEvent(this)));

        binder.addStatusChangeListener(evt -> start.setEnabled(binder.isValid()));

        return new HorizontalLayout(start, close);
    }

    // Events
    public static abstract class TaskFormEvent extends ComponentEvent<TaskForm> {
        private Task task;

        protected TaskFormEvent(TaskForm source, Task task) {
            super(source, false);
            this.task = task;
        }

        public Task getTask() {
            return task;
        }
    }

    public static class SaveEvent extends ComponentEvent<TaskForm> {

        private final TaskRequest taskRequest;

        public SaveEvent(TaskForm source,TaskRequest taskRequest) {
            super(source,false);
            this.taskRequest = taskRequest;
        }

        public TaskRequest getTaskRequest() {
            return taskRequest;
        }
        

    }

    public static class DeleteEvent extends TaskFormEvent {
        DeleteEvent(TaskForm source, Task task) {
            super(source, task);
        }

    }

    public static class CloseEvent extends TaskFormEvent {
        CloseEvent(TaskForm source) {
            super(source, null);
        }
    }

    public <T extends ComponentEvent<?>> Registration addListener(Class<T> eventType,
            ComponentEventListener<T> listener) {
        return getEventBus().addListener(eventType, listener);
    }
}
