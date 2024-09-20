package org.opensbpm.webui.ui.views.model;

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

import org.opensbpm.engine.api.model.ProcessModelInfo;

public class ProcessModelInfoForm extends FormLayout {

    private TextField name = new TextField("Name");
    private TextArea description = new TextArea("Description");

    private Button start = new Button("Start");
    private Button close = new Button("Cancel");

    private Binder<ProcessModelInfo> binder = new BeanValidationBinder<>(ProcessModelInfo.class);
    private ProcessModelInfo modelInfo;

    public ProcessModelInfoForm() {
        addClassName("contact-form");

        binder.bindInstanceFields(this);

        add(
                name,
                description,
                createButtonsLayout()
        );
    }

    public void setProcessModelInfo(ProcessModelInfo modelInfo) {
        this.modelInfo = modelInfo;
        binder.readBean(modelInfo);
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
            binder.writeBean(modelInfo);
            fireEvent(new SaveEvent(this, modelInfo));
        } catch (ValidationException e) {
            e.printStackTrace();
        }
    }

    // Events
    public static abstract class ProcessModelInfoFormEvent extends ComponentEvent<ProcessModelInfoForm> {

        private ProcessModelInfo modelInfo;

        protected ProcessModelInfoFormEvent(ProcessModelInfoForm source, ProcessModelInfo modelInfo) {
            super(source, false);
            this.modelInfo = modelInfo;
        }

        public ProcessModelInfo getProcessModelInfo() {
            return modelInfo;
        }
    }

    public static class SaveEvent extends ProcessModelInfoFormEvent {

        SaveEvent(ProcessModelInfoForm source, ProcessModelInfo modelInfo) {
            super(source, modelInfo);
        }
    }

    public static class DeleteEvent extends ProcessModelInfoFormEvent {

        DeleteEvent(ProcessModelInfoForm source, ProcessModelInfo modelInfo) {
            super(source, modelInfo);
        }

    }

    public static class CloseEvent extends ProcessModelInfoFormEvent {

        CloseEvent(ProcessModelInfoForm source) {
            super(source, null);
        }
    }

    public <T extends ComponentEvent<?>> Registration addListener(Class<T> eventType,
            ComponentEventListener<T> listener) {
        return getEventBus().addListener(eventType, listener);
    }
}
