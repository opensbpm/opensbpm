package org.opensbpm.webui.ui.views.model;

import com.vaadin.flow.component.formlayout.FormLayout;
import org.junit.jupiter.api.Test;
import org.opensbpm.engine.api.instance.AttributeSchema;
import org.opensbpm.engine.api.instance.IndexedAttributeSchema;
import org.opensbpm.engine.api.instance.NestedAttributeSchema;
import org.opensbpm.engine.api.instance.ObjectSchema;
import org.opensbpm.engine.api.instance.SimpleAttributeSchema;
import org.opensbpm.engine.api.instance.TaskInfo;
import org.opensbpm.engine.api.model.FieldType;
import org.opensbpm.webui.backend.SbpmEngine;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@SpringBootTest
@DirtiesContext
public class ComponentFactoryTest {

    @MockBean
    private SbpmEngine sbpmEngine;

    @Test
    public void testCreateForm() {
        //arrange
        long id = 0l;

        SimpleAttributeSchema reference;
        ObjectSchema objectSchema = ObjectSchema.of(id++, "Test", asList(
                SimpleAttributeSchema.of(id++, "string", FieldType.STRING),
                SimpleAttributeSchema.of(id++, "number", FieldType.NUMBER),
                SimpleAttributeSchema.of(id++, "decimal", FieldType.DECIMAL),
                //                SimpleAttributeSchema.of(id++, "date", FieldType.DATE),//ui needed
                SimpleAttributeSchema.of(id++, "time", FieldType.TIME),
                SimpleAttributeSchema.of(id++, "boolean", FieldType.BOOLEAN),
                SimpleAttributeSchema.of(id++, "binary", FieldType.BINARY),
                reference = SimpleAttributeSchema.of(id++, "reference", FieldType.REFERENCE),
                NestedAttributeSchema.createNested(id++, "nested", asList(
                        SimpleAttributeSchema.of(id++, "string", FieldType.STRING)
                )),
                IndexedAttributeSchema.create(id++, "list", asList(
                        SimpleAttributeSchema.of(id++, "string", FieldType.STRING)
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
