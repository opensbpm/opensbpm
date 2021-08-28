package at.softwaremacherei.jsbpm.webui.ui.views.model;

import at.softwaremacherei.jsbpm.engine.api.instance.IsAttributesContainer;
import at.softwaremacherei.jsbpm.engine.api.instance.ObjectBean;
import at.softwaremacherei.jsbpm.engine.api.instance.ObjectSchema;
import at.softwaremacherei.jsbpm.engine.api.instance.TaskInfo;
import at.softwaremacherei.jsbpm.webui.backend.SbpmEngine;
import at.softwaremacherei.jsbpm.webui.ui.views.model.ComponentFactory.FormHelper;
import com.vaadin.flow.component.AbstractCompositeField;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.data.binder.Binder;

public class EmbeddedForm extends AbstractCompositeField<FormLayout, EmbeddedForm, ObjectBean> {

    private final SbpmEngine sbpmEngine;
    private final TaskInfo taskInfo;
    private final IsAttributesContainer attributesContainer;
    private final ObjectSchema objectSchema;

    private Binder<ObjectBean> binder;

    public EmbeddedForm(SbpmEngine sbpmEngine, TaskInfo taskInfo, IsAttributesContainer attributesContainer, ObjectSchema objectSchema) {
        super(null);
        this.sbpmEngine = sbpmEngine;
        this.taskInfo = taskInfo;
        this.attributesContainer = attributesContainer;
        this.objectSchema = objectSchema;
    }

    @Override
    protected FormLayout initContent() {
        FormHelper formHelper = new FormHelper(sbpmEngine, null);
        FormLayout formLayout = formHelper.createForm(taskInfo, attributesContainer);
        binder = formHelper.getBinder();
        return formLayout;
    }

    @Override
    protected void setPresentationValue(ObjectBean newPresentationValue) {
        binder.setBean(newPresentationValue);
    }

}
