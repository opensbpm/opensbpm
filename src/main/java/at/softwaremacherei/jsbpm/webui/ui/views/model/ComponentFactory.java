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
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ComponentFactory {

    private final SbpmEngine sbpmEngine;

    public ComponentFactory(SbpmEngine sbpmEngine) {
        this.sbpmEngine = sbpmEngine;
    }

    public <V, C extends Component & HasValue<?, V>> C createEditorComponent(TaskInfo taskInfo, AttributeSchema attributeSchema) {
        switch (attributeSchema.getFieldType()) {
            case STRING:
                return (C) new TextField();
            case NUMBER:
                return (C) new NumberField();
            case DECIMAL:
                return (C) new NumberField();
            case DATE:
                return (C) new DatePicker();
            case TIME:
                return (C) new TimePicker();
            case BOOLEAN:
                return (C) new Checkbox();
            case LIST:
                if (attributeSchema instanceof NestedAttributeSchema) {
                    if (Occurs.UNBOUND == ((NestedAttributeSchema) attributeSchema).getOccurs()) {
                        return (C) new EmbeddedGrid(sbpmEngine, taskInfo, (NestedAttributeSchema) attributeSchema);
                    } else {
                        throw new UnsupportedOperationException("Occurs " + ((NestedAttributeSchema) attributeSchema).getOccurs() + " not supported yet");
                    }
                } else {
                    throw new UnsupportedOperationException("FieldType.LIST must use NestedAttributeSchema");
                }
            case REFERENCE:
                ObjectSchema referenceSchema = attributeSchema.getAutocompleteReference()
                        .orElseThrow(() -> new IllegalStateException("no AutocompleteReference for attribute '" + attributeSchema.getName() + "'"));
                AutocompleteQuery autocompleteQuery = new AutocompleteQuery(referenceSchema);
                ComboBox<AttributeItem> comboBox = new ComboBox<AttributeItem>();
                comboBox.setDataProvider(new FetchItemsCallback<AttributeItem>() {
                    @Override
                    public Stream<AttributeItem> fetchItems(String filter, int offset, int limit) {
                        try {
                            return autocompleteQuery.query(taskInfo, filter)
                                    .map(autocomplete -> new AttributeItem() {
                                public String toString() {
                                    return autocomplete.getObjectData().getDisplayName()
                                            .orElse(autocomplete.getObjectData().toString());
                                }
                            });
                        } catch (UserNotFoundException | TaskNotFoundException | TaskOutOfDateException ex) {
                            //Logger.getLogger(ComponentFactory.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
                            throw new IllegalStateException(ex);
                        }
                    }
                }, new SerializableFunction<String, Integer>() {
                    @Override
                    public Integer apply(String filter) {
                        try {
                            //determine item count
                            return new Long(autocompleteQuery.query(taskInfo, filter).count()).intValue();
                        } catch (UserNotFoundException | TaskNotFoundException | TaskOutOfDateException ex) {
                            //Logger.getLogger(ComponentFactory.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
                            throw new IllegalStateException(ex);
                        }
                    }
                });

                return (C) comboBox;
            case NESTED:
                //                    if (attributeSchema instanceof NestedAttributeSchema) {
                //                        if (Occurs.ONE == ((NestedAttributeSchema) attributeSchema).getOccurs()) {
                //                            FormLayout formLayout = new FormHelper(null).createForm(task, ((NestedAttributeSchema) attributeSchema));
                //                            return (C) new EmbeddedForm(formLayout);
                //                        } else if (Occurs.UNBOUND == ((NestedAttributeSchema) attributeSchema).getOccurs()) {
                //                                bindingBuilder = binder.forField(new EmbeddedGrid((NestedAttributeSchema) attributeSchema));
                //                        }else{
                //                            throw new UnsupportedOperationException("Occurs "+ ((NestedAttributeSchema)attributeSchema).getOccurs() +" not supported yet");
                //                        }
                //                        } else {
                throw new UnsupportedOperationException("FieldType.NESTED must use NestedAttributeSchema");
            default:
                throw new UnsupportedOperationException("no component binding for " + attributeSchema.getFieldType());
        }
    }

    public static class AttributeItem implements Serializable {
    }

    public class AutocompleteQuery {

        private final ObjectSchema objectSchema;

        public AutocompleteQuery(ObjectSchema objectSchema) {
            this.objectSchema = objectSchema;
        }

        public Stream<Autocomplete> query(TaskInfo taskInfo, String filter) throws UserNotFoundException, TaskNotFoundException, TaskOutOfDateException {
            ObjectRequest objectRequest = ObjectRequest.of(objectSchema.getId());
            return sbpmEngine.getAutocompleteResponse(taskInfo, objectRequest, filter).getAutocompletes().stream();
        }

    }
}
