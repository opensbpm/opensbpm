package at.softwaremacherei.jsbpm.webui.ui.views;

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
import at.softwaremacherei.jsbpm.webui.backend.SbpmEngine;
import at.softwaremacherei.jsbpm.webui.ui.MainLayout;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.RouteParameters;
import java.util.Objects;

@Component
@Scope("prototype")
@Route(value = "tasks", layout = MainLayout.class)
@PageTitle("Tasks | SBPM Engine")
public class TasksView extends VerticalLayout {

    private final SbpmEngine sbpmEngine;

    private Grid<TaskInfo> grid = new Grid<>(TaskInfo.class);
    private TextField filterText = new TextField();

    public TasksView(SbpmEngine sbpmEngine) {
        this.sbpmEngine = Objects.requireNonNull(sbpmEngine, "sbpmEngine must be non null");

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

//    private void addContact() {
//        grid.asSingleSelect().clear();
//        showProcessModel(new Contact());
//    }
    private void configureGrid() {
        grid.addClassName("contact-grid");
        grid.setSizeFull();
        //grid.removeColumnByKey("company");
        grid.setColumns("processName", "stateName");
        grid.getColumns().forEach(col -> col.setAutoWidth(true));

        grid.setItemDetailsRenderer(new ComponentRenderer<>(
                taskInfo -> {
                    Button execute = new Button("Execute");
                    execute.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                    execute.addClickShortcut(Key.ENTER);
                    execute.addClickListener(click -> {
                        execute.getUI().ifPresent(ui -> ui.navigate(TaskEditor.class, new RouteParameters("taskId", String.valueOf(taskInfo.getId()))));
                    });
                    return new HorizontalLayout(execute);
                }));
    }

    private void updateList() {
        try {
            grid.setItems(sbpmEngine.getTasks(filterText.getValue()));
        } catch (UserNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
