package at.softwaremacherei.jsbpm.webui.ui.views.model;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.shared.Registration;

import org.opensbpm.engine.api.instance.TaskInfo;

public class TaskInfoForm extends FormLayout {

    private TextField processName = new TextField("ProcessName");
    private TextArea stateName = new TextArea("StateName");

    private Button start = new Button("Start");
    private Button close = new Button("Cancel");

    private Binder<TaskInfo> binder = new BeanValidationBinder<>(TaskInfo.class);
    private TaskInfo taskInfo;

    public TaskInfoForm() {
        addClassName("contact-form");

        binder.bindInstanceFields(this);

        add(
                processName,
                stateName,
                createButtonsLayout()
        );
    }

    public void setTaskInfo(TaskInfo taskInfo) {
        this.taskInfo = taskInfo;
        binder.readBean(taskInfo);
    }

    private Component createButtonsLayout() {
        start.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        close.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        start.addClickShortcut(Key.ENTER);
        close.addClickShortcut(Key.ESCAPE);

        start.addClickListener(click -> validateAndSave());
        //delete.addClickListener(click -> fireEvent(new DeleteEvent(this, modelInfo)));
        close.addClickListener(click -> fireEvent(new CloseEvent(this)));

        binder.addStatusChangeListener(evt -> start.setEnabled(binder.isValid()));

        return new HorizontalLayout(start, close);
    }

    private void validateAndSave() {

        try {
            binder.writeBean(taskInfo);
            fireEvent(new SaveEvent(this, taskInfo));
        } catch (ValidationException e) {
            e.printStackTrace();
        }
    }

    // Events
    public static abstract class TaskInfoFormEvent extends ComponentEvent<TaskInfoForm> {

        private TaskInfo taskInfo;

        protected TaskInfoFormEvent(TaskInfoForm source, TaskInfo modelInfo) {
            super(source, false);
            this.taskInfo = modelInfo;
        }

        public TaskInfo getTaskInfo() {
            return taskInfo;
        }
    }

    public static class SaveEvent extends TaskInfoFormEvent {

        SaveEvent(TaskInfoForm source, TaskInfo modelInfo) {
            super(source, modelInfo);
        }
    }

    public static class DeleteEvent extends TaskInfoFormEvent {

        DeleteEvent(TaskInfoForm source, TaskInfo modelInfo) {
            super(source, modelInfo);
        }

    }

    public static class CloseEvent extends TaskInfoFormEvent {

        CloseEvent(TaskInfoForm source) {
            super(source, null);
        }
    }

    public <T extends ComponentEvent<?>> Registration addListener(Class<T> eventType, ComponentEventListener<T> listener) {
        return getEventBus().addListener(eventType, listener);
    }
}
