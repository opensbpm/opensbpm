package at.softwaremacherei.jsbpm.webui.ui.views.model;

import at.softwaremacherei.jsbpm.engine.api.model.Binary;
import at.softwaremacherei.jsbpm.webui.ui.components.ContentViewer;
import com.vaadin.flow.component.AbstractCompositeField;
import com.vaadin.flow.server.StreamResource;
import java.io.ByteArrayInputStream;

public class BinaryViewer extends AbstractCompositeField<ContentViewer, BinaryViewer, Binary> {

    public BinaryViewer() {
        super(null);
        getContent().setSizeFull();
    }

    @Override
    protected void setPresentationValue(Binary binary) {
        StreamResource resource = new StreamResource(binary.toString(), () -> new ByteArrayInputStream(binary.getValue()));
        resource.setContentType(binary.getMimeType());
        getContent().setValue(binary.getMimeType(), resource);
    }

}
