package org.opensbpm.webui.ui.views.model;

import org.opensbpm.engine.api.instance.IsAttributesContainer;
import org.opensbpm.engine.api.instance.ObjectBean;
import org.opensbpm.engine.api.instance.ObjectSchema;
import org.opensbpm.engine.api.instance.TaskInfo;
import org.opensbpm.webui.backend.SbpmEngine;
import com.vaadin.flow.component.AbstractCompositeField;
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
        ComponentFactory componentFactory = new ComponentFactory(sbpmEngine, null);
        FormLayout formLayout = componentFactory.createForm(taskInfo, attributesContainer);
        binder = componentFactory.getBinder();
        return formLayout;
    }

    @Override
    protected void setPresentationValue(ObjectBean newPresentationValue) {
        binder.setBean(newPresentationValue);
    }

}
