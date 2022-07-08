package org.opensbpm.webui.ui.views.model;

import com.vaadin.flow.component.formlayout.FormLayout;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.opensbpm.engine.api.instance.AttributeSchema;
import org.opensbpm.engine.api.instance.NestedAttributeSchema;
import org.opensbpm.engine.api.instance.ObjectSchema;
import org.opensbpm.engine.api.instance.TaskInfo;
import org.opensbpm.engine.api.model.FieldType;
import org.opensbpm.engine.api.model.definition.Occurs;
import org.opensbpm.webui.backend.SbpmEngine;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@SpringBootTest
public class ComponentFactoryTest {

    @MockBean
    private SbpmEngine sbpmEngine;

    @Test
    public void testCreateForm() {
        //arrange
        long id=0l;
        
        AttributeSchema reference;
        ObjectSchema objectSchema = ObjectSchema.of(id++, "Test", Arrays.asList(
                AttributeSchema.of(id++, "string", FieldType.STRING),
                AttributeSchema.of(id++, "number", FieldType.NUMBER),
                AttributeSchema.of(id++, "decimal", FieldType.DECIMAL),
                //                AttributeSchema.of(id++, "date", FieldType.DATE),//ui needed
                AttributeSchema.of(id++, "time", FieldType.TIME),
                AttributeSchema.of(id++, "boolean", FieldType.BOOLEAN),
                AttributeSchema.of(id++, "binary", FieldType.BINARY),
                reference = AttributeSchema.of(id++, "reference", FieldType.REFERENCE),                
                new NestedAttributeSchema(id++, "nested", Occurs.ONE, Arrays.asList(
                        AttributeSchema.of(id++, "string", FieldType.STRING)
                )),
                new NestedAttributeSchema(id++, "list", Occurs.UNBOUND, Arrays.asList(
                        AttributeSchema.of(id++, "string", FieldType.STRING)
                ))
        ));
        reference.setAutocompleteReference(new ObjectSchema());

        TaskInfo taskInfo = new TaskInfo();
        ComponentFactory componentFactory = new ComponentFactory(sbpmEngine, objectSchema);

        //act
        FormLayout form = componentFactory.createForm(taskInfo, objectSchema);

        //assert
        assertThat(form.getElement().getChildren().count(), is(9l));

    }

}
