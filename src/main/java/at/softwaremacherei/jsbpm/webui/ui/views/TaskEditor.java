package at.softwaremacherei.jsbpm.webui.ui.views;


import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.Binder.Binding;
import com.vaadin.flow.data.binder.Binder.BindingBuilder;
import com.vaadin.flow.shared.Registration;

import at.softwaremacherei.jsbpm.engine.api.EngineService.ObjectRequest;
import at.softwaremacherei.jsbpm.engine.api.UserNotFoundException;
import at.softwaremacherei.jsbpm.engine.api.instance.AttributeSchema;
import at.softwaremacherei.jsbpm.engine.api.instance.AttributeStore;
import at.softwaremacherei.jsbpm.engine.api.instance.IsAttributesContainer;
import at.softwaremacherei.jsbpm.engine.api.instance.NestedAttributeSchema;
import at.softwaremacherei.jsbpm.engine.api.instance.ObjectBean;
import at.softwaremacherei.jsbpm.engine.api.instance.ObjectSchema;
import at.softwaremacherei.jsbpm.engine.api.instance.Task;
import at.softwaremacherei.jsbpm.engine.api.instance.Task.AttributeBean;
import at.softwaremacherei.jsbpm.engine.api.instance.TaskInfo;
import at.softwaremacherei.jsbpm.engine.api.instance.TaskNotFoundException;
import at.softwaremacherei.jsbpm.engine.api.instance.TaskOutOfDateException;
import at.softwaremacherei.jsbpm.engine.api.instance.TaskRequest;
import at.softwaremacherei.jsbpm.engine.api.model.definition.Occurs;
import at.softwaremacherei.jsbpm.webui.backend.SbpmEngine;
import at.softwaremacherei.jsbpm.webui.ui.MainLayout;
import at.softwaremacherei.jsbpm.webui.ui.views.model.EmbeddedGrid;
import com.vaadin.flow.component.AbstractCompositeField;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.ComboBox.FetchItemsCallback;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout.FormItem;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.Setter;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.apache.commons.beanutils.PropertyUtils;
import org.springframework.context.annotation.Scope;

@org.springframework.stereotype.Component
@Scope("prototype")
@Route(value = "tasks/:taskId/execute", layout = MainLayout.class)

public class TaskEditor extends VerticalLayout  implements BeforeEnterObserver {

    private final SbpmEngine sbpmEngine;
    private HorizontalLayout toolbar = new HorizontalLayout();
    private Label stateLabel = new Label();

    private Button start = new Button("Start");
    private Button close = new Button("Cancel");

    //only one binder per form allowed
    private final Binder<ObjectBean> binder = new BeanValidationBinder<>(ObjectBean.class);

    private final Div formContent = new Div();

    public TaskEditor(SbpmEngine sbpmEngine) {
        this.sbpmEngine = Objects.requireNonNull(sbpmEngine,"sbpmEngine must be non null");
        addClassName("contact-form");

        add(    stateLabel, 
                formContent,
                toolbar);
    }

     @Override
    public void beforeEnter(BeforeEnterEvent event) {
        try {
            String taskId = event.getRouteParameters().get("taskId").get();
            TaskInfo taskInfo = sbpmEngine.getTasks("")
                    .filter(task -> String.valueOf(task.getId()).equals(taskId))
                    .findFirst()
                    .orElseThrow(() -> new TaskOutOfDateException(taskId));
            Task task = sbpmEngine.getTask(taskInfo);
            
            stateLabel.setText(task.getProcessName()+":"+ task.getStateName());
            
            ObjectSchema objectSchema = task.getTaskDocument().getSchemas().stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("only one business-object allowed"));
            formContent.add(new FormHelper(objectSchema).createForm(task, objectSchema));

            AttributeStore attributeStore = task.createAttributeStore(objectSchema);

            task.getNextStates().stream()
                    .map(nextState -> {
                        Button nextStateButton = new Button(nextState.getName());
                        nextStateButton.addClickListener(click -> {
                            if (binder.validate().isOk()) {
                                try {
                                    TaskRequest taskRequest = task.createTaskRequest(nextState,objectSchema, attributeStore);
                                    sbpmEngine.executeTask(taskRequest);
                                    if (nextState.isEnd()) {
                                        nextStateButton.getUI().ifPresent(ui -> ui.navigate(TasksView.class));
                                    }else{
                                        TaskInfo nextTaskInfo= sbpmEngine.getNextTask(taskInfo);
                                        nextStateButton.getUI().ifPresent(ui -> ui.navigate(TaskEditor.class, new RouteParameters("taskId", String.valueOf(nextTaskInfo.getId()))));
                                    }                                    
                                } catch (UserNotFoundException|TaskNotFoundException|TaskOutOfDateException ex) {
                                    Logger.getLogger(TaskEditor.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
                                }
                            }
                        });
                        return nextStateButton;
                    })
                    .forEach(nextStateButton -> toolbar.add(nextStateButton));
            
            binder.setBean(new ObjectBean(objectSchema, attributeStore));
            
        } catch (TaskNotFoundException | TaskOutOfDateException | UserNotFoundException ex) {
            Logger.getLogger(TaskEditor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public class FormHelper{
        private final ObjectSchema objectSchema;

        public FormHelper(ObjectSchema objectSchema) {
            this.objectSchema = objectSchema;
        }
        
        private FormLayout createForm(Task task, IsAttributesContainer attributesContainer) throws UnsupportedOperationException {
            FormLayout formLayout = new FormLayout();
            for (AttributeSchema attributeSchema : attributesContainer.getAttributes()) {
                Component field = createField(task, attributeSchema);
                formLayout.addFormItem(field, attributeSchema.getName());
            }
            return formLayout;
        }
        
        private Component createField(Task task,AttributeSchema attributeSchema) throws UnsupportedOperationException {
            BindingBuilder<ObjectBean, ?> bindingBuilder = null;
            switch (attributeSchema.getFieldType()) {
                case STRING:
                    bindingBuilder = binder.forField(new TextField(attributeSchema.getName()));
                    break;
                case NUMBER:
                    bindingBuilder = binder.forField(new NumberField(attributeSchema.getName()))
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
                    bindingBuilder = binder.forField(new NumberField(attributeSchema.getName()))
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
                    bindingBuilder = binder.forField(new DatePicker(attributeSchema.getName()));
                    break;
                case TIME:
                    bindingBuilder = binder.forField(new TimePicker(attributeSchema.getName()));
                    break;
                case BOOLEAN:
                    bindingBuilder = binder.forField(new Checkbox(attributeSchema.getName()));
                    break;
                case LIST:
                    bindingBuilder = binder.forField(new EmbeddedGrid((NestedAttributeSchema)attributeSchema));
                    break;
                case NESTED:
                    if(attributeSchema instanceof NestedAttributeSchema){
                        if(Occurs.ONE == ((NestedAttributeSchema)attributeSchema).getOccurs()){
                            if(false){
                                ComboBox<AttributeItem> comboBox = new ComboBox<AttributeItem>();
                                comboBox.setDataProvider(new FetchItemsCallback<AttributeItem>() {
                                    @Override
                                    public Stream<AttributeItem> fetchItems(String filter, int offset, int limit) {
                                        try{
                                        ObjectRequest objectRequest= ObjectRequest.of(objectSchema.getId());
                                        return sbpmEngine.getAutocompleteResponse(task.getTaskInfo(),objectRequest,filter).getAutocompletes().stream()
                                                .map(autocomplete -> new AttributeItem(){
                                                    public String toString(){
                                                        return autocomplete.toString();
                                                    }
                                                });
                                        }catch(Exception ex){
                                            ex.printStackTrace();
                                        }
                                        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                                    }
                                }, new SerializableFunction<String, Integer>() {
                                    @Override
                                    public Integer apply(String filter) {
                                        //determine item count
                                        return 10;
                                    }
                                });

                                //bindingBuilder = binder.forField(new Select<SelectItem>());
                                bindingBuilder = binder.forField(comboBox);
                            }
                            EmbeddedForm embeddedForm = new EmbeddedForm();
                            for (AttributeSchema attribute : ((NestedAttributeSchema)attributeSchema).getAttributes()) {
                                Component field = createField(task, attribute);
                                embeddedForm.addFormItem(field, attribute.getName());
                            }                        
                            bindingBuilder = binder.forField(embeddedForm);
                        }else{
                            bindingBuilder = binder.forField(new EmbeddedGrid((NestedAttributeSchema)attributeSchema));
                        }
                    }else{
                        throw new UnsupportedOperationException("FieldType.NESTED must use NestedAttributeSchema");
                    }
                    break;
                default:
                    throw new UnsupportedOperationException("no component binding for " + attributeSchema.getFieldType());
            }
            if (attributeSchema.isRequired()) {
                bindingBuilder.asRequired();
            }
            Binding<ObjectBean, ?> binding = bind(attributeSchema, bindingBuilder);
            Component field =  (Component) binding.getField();
            field.setId(String.valueOf(attributeSchema.getId()));
            return field;
        }

        @SuppressWarnings("unchecked")
        private <T> Binding<ObjectBean, T> bind(AttributeSchema attributeSchema, BindingBuilder<ObjectBean, T> bindingBuilder) {
            Setter<ObjectBean, T> setter = null;
            if (!attributeSchema.isReadonly()) {
                setter = (ObjectBean bean, T fieldvalue) -> bean.set(attributeSchema,(Serializable) fieldvalue);
            }
            return bindingBuilder.bind((ObjectBean bean) -> (T) bean.get(attributeSchema), setter);
        }
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
    public static abstract class TaskFormEvent extends ComponentEvent<TaskEditor> {
        private Task task;

        protected TaskFormEvent(TaskEditor source, Task task) {
            super(source, false);
            this.task = task;
        }

        public Task getTask() {
            return task;
        }
    }

    public static class SaveEvent extends ComponentEvent<TaskEditor> {

        private final TaskRequest taskRequest;

        public SaveEvent(TaskEditor source,TaskRequest taskRequest) {
            super(source,false);
            this.taskRequest = taskRequest;
        }

        public TaskRequest getTaskRequest() {
            return taskRequest;
        }
        

    }

    public static class DeleteEvent extends TaskFormEvent {
        DeleteEvent(TaskEditor source, Task task) {
            super(source, task);
        }

    }

    public static class CloseEvent extends TaskFormEvent {
        CloseEvent(TaskEditor source) {
            super(source, null);
        }
    }

    public <T extends ComponentEvent<?>> Registration addListener(Class<T> eventType,
            ComponentEventListener<T> listener) {
        return getEventBus().addListener(eventType, listener);
    }

    public static class EmbeddedForm extends AbstractCompositeField<FormLayout, EmbeddedForm, Object>{

        public EmbeddedForm() {
            super(null);
        }

        public FormItem addFormItem(Component field, String label) {
            return getContent().addFormItem(field, label);
        }
        @Override
        protected void setPresentationValue(Object newPresentationValue) {
            System.out.println(getClass()+":"+newPresentationValue);
//            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
        
    }

    public static class AttributeItem    implements Serializable{
    }
}
