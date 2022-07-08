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
        ObjectSchema objectSchema = ObjectSchema.of(1l, "Test", Arrays.asList(
                AttributeSchema.of(2l, "string", FieldType.STRING),
                AttributeSchema.of(3l, "number", FieldType.NUMBER),
                AttributeSchema.of(4l, "decimal", FieldType.DECIMAL),
                //                AttributeSchema.of(5l, "date", FieldType.DATE),
                //                AttributeSchema.of(6l, "time", FieldType.TIME),
                AttributeSchema.of(7l, "boolean", FieldType.BOOLEAN),
                AttributeSchema.of(8l, "binary", FieldType.BINARY),
                //AttributeSchema.of(9l, "list", FieldType.LIST),
                new NestedAttributeSchema(9l, "lis", Occurs.UNBOUND, Arrays.asList(
                        AttributeSchema.of(92l, "string", FieldType.STRING)
                )),
//                AttributeSchema.of(10l, "reference", FieldType.REFERENCE),
                new NestedAttributeSchema(11l, "nested", Occurs.ONE, Arrays.asList(
                        AttributeSchema.of(12l, "string", FieldType.STRING)
                ))
        ));

        TaskInfo taskInfo = new TaskInfo();
        ComponentFactory componentFactory = new ComponentFactory(sbpmEngine, objectSchema);

        //act
        FormLayout form = componentFactory.createForm(taskInfo, objectSchema);

        //assert
        assertThat(form.getElement().getChildren().count(), is(7l));

    }

}
