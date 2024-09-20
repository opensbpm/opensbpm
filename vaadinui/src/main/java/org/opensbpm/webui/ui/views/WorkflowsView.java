package org.opensbpm.webui.ui.views;

import java.util.Optional;

import com.vaadin.flow.data.provider.ListDataProvider;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.router.RouteParameters;

import org.opensbpm.engine.api.ModelNotFoundException;
import org.opensbpm.engine.api.UserNotFoundException;
import org.opensbpm.engine.api.instance.TaskInfo;
import org.opensbpm.engine.api.model.ProcessModelInfo;
import org.opensbpm.webui.backend.SbpmEngine;
import org.opensbpm.webui.ui.MainLayout;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
@Scope("prototype")
@RouteAlias(value = "", layout = MainLayout.class)
@Route(value = "workflows", layout = MainLayout.class)
@PageTitle("Workflows | OpenSBPM Vaadin Demo")
@PermitAll
public class WorkflowsView extends VerticalLayout {

    private final SbpmEngine sbpmEngine;

    private Grid<ProcessModelInfo> grid = new Grid<>();
    private TextField filterText = new TextField();

    public WorkflowsView(SbpmEngine sbpmEngine) {
        this.sbpmEngine = Objects.requireNonNull(sbpmEngine, "SbpmEngine must be non null");

        // addClassName("workflows-view");
        addClassName("list-view");
        setSizeFull();
        configureGrid();

        add(getToolBar(), grid);
        updateList();
    }

    private HorizontalLayout getToolBar() {
        filterText.setPlaceholder("Filter by name...");
        filterText.setClearButtonVisible(true);
        filterText.setValueChangeMode(ValueChangeMode.LAZY);
        filterText.addValueChangeListener(e -> updateList());

        HorizontalLayout toolbar = new HorizontalLayout(filterText);
        toolbar.addClassName("toolbar");
        return toolbar;
    }

    private void configureGrid() {
        grid.addClassName("contact-grid");
        grid.setSizeFull();
        // grid.removeColumnByKey("company");
        grid.addColumn(ProcessModelInfo::getName).setHeader("Name");
        grid.addColumn(ProcessModelInfo::getVersion).setHeader("Version");
        //grid.addColumn(ProcessModelInfo::getDescription).setHeader("Description");        
        
        grid.getColumns().forEach(col -> col.setAutoWidth(true));

        grid.setItemDetailsRenderer(new ComponentRenderer<>(
                processModel -> {
                    Button start = new Button("Start");
                    start.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                    start.addClickShortcut(Key.ENTER);
                    start.addClickListener(click -> {
                        try {
                            TaskInfo taskInfo = sbpmEngine.startProcess(processModel);
                            start.getUI().ifPresent(ui -> ui.navigate(TaskEditor.class, new RouteParameters("taskId", String.valueOf(taskInfo.getId()))));
                        } catch (UserNotFoundException | ModelNotFoundException ex) {
                            Logger.getLogger(WorkflowsView.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
                        }
                    });
                    if (Optional.ofNullable(processModel.getDescription())
                            .filter(description -> !description.isEmpty())
                            .isPresent()) {
                        return new VerticalLayout(
                                new Pre(processModel.getDescription()),
                                new HorizontalLayout(start));
                    } else {
                        return new HorizontalLayout(start);
                    }
                }));
    }

    private void updateList() {
        try {
            grid.setItems(new ListDataProvider<>(
                    sbpmEngine.findStartableProcessModels().stream()
                    .filter(processModel -> processModel.getName().contains(filterText.getValue()))
                            .toList()
                    )
            );
        } catch (UserNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
