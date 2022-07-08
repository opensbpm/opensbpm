package org.opensbpm.webui.ui.views;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.shared.Registration;

import org.opensbpm.engine.api.UserNotFoundException;
import org.opensbpm.engine.api.instance.ObjectSchema;
import org.opensbpm.engine.api.instance.Task;
import org.opensbpm.engine.api.instance.TaskInfo;
import org.opensbpm.engine.api.instance.TaskNotFoundException;
import org.opensbpm.engine.api.instance.TaskOutOfDateException;
import org.opensbpm.engine.api.instance.TaskRequest;
import org.opensbpm.webui.backend.SbpmEngine;
import org.opensbpm.webui.ui.MainLayout;
import org.opensbpm.webui.ui.views.model.ComponentFactory;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import org.springframework.context.annotation.Scope;

@org.springframework.stereotype.Component
@Scope("prototype")
@Route(value = "tasks/:taskId/execute", layout = MainLayout.class)
public class TaskEditor extends VerticalLayout implements BeforeEnterObserver {

    private final transient SbpmEngine sbpmEngine;
    private final HorizontalLayout toolbar = new HorizontalLayout();
    private final Label stateLabel = new Label();

    private final Div formContent = new Div();

    public TaskEditor(SbpmEngine sbpmEngine) {
        this.sbpmEngine = Objects.requireNonNull(sbpmEngine, "sbpmEngine must be non null");
    }

    @PostConstruct
    public void postConstruct() {
        addClassName("task-form");

        setSizeFull();
        formContent.setWidthFull();
        add(stateLabel, formContent, toolbar);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        formContent.removeAll();
        toolbar.removeAll();
        try {
            String taskId = event.getRouteParameters().get("taskId")
                    .orElseThrow(() -> new IllegalStateException("no route-parameter 'taskId' given"));
            TaskInfo taskInfo = sbpmEngine.getTasks("")
                    .filter(task -> String.valueOf(task.getId()).equals(taskId))
                    .findFirst()
                    .orElseThrow(() -> new TaskOutOfDateException(taskId));
            Task task = sbpmEngine.getTask(taskInfo);

            stateLabel.setText(task.getProcessName() + ":" + task.getStateName());

            ObjectSchema objectSchema = task.getSchemas().stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("only one business-object allowed"));
            ComponentFactory componentFactory = new ComponentFactory(sbpmEngine, objectSchema);
            formContent.add(componentFactory.createForm(taskInfo, objectSchema));

            task.getNextStates().stream()
                    .map(nextState -> {
                        Button nextStateButton = new Button(nextState.getName());
                        nextStateButton.addClickListener(click -> {
                            if (componentFactory.getBinder().validate().isOk()) {
                                try {
                                    TaskRequest taskRequest = task.createTaskRequest(nextState);
                                    sbpmEngine.executeTask(taskRequest);
                                    if (nextState.isEnd()) {
                                        nextStateButton.getUI().ifPresent(ui
                                                -> ui.navigate(TasksView.class));
                                    } else {
                                        TaskInfo nextTaskInfo = sbpmEngine.getNextTask(taskInfo);
                                        nextStateButton.getUI().ifPresent(ui
                                                -> ui.navigate(TaskEditor.class, createTaskParameter(nextTaskInfo)));
                                    }
                                } catch (UserNotFoundException | TaskNotFoundException | TaskOutOfDateException ex) {
                                    Logger.getLogger(TaskEditor.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
                                }
                            }
                        });
                        componentFactory.getBinder().addStatusChangeListener(evt -> nextStateButton.setEnabled(componentFactory.getBinder().isValid()));
                        return nextStateButton;
                    })
                    .forEach(nextStateButton -> toolbar.add(nextStateButton));

            componentFactory.getBinder().setBean(task.getObjectBean(objectSchema));

        } catch (TaskNotFoundException | TaskOutOfDateException | UserNotFoundException ex) {
            Logger.getLogger(TaskEditor.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public static RouteParameters createTaskParameter(TaskInfo taskInfo) {
        return new RouteParameters("taskId", String.valueOf(taskInfo.getId()));
    }

    @Override
    public <T extends ComponentEvent<?>> Registration addListener(Class<T> eventType,
            ComponentEventListener<T> listener) {
        return getEventBus().addListener(eventType, listener);
    }

    public static abstract class TaskFormEvent extends ComponentEvent<TaskEditor> {

        private final Task task;

        protected TaskFormEvent(TaskEditor source, Task task) {
            super(source, false);
            this.task = task;
        }

        public Task getTask() {
            return task;
        }
    }

    public static class SaveEvent extends ComponentEvent<TaskEditor> {

        private final TaskRequest taskRequest;

        public SaveEvent(TaskEditor source, TaskRequest taskRequest) {
            super(source, false);
            this.taskRequest = taskRequest;
        }

        public TaskRequest getTaskRequest() {
            return taskRequest;
        }

    }

    public static class DeleteEvent extends TaskFormEvent {

        DeleteEvent(TaskEditor source, Task task) {
            super(source, task);
        }

    }

    public static class CloseEvent extends TaskFormEvent {

        CloseEvent(TaskEditor source) {
            super(source, null);
        }
    }

}
