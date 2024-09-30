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
package org.opensbpm.vaadinui.ui.views;

import jakarta.annotation.security.PermitAll;
import org.opensbpm.engine.api.model.ProcessModelInfo;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.Objects;
import jakarta.annotation.PostConstruct;
import org.opensbpm.vaadinui.backend.SbpmEngine;
import org.opensbpm.vaadinui.ui.MainLayout;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
@Route(value = "models", layout = MainLayout.class)
@PageTitle("Models | OpenSBPM Vaadin Demo")
@PermitAll
public class ProcessModelsView extends VerticalLayout {

    private final transient SbpmEngine sbpmEngine;
    private final Grid<ProcessModelInfo> grid = new Grid<>();

    public ProcessModelsView(SbpmEngine sbpmEngine) {
        this.sbpmEngine = Objects.requireNonNull(sbpmEngine, "ModelService must be non null");
    }
    
    @PostConstruct
    public void postConstruct() {
        HorizontalLayout horizontalLayout = new HorizontalLayout();
        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.addSucceededListener(t -> {
                final ProcessModelInfo processModelInfo = sbpmEngine.uploadModel(buffer.getInputStream());
                Notification.show("Processmodel " + processModelInfo.getName() + " successful saved");
                updateItems();
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
        grid.setItems(sbpmEngine.getProcessModels());
    }

}
