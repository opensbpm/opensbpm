package org.opensbpm.vaadinui.ui.views;

import com.vaadin.flow.data.provider.ListDataProvider;
import jakarta.annotation.security.PermitAll;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import org.opensbpm.engine.api.UserNotFoundException;
import org.opensbpm.engine.api.instance.TaskInfo;
import org.opensbpm.vaadinui.backend.SbpmEngine;
import org.opensbpm.vaadinui.ui.MainLayout;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.RouteParameters;

import java.util.Objects;

import jakarta.annotation.PostConstruct;

@Component
@Scope("prototype")
@Route(value = "tasks", layout = MainLayout.class)
@PageTitle("Tasks | OpenSBPM Vaadin Demo")
@PermitAll
public class TasksView extends VerticalLayout {

    private final transient SbpmEngine sbpmEngine;

    private final Grid<TaskInfo> grid = new Grid<>();
    private final TextField filterText = new TextField();

    public TasksView(SbpmEngine sbpmEngine) {
        this.sbpmEngine = Objects.requireNonNull(sbpmEngine, "SbpmEngine must be non null");
    }

    @PostConstruct
    public void postConstruct() {
        //addClassName("workflows-view");
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

        //Button addContactButton = new Button("Add contact", click -> addContact());
        HorizontalLayout toolbar = new HorizontalLayout(filterText/*, addContactButton*/);
        toolbar.addClassName("toolbar");
        return toolbar;
    }

    private void configureGrid() {
        grid.addClassName("contact-grid");
        grid.setSizeFull();

        grid.addColumn(TaskInfo::getProcessName).setHeader("Processname");
        grid.addColumn(TaskInfo::getStateName).setHeader("State");
        grid.getColumns().forEach(col -> col.setAutoWidth(true));

        grid.setItemDetailsRenderer(new ComponentRenderer<>(
                taskInfo -> {
                    Button execute = new Button("Execute");
                    execute.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                    execute.addClickShortcut(Key.ENTER);
                    execute.addClickListener(click -> {
                        execute.getUI().ifPresent(ui
                                -> ui.navigate(TaskEditor.class, createTaskParameter(taskInfo)));
                    });
                    return new HorizontalLayout(execute);
                }));
    }

    private void updateList() {
        try {
            grid.setItems(new ListDataProvider<>(
                    sbpmEngine.getTasks(filterText.getValue()).toList()
            ));
        } catch (UserNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static RouteParameters createTaskParameter(TaskInfo taskInfo) {
        return new RouteParameters("taskId", String.valueOf(taskInfo.getId()));
    }

}
