package at.softwaremacherei.jsbpm.webui.ui.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.shared.Registration;

import at.softwaremacherei.jsbpm.engine.api.UserNotFoundException;
import at.softwaremacherei.jsbpm.engine.api.instance.AttributeStore;
import at.softwaremacherei.jsbpm.engine.api.instance.ObjectBean;
import at.softwaremacherei.jsbpm.engine.api.instance.ObjectSchema;
import at.softwaremacherei.jsbpm.engine.api.instance.Task;
import at.softwaremacherei.jsbpm.engine.api.instance.TaskInfo;
import at.softwaremacherei.jsbpm.engine.api.instance.TaskNotFoundException;
import at.softwaremacherei.jsbpm.engine.api.instance.TaskOutOfDateException;
import at.softwaremacherei.jsbpm.engine.api.instance.TaskRequest;
import at.softwaremacherei.jsbpm.engine.api.model.Binary;
import at.softwaremacherei.jsbpm.webui.backend.SbpmEngine;
import at.softwaremacherei.jsbpm.webui.ui.MainLayout;
import at.softwaremacherei.jsbpm.webui.ui.components.ContentViewer;
import at.softwaremacherei.jsbpm.webui.ui.views.model.ComponentFactory.FormHelper;
import com.vaadin.flow.component.AbstractCompositeField;
import com.vaadin.flow.component.formlayout.FormLayout.FormItem;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.server.StreamResource;
import java.io.ByteArrayInputStream;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.context.annotation.Scope;

@org.springframework.stereotype.Component
@Scope("prototype")
@Route(value = "tasks/:taskId/execute", layout = MainLayout.class)

public class TaskEditor extends VerticalLayout implements BeforeEnterObserver {

    private final SbpmEngine sbpmEngine;
    private final HorizontalLayout toolbar = new HorizontalLayout();
    private final Label stateLabel = new Label();

    private final Button start = new Button("Start");
    private final Button close = new Button("Cancel");

    private final Div formContent = new Div();

    public TaskEditor(SbpmEngine sbpmEngine) {
        this.sbpmEngine = Objects.requireNonNull(sbpmEngine, "sbpmEngine must be non null");
        addClassName("contact-form");

        formContent.setSizeFull();
        add(stateLabel,
                formContent,
                toolbar);
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

            ObjectSchema objectSchema = task.getTaskDocument().getSchemas().stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("only one business-object allowed"));
            FormHelper formHelper = new FormHelper(sbpmEngine, objectSchema);
            formContent.add(formHelper.createForm(taskInfo, objectSchema));

            AttributeStore attributeStore = task.createAttributeStore(objectSchema);

            task.getNextStates().stream()
                    .map(nextState -> {
                        Button nextStateButton = new Button(nextState.getName());
                        nextStateButton.addClickListener(click -> {
                            if (formHelper.getBinder().validate().isOk()) {
                                try {
                                    TaskRequest taskRequest = task.createTaskRequest(nextState, objectSchema, attributeStore);
                                    sbpmEngine.executeTask(taskRequest);
                                    if (nextState.isEnd()) {
                                        nextStateButton.getUI().ifPresent(ui -> ui.navigate(TasksView.class));
                                    } else {
                                        TaskInfo nextTaskInfo = sbpmEngine.getNextTask(taskInfo);
                                        nextStateButton.getUI().ifPresent(ui -> ui.navigate(TaskEditor.class, new RouteParameters("taskId", String.valueOf(nextTaskInfo.getId()))));
                                    }
                                } catch (UserNotFoundException | TaskNotFoundException | TaskOutOfDateException ex) {
                                    Logger.getLogger(TaskEditor.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
                                }
                            }
                        });
                        formHelper.getBinder().addStatusChangeListener(evt -> nextStateButton.setEnabled(formHelper.getBinder().isValid()));
                        return nextStateButton;
                    })
                    .forEach(nextStateButton -> toolbar.add(nextStateButton));

            formHelper.getBinder().setBean(new ObjectBean(objectSchema, attributeStore));

        } catch (TaskNotFoundException | TaskOutOfDateException | UserNotFoundException ex) {
            Logger.getLogger(TaskEditor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

//    private Component createButtonsLayout() {
//        start.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
//        close.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
//
//        start.addClickShortcut(Key.ENTER);
//        close.addClickShortcut(Key.ESCAPE);
//
//        //start.addClickListener(click -> validateAndSave());
//        // delete.addClickListener(click -> fireEvent(new DeleteEvent(this, modelInfo)));
//        close.addClickListener(click -> fireEvent(new CloseEvent(this)));
//
//        binder.addStatusChangeListener(evt -> start.setEnabled(binder.isValid()));
//
//        return new HorizontalLayout(start, close);
//    }
    
    // Events
    public static abstract class TaskFormEvent extends ComponentEvent<TaskEditor> {

        private Task task;

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

    public <T extends ComponentEvent<?>> Registration addListener(Class<T> eventType,
            ComponentEventListener<T> listener) {
        return getEventBus().addListener(eventType, listener);
    }

    public static class BinaryViewer extends AbstractCompositeField<ContentViewer, BinaryViewer, Binary> {

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

    public static class EmbeddedForm extends AbstractCompositeField<FormLayout, EmbeddedForm, ObjectBean> {

        private final Binder<ObjectBean> binder = new BeanValidationBinder<>(ObjectBean.class);
        private final FormLayout formLayout;

        public EmbeddedForm(FormLayout formLayout) {
            super(null);
            this.formLayout = formLayout;
        }

        @Override
        protected FormLayout initContent() {
            return formLayout;
        }

        public FormItem addFormItem(Component field, String label) {
            return getContent().addFormItem(field, label);
        }

        @Override
        protected void setPresentationValue(ObjectBean newPresentationValue) {
            binder.setBean(newPresentationValue);
        }

    }

}
