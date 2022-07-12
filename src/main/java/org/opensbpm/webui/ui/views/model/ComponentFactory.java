package org.opensbpm.webui.ui.views.model;

import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;

import org.opensbpm.engine.api.EngineService.ObjectRequest;
import org.opensbpm.engine.api.UserNotFoundException;
import org.opensbpm.engine.api.instance.NestedAttributeSchema;
import org.opensbpm.engine.api.instance.ObjectSchema;
import org.opensbpm.engine.api.instance.TaskInfo;
import org.opensbpm.engine.api.instance.TaskNotFoundException;
import org.opensbpm.engine.api.instance.TaskOutOfDateException;
import org.opensbpm.webui.backend.SbpmEngine;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.ComboBox.FetchItemsCallback;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.function.SerializableFunction;
import java.util.stream.Stream;
import org.opensbpm.engine.api.instance.AttributeSchema;
import org.opensbpm.engine.api.instance.AutocompleteResponse.Autocomplete;
import org.opensbpm.engine.api.instance.IsAttributesContainer;
import org.opensbpm.engine.api.instance.ObjectBean;
import org.opensbpm.engine.api.instance.ObjectData;
import org.opensbpm.engine.api.model.FieldType;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.FormItem;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.Binder.Binding;
import com.vaadin.flow.data.binder.Binder.BindingBuilder;
import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.Setter;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashMap;
import org.opensbpm.engine.api.instance.AttributeSchemaVisitor;

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
            FormItem formItem = formLayout.addFormItem(field, attributeSchema.getName());
            //?? formItem.getElement().setAttribute("label-position", "top");
            if (FieldType.LIST == attributeSchema.getFieldType()
                    || FieldType.NESTED == attributeSchema.getFieldType()) {
                formLayout.setColspan(formItem, 2);
            }
        }
        formLayout.setSizeFull();
        return formLayout;
    }

    public <V, C extends Component & HasValue<?, V>> C createField(TaskInfo taskInfo, AttributeSchema attributeSchema) {
        BindingBuilder<ObjectBean, ?> bindingBuilder;
        bindingBuilder = attributeSchema.accept(new AttributeSchemaVisitor<BindingBuilder<ObjectBean, ?>>() {
            @Override
            public BindingBuilder<ObjectBean, ?> visitSimple(AttributeSchema attributeSchema) {
                switch (attributeSchema.getFieldType()) {
                    case STRING:
                        return binder.forField(new TextField());
                    //break;
                    case NUMBER:
                        return binder.forField(new NumberField())
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
                    //break;
                    case DECIMAL:
                        return binder.forField(new NumberField())
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
                    //break;
                    case DATE:
                        return binder.forField(new DatePicker());
                    //break;
                    case TIME:
                        return binder.forField(new TimePicker());
                    //break;
                    case BOOLEAN:
                        return binder.forField(new Checkbox());
                    //break;
                    case BINARY:
                        return binder.forField(new BinaryViewer());
                    //break;
                    case REFERENCE:
                        ObjectSchema referenceSchema = attributeSchema.getAutocompleteReference()
                                .orElseThrow(() -> new IllegalStateException("no AutocompleteReference for attribute '" + attributeSchema.getName() + "'"));
                        AutocompleteQuery autocompleteQuery = new AutocompleteQuery(sbpmEngine, referenceSchema);
                        ComboBox<AttributeItem> comboBox = new ComboBox<>();
                        comboBox.setDataProvider(autocompleteQuery.createDataProvider(taskInfo));
                        return binder.forField(comboBox)
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
                    //break;
//                    case NESTED:
//                    case LIST:
//                        if (attributeSchema instanceof NestedAttributeSchema) {
//                            NestedAttributeSchema nestedSchema = (NestedAttributeSchema) attributeSchema;
//                            if (Occurs.ONE == nestedSchema.getOccurs()) {
//                                return binder.forField(new EmbeddedForm(sbpmEngine, taskInfo, nestedSchema, objectSchema));
//                            } else if (Occurs.UNBOUND == nestedSchema.getOccurs()) {
//                                return binder.forField(new EmbeddedGrid(sbpmEngine, taskInfo, nestedSchema));
//                            } else {
//                                throw new UnsupportedOperationException("Occurs " + nestedSchema.getOccurs() + " not supported yet");
//                            }
//                        } else {
//                            throw new UnsupportedOperationException("FieldType.NESTED must use NestedAttributeSchema");
//                        }
//                        //break;
                    default:
                        throw new UnsupportedOperationException("no component binding for " + attributeSchema.getFieldType());
                }
            }

            @Override
            public BindingBuilder<ObjectBean, ?> visitNested(NestedAttributeSchema attributeSchema) {
                return binder.forField(new EmbeddedForm(sbpmEngine, taskInfo, attributeSchema, objectSchema));
            }

            @Override
            public BindingBuilder<ObjectBean, ?> visitIndexed(NestedAttributeSchema attributeSchema) {
                return binder.forField(new EmbeddedGrid(sbpmEngine, taskInfo, attributeSchema));
            }
        });
        if (attributeSchema.isRequired()) {
            bindingBuilder.asRequired();
        }
        Binding<ObjectBean, ?> binding = bind(attributeSchema, bindingBuilder);
        Component field = (Component) binding.getField();
        field.setId(String.valueOf(attributeSchema.getId()));
        return (C) field;
    }

    @SuppressWarnings("unchecked")
    private <T> Binding<ObjectBean, T> bind(AttributeSchema attributeSchema, BindingBuilder<ObjectBean, T> bindingBuilder) {
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
