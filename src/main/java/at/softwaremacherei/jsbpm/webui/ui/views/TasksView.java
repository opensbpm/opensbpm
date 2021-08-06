package at.softwaremacherei.jsbpm.webui.ui.views;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import at.softwaremacherei.jsbpm.engine.api.UserNotFoundException;
import at.softwaremacherei.jsbpm.engine.api.instance.TaskInfo;
import at.softwaremacherei.jsbpm.engine.api.instance.TaskNotFoundException;
import at.softwaremacherei.jsbpm.engine.api.instance.TaskOutOfDateException;
import at.softwaremacherei.jsbpm.webui.backend.SbpmEngine;
import at.softwaremacherei.jsbpm.webui.ui.MainLayout;
import at.softwaremacherei.jsbpm.webui.ui.views.model.TaskForm.SaveEvent;
import at.softwaremacherei.jsbpm.webui.ui.views.model.TaskForm;
import at.softwaremacherei.jsbpm.webui.ui.views.model.TaskForm.CloseEvent;
import java.util.Objects;

@Component
@Scope("prototype")
@Route(value = "tasks", layout = MainLayout.class)
@PageTitle("Workflows | SBPM Engine")
public class TasksView extends VerticalLayout {

    private final SbpmEngine sbpmEngine;

    private TaskForm form;
    private Grid<TaskInfo> grid = new Grid<>(TaskInfo.class);
    private TextField filterText = new TextField();

    public TasksView(SbpmEngine sbpmEngine) {
        this.sbpmEngine = Objects.requireNonNull(sbpmEngine, "sbpmEngine must be non null");

        //addClassName("workflows-view");
        addClassName("list-view");
        setSizeFull();
        configureGrid();

        form = new TaskForm(sbpmEngine);
        form.addListener(SaveEvent.class, this::executeTask);
        //form.addListener(DeleteEvent.class, this::deleteContact);
        form.addListener(CloseEvent.class, e -> closeEditor());

        Div content = new Div(grid, form);
        content.addClassName("content");
        content.setSizeFull();

        add(getToolBar(), content);
        updateList();
        closeEditor();
    }

//    private void deleteContact(DeleteEvent evt) {
//        contactService.delete(evt.getContact());
//        updateList();
//        closeEditor();
//    }
    private void executeTask(SaveEvent evt) {
        try {
            sbpmEngine.executeTask(evt.getTaskRequest());
        } catch (UserNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (TaskOutOfDateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (TaskNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        updateList();
        closeEditor();
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

        //TODO 
        grid.asSingleSelect().addValueChangeListener(evt -> showTask(evt.getValue()));
    }

    private void showTask(TaskInfo taskInfo) {
        if (taskInfo == null) {
            form.setTask(null);
        } else
        try {
            form.setTask(sbpmEngine.getTasks(taskInfo));
        } catch (TaskNotFoundException | TaskOutOfDateException | UserNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        form.setVisible(true);
        addClassName("editing");
    }

    private void closeEditor() {
        form.setTask(null);
        form.setVisible(false);
        removeClassName("editing");
    }

    private void updateList() {
        try {
            grid.setItems(sbpmEngine.getTasks(filterText.getValue()));
        } catch (UserNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        //grid.setItems(contactService.findAll(filterText.getValue()));
    }

}
