package org.opensbpm.vaadinui.ui.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.server.StreamResource;

@Tag(value = "object")
public class ContentViewer extends Component implements HasSize {

    public void setValue(String contentType, StreamResource resource) {
        getElement().setAttribute("type", contentType);
        getElement().setAttribute("data", resource);
    }

}
