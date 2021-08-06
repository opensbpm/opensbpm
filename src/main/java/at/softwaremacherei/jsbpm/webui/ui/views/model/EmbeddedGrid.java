package at.softwaremacherei.jsbpm.webui.ui.views.model;

import at.softwaremacherei.jsbpm.engine.api.instance.AttributeSchema;
import at.softwaremacherei.jsbpm.engine.api.instance.NestedAttributeSchema;
import at.softwaremacherei.jsbpm.engine.api.instance.ObjectData;
import com.vaadin.flow.component.AbstractCompositeField;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.data.renderer.TemplateRenderer;
import com.vaadin.flow.function.ValueProvider;

public class EmbeddedGrid extends AbstractCompositeField<Grid, EmbeddedGrid, ObjectData> {

    private final NestedAttributeSchema nestedAttributeSchema;

    public EmbeddedGrid(NestedAttributeSchema nestedAttributeSchema) {
        super(null);
        this.nestedAttributeSchema = nestedAttributeSchema;

        for (AttributeSchema attribute : nestedAttributeSchema.getAttributes()) {
            getContent().addColumn(TemplateRenderer.of(attribute.getName()));
        }

    }

    @Override
    protected Grid initContent() {
        return new Grid();
    }

    @Override
    protected void setPresentationValue(ObjectData objectData) {
        System.out.println(""+ objectData);
        //getContent().setItems(items);
    }

}
