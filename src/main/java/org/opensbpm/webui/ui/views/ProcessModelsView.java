/** *****************************************************************************
 * Copyright (C) 2020 Stefan Sedelmaier
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *****************************************************************************
 */
package org.opensbpm.webui.ui.views;

import org.opensbpm.engine.api.model.ProcessModelInfo;
import org.opensbpm.engine.api.model.ProcessModelState;
import org.opensbpm.engine.api.model.definition.ProcessDefinition;
import org.opensbpm.engine.api.ModelService;
import org.opensbpm.engine.xmlmodel.ProcessModel;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.util.EnumSet;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBException;
import org.opensbpm.webui.ui.MainLayout;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
@Route(value = "models", layout = MainLayout.class)
@PageTitle("Models | OpenSBPM Vaadin Demo")
public class ProcessModelsView extends VerticalLayout {

    private final transient ModelService modelService;
    private final Grid<ProcessModelInfo> grid = new Grid<>();

    public ProcessModelsView(ModelService modelService) {
        this.modelService = Objects.requireNonNull(modelService, "ModelService must be non null");
    }
    
    @PostConstruct
    public void postConstruct() {
        HorizontalLayout horizontalLayout = new HorizontalLayout();
        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.addSucceededListener(t -> {
            try {
                final ProcessDefinition processDefinition = new ProcessModel().unmarshal(buffer.getInputStream());
                final ProcessModelInfo processModelInfo = modelService.save(processDefinition);
                Notification.show("Processmodel " + processModelInfo.getName() + " succesful saved");
                updateItems();
            } catch (JAXBException ex) {
                Logger.getLogger(ProcessModelsView.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
                //Notification.show("This is the caption", "This is the description", Notification.Type.HUMANIZED_MESSAGE);
            }
        });
        horizontalLayout.add(upload);

        add(horizontalLayout);

        grid.setSizeFull();
        grid.addColumn(ProcessModelInfo::getName).setHeader("Name");
        grid.addColumn(ProcessModelInfo::getVersion).setHeader("Version");
        grid.addColumn(ProcessModelInfo::getDescription).setHeader("Description");
        grid.addColumn(ProcessModelInfo::getState).setHeader("State");
        updateItems();

        add(grid);
        setSizeFull();
    }

    private void updateItems() {
        grid.setItems(modelService.findAllByStates(EnumSet.of(ProcessModelState.ACTIVE)));
    }

}
