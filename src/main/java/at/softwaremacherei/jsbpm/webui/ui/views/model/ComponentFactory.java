package at.softwaremacherei.jsbpm.webui.ui.views.model;

import at.softwaremacherei.jsbpm.engine.api.instance.AttributeSchema;
import at.softwaremacherei.jsbpm.engine.api.instance.NestedAttributeSchema;
import at.softwaremacherei.jsbpm.engine.api.model.definition.Occurs;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;

public class ComponentFactory {
    
    public static <V, C extends Component & HasValue<?, V>> C createEditorComponent(AttributeSchema attributeSchema) {
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
                        return (C) new EmbeddedGrid((NestedAttributeSchema) attributeSchema);
                    } else {
                        throw new UnsupportedOperationException("Occurs " + ((NestedAttributeSchema) attributeSchema).getOccurs() + " not supported yet");
                    }
                } else {
                    throw new UnsupportedOperationException("FieldType.LIST must use NestedAttributeSchema");
                }
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
    
}
