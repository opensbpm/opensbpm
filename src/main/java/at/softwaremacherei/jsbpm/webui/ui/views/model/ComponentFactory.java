package at.softwaremacherei.jsbpm.webui.ui.views.model;

import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;

import at.softwaremacherei.jsbpm.engine.api.EngineService.ObjectRequest;
import at.softwaremacherei.jsbpm.engine.api.UserNotFoundException;
import at.softwaremacherei.jsbpm.engine.api.instance.NestedAttributeSchema;
import at.softwaremacherei.jsbpm.engine.api.instance.ObjectSchema;
import at.softwaremacherei.jsbpm.engine.api.instance.TaskInfo;
import at.softwaremacherei.jsbpm.engine.api.instance.TaskNotFoundException;
import at.softwaremacherei.jsbpm.engine.api.instance.TaskOutOfDateException;
import at.softwaremacherei.jsbpm.engine.api.model.definition.Occurs;
import at.softwaremacherei.jsbpm.webui.backend.SbpmEngine;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.ComboBox.FetchItemsCallback;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.function.SerializableFunction;
import java.util.stream.Stream;
import at.softwaremacherei.jsbpm.engine.api.instance.AttributeSchema;
import at.softwaremacherei.jsbpm.engine.api.instance.AutocompleteResponse.Autocomplete;
import at.softwaremacherei.jsbpm.engine.api.instance.IsAttributesContainer;
import at.softwaremacherei.jsbpm.engine.api.instance.ObjectBean;
import at.softwaremacherei.jsbpm.engine.api.instance.ObjectData;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.Setter;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashMap;

public class ComponentFactory {

    private final SbpmEngine sbpmEngine;
    private final ObjectSchema objectSchema;
    private final Binder<ObjectBean> binder = new BeanValidationBinder<>(ObjectBean.class);

    public ComponentFactory(SbpmEngine sbpmEngine, ObjectSchema objectSchema) {
        this.sbpmEngine = sbpmEngine;
        this.objectSchema = objectSchema;
    }

    public Binder<ObjectBean> getBinder() {
        return binder;
    }

    public FormLayout createForm(TaskInfo taskInfo, IsAttributesContainer attributesContainer) {
        FormLayout formLayout = new FormLayout();
        for (AttributeSchema attributeSchema : attributesContainer.getAttributes()) {
            Component field = createField(taskInfo, attributeSchema);
            formLayout.addFormItem(field, attributeSchema.getName());
        }
        formLayout.setSizeFull();
        return formLayout;
    }

    public <V, C extends Component & HasValue<?, V>> C createField(TaskInfo taskInfo, AttributeSchema attributeSchema) {
        Binder.BindingBuilder<ObjectBean, ?> bindingBuilder = null;
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
            case BINARY:
                bindingBuilder = binder.forField(new BinaryViewer(/*attributeSchema.getName()*/));
                break;
            case LIST:
                if (attributeSchema instanceof NestedAttributeSchema) {
                    if (Occurs.UNBOUND == ((NestedAttributeSchema) attributeSchema).getOccurs()) {
                        bindingBuilder = binder.forField(new EmbeddedGrid(sbpmEngine, taskInfo, (NestedAttributeSchema) attributeSchema));
                    } else {
                        throw new UnsupportedOperationException("Occurs " + ((NestedAttributeSchema) attributeSchema).getOccurs() + " not supported yet");
                    }
                } else {
                    throw new UnsupportedOperationException("FieldType.LIST must use NestedAttributeSchema");
                }
                break;
            case REFERENCE:
                ObjectSchema referenceSchema = attributeSchema.getAutocompleteReference()
                        .orElseThrow(() -> new IllegalStateException("no AutocompleteReference for attribute '" + attributeSchema.getName() + "'"));
                AutocompleteQuery autocompleteQuery = new AutocompleteQuery(sbpmEngine, referenceSchema);
                ComboBox<AttributeItem> comboBox = new ComboBox<>();
                comboBox.setDataProvider(autocompleteQuery.createDataProvider(taskInfo));
                bindingBuilder = binder.forField(comboBox)
                        .withConverter(new Converter<AttributeItem, HashMap<Long, Serializable>>() {
                            @Override
                            public Result<HashMap<Long, Serializable>> convertToModel(AttributeItem value, ValueContext context) {
                                return Result.ok(value == null ? null : value.toSourceMap());
                            }

                            @Override
                            public AttributeItem convertToPresentation(HashMap<Long, Serializable> value, ValueContext context) {
                                return null;
                            }
                        });
                break;
            case NESTED:
                if (attributeSchema instanceof NestedAttributeSchema) {
                    if (Occurs.ONE == ((NestedAttributeSchema) attributeSchema).getOccurs()) {
                        bindingBuilder = binder.forField(new EmbeddedForm(sbpmEngine, taskInfo, (NestedAttributeSchema) attributeSchema, objectSchema));
                    } else if (Occurs.UNBOUND == ((NestedAttributeSchema) attributeSchema).getOccurs()) {
                        bindingBuilder = binder.forField(new EmbeddedGrid(sbpmEngine, taskInfo, (NestedAttributeSchema) attributeSchema));
                    } else {
                        throw new UnsupportedOperationException("Occurs " + ((NestedAttributeSchema) attributeSchema).getOccurs() + " not supported yet");
                    }
                } else {
                    throw new UnsupportedOperationException("FieldType.NESTED must use NestedAttributeSchema");
                }
                break;
            default:
                throw new UnsupportedOperationException("no component binding for " + attributeSchema.getFieldType());
        }
        if (attributeSchema.isRequired()) {
            bindingBuilder.asRequired();
        }
        Binder.Binding<ObjectBean, ?> binding = bind(attributeSchema, bindingBuilder);
        Component field = (Component) binding.getField();
        field.setId(String.valueOf(attributeSchema.getId()));
        return (C) field;
    }

    @SuppressWarnings("unchecked")
    private <T> Binder.Binding<ObjectBean, T> bind(AttributeSchema attributeSchema, Binder.BindingBuilder<ObjectBean, T> bindingBuilder) {
        Setter<ObjectBean, T> setter = null;
        if (!attributeSchema.isReadonly()) {
            setter = (ObjectBean bean, T fieldvalue) -> bean.set(attributeSchema, (Serializable) fieldvalue);
        }
        return bindingBuilder.bind(bean -> (T) bean.get(attributeSchema), setter);
    }

    public static class AutocompleteQuery {

        private final SbpmEngine sbpmEngine;
        private final ObjectSchema objectSchema;

        public AutocompleteQuery(SbpmEngine sbpmEngine, ObjectSchema objectSchema) {
            this.sbpmEngine = sbpmEngine;
            this.objectSchema = objectSchema;
        }

        CallbackDataProvider<AttributeItem, String> createDataProvider(TaskInfo taskInfo) {
            return new CallbackDataProvider<>(
                    q -> createFetchItems(taskInfo).fetchItems(q.getFilter().orElse(""),
                            q.getOffset(), q.getLimit()),
                    q -> createSizeCallback(taskInfo).apply(q.getFilter().orElse(""))
            );
        }

        private FetchItemsCallback<AttributeItem> createFetchItems(TaskInfo taskInfo) {
            return (String filter, int offset, int limit) -> {
                try {
                    return query(taskInfo, filter).map(autocomplete -> new AttributeItem(autocomplete.getObjectData()));
                } catch (UserNotFoundException | TaskNotFoundException | TaskOutOfDateException ex) {
                    //Logger.getLogger(ComponentFactory.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
                    throw new IllegalStateException(ex);
                }
            };
        }

        private SerializableFunction<String, Integer> createSizeCallback(TaskInfo taskInfo) {
            return filter -> {
                try {
                    //determine item count
                    return (int) query(taskInfo, filter).count();
                } catch (UserNotFoundException | TaskNotFoundException | TaskOutOfDateException ex) {
                    //Logger.getLogger(ComponentFactory.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
                    throw new IllegalStateException(ex);
                }
            };
        }

        private Stream<Autocomplete> query(TaskInfo taskInfo, String filter) throws UserNotFoundException, TaskNotFoundException, TaskOutOfDateException {
            ObjectRequest objectRequest = ObjectRequest.of(objectSchema.getId());
            return sbpmEngine.getAutocompleteResponse(taskInfo, objectRequest, filter).getAutocompletes().stream();
        }

    }

    public static class AttributeItem implements Serializable {

        private final ObjectData objectData;

        public AttributeItem(ObjectData objectData) {
            this.objectData = objectData;
        }

        public HashMap<Long, Serializable> toSourceMap() {
            return (HashMap<Long, Serializable>) objectData.getData();
        }

        @Override
        public String toString() {
            return objectData.getDisplayName().orElse(objectData.toString());
        }
    }

}
