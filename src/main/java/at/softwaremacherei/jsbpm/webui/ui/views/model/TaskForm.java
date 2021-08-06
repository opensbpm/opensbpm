package at.softwaremacherei.jsbpm.webui.ui.views.model;

import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.Binder.Binding;
import com.vaadin.flow.data.binder.Binder.BindingBuilder;
import com.vaadin.flow.shared.Registration;

import at.softwaremacherei.jsbpm.engine.api.EngineService.ObjectRequest;
import at.softwaremacherei.jsbpm.engine.api.instance.AttributeSchema;
import at.softwaremacherei.jsbpm.engine.api.instance.IsAttributesContainer;
import at.softwaremacherei.jsbpm.engine.api.instance.NestedAttributeSchema;
import at.softwaremacherei.jsbpm.engine.api.instance.ObjectData;
import at.softwaremacherei.jsbpm.engine.api.instance.ObjectSchema;
import at.softwaremacherei.jsbpm.engine.api.instance.Task;
import at.softwaremacherei.jsbpm.engine.api.instance.Task.AttributeBean;
import at.softwaremacherei.jsbpm.engine.api.instance.TaskRequest;
import at.softwaremacherei.jsbpm.engine.api.model.definition.Occurs;
import at.softwaremacherei.jsbpm.webui.backend.SbpmEngine;
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
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.stream.Stream;

public class TaskForm extends VerticalLayout {

    private final SbpmEngine sbpmEngine;
    private HorizontalLayout toolbar = new HorizontalLayout();
    private Label stateLabel = new Label();

    private Button start = new Button("Start");
    private Button close = new Button("Cancel");

    //only one binder per form allowed
    private final Binder<ObjectData> binder = new BeanValidationBinder<>(ObjectData.class);

    private final Div formContent = new Div();

    public TaskForm(SbpmEngine sbpmEngine) {
        this.sbpmEngine = Objects.requireNonNull(sbpmEngine,"sbpmEngine must be non null");
        addClassName("contact-form");

        add(    stateLabel, 
                formContent,
                toolbar);
    }

    public void setTask(Task task) {
        if (Optional.ofNullable(task).isPresent()) {
           stateLabel.setText(task.getProcessName()+":"+ task.getStateName());
           
            ObjectSchema objectSchema = task.getTaskDocument().getSchemas().stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("only one bsinnessobject allowed"));
            formContent.add(new FormHelper(objectSchema).createForm(task, objectSchema));

            task.getNextStates().stream()
                    .map(nextState -> {
                        Button nextStateButton = new Button(nextState.getName());
                        nextStateButton.addClickListener(click -> {
                            if (binder.validate().isOk()) {
                                fireEvent(new SaveEvent(this, task.createTaskRequest(nextState)));
                            }
                        });
                        return nextStateButton;
            })
            .forEach(nextStateButton -> toolbar.add(nextStateButton));
            
            binder.setBean(task.getTaskDocument().getData(objectSchema));
            
                
        }
        //binder.readBean(task);
    }


    public class FormHelper{
        private final ObjectSchema objectSchema;

        public FormHelper(ObjectSchema objectSchema) {
            this.objectSchema = objectSchema;
        }
        
        private EmbeddedForm createForm(Task task, IsAttributesContainer attributesContainer) throws UnsupportedOperationException {
            EmbeddedForm formLayout = new EmbeddedForm();
            for (AttributeSchema attributeSchema : attributesContainer.getAttributes()) {
                AttributeBean attributeBean = task.getTaskDocument().getAttribute(objectSchema, attributeSchema);
                Component field = createField(task, attributeBean);
                formLayout.addFormItem(field, attributeSchema.getName());
            }
            return formLayout;
        }
        
        private Component createField(Task task,AttributeBean attributeBean) throws UnsupportedOperationException {
        BindingBuilder<ObjectData, ?> bindingBuilder = null;
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
                bindingBuilder = binder.forField(new EmbeddedGrid((NestedAttributeSchema)attributeBean.getAttributeSchema()));
                break;
            case NESTED:
                if(attributeBean.getAttributeSchema() instanceof NestedAttributeSchema){
                    if(Occurs.ONE == ((NestedAttributeSchema)attributeBean.getAttributeSchema()).getOccurs()){
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
                        bindingBuilder = binder.forField(
                                new FormHelper(objectSchema).createForm(task,((NestedAttributeSchema)attributeBean.getAttributeSchema()))
                        );
                    }else{
                        bindingBuilder = binder.forField(new EmbeddedGrid((NestedAttributeSchema)attributeBean.getAttributeSchema()));
                    }
                }else{
                    throw new UnsupportedOperationException("FieldType.NESTED must use NestedAttributeSchema");
                }
                break;
            default:
                throw new UnsupportedOperationException("no component binding for " + attributeBean.getFieldType());
        }
        if (attributeBean.isRequired()) {
            bindingBuilder.asRequired();
        }
        Binding<ObjectData, ?> binding = bind(attributeBean, bindingBuilder);
        Component field =  (Component) binding.getField();
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

    public static class EmbeddedForm extends AbstractCompositeField<FormLayout, EmbeddedForm, Object>{

        public EmbeddedForm() {
            super(null);
        }

        public FormItem addFormItem(Component field, String label) {
            return getContent().addFormItem(field, label);
        }
        @Override
        protected void setPresentationValue(Object arg0) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
        
    }

    public static class AttributeItem    implements Serializable{
    }
}
