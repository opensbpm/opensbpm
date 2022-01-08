package org.opensbpm.webui.ui.views.model;

import org.opensbpm.engine.api.instance.AttributeSchema;
import org.opensbpm.engine.api.instance.NestedAttributeSchema;
import org.opensbpm.engine.api.instance.ObjectBean;
import org.opensbpm.engine.api.instance.AttributeStore;
import org.opensbpm.engine.api.instance.TaskInfo;
import org.opensbpm.webui.backend.SbpmEngine;
import org.opensbpm.webui.ui.views.model.EmbeddedGrid.GridEditor;
import com.vaadin.flow.component.AbstractCompositeField;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.DataProvider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EmbeddedGrid extends AbstractCompositeField<GridEditor, EmbeddedGrid, List<ObjectBean>> {

    private final SbpmEngine sbpmEngine;
    private final TaskInfo taskInfo;
    private final NestedAttributeSchema nestedAttributeSchema;

    public EmbeddedGrid(SbpmEngine sbpmEngine, TaskInfo taskInfo, NestedAttributeSchema nestedAttributeSchema) {
        super(null);
        this.sbpmEngine = sbpmEngine;
        this.taskInfo = taskInfo;
        this.nestedAttributeSchema = nestedAttributeSchema;

    }

    @Override
    protected GridEditor initContent() {
        return new GridEditor(sbpmEngine, taskInfo, nestedAttributeSchema);
    }

    @Override
    protected void setPresentationValue(List<ObjectBean> objectBeans) {
        getContent().setItems(objectBeans);
    }

    public class GridEditor extends Composite<Grid<ObjectBean>> {

        private final Binder<ObjectBean> binder;
        private transient List<ObjectBean> interalStore = new ArrayList<>();

        public GridEditor(SbpmEngine sbpmEngine, TaskInfo taskInfo, NestedAttributeSchema parentSchema) {
            super();

            ComponentFactory componentFactory = new ComponentFactory(sbpmEngine, null);
            for (AttributeSchema attribute : parentSchema.getAttributes()) {
                Component field = componentFactory.createField(taskInfo, attribute);

                getContent().addColumn(bean -> bean.get(attribute))
                        .setEditorComponent(field)
                        .setHeader(attribute.getName())
                        .setFlexGrow(1)
                        .setAutoWidth(true);
            }
            binder = componentFactory.getBinder();

            getContent().setWidthFull();
        }

        @Override
        protected Grid<ObjectBean> initContent() {
            Grid<ObjectBean> grid = new Grid<>();
            grid.setDataProvider(DataProvider.ofCollection(interalStore));

            grid.setSelectionMode(SelectionMode.NONE);
            grid.addItemClickListener(evt -> editItem(evt.getItem()));

            Set<Button> actionButtons = new HashSet<>();
            Grid.Column<ObjectBean> actionsColumn = grid.addComponentColumn(item -> {
                final Button editButton = new Button(VaadinIcon.EDIT.create(), e -> editItem(item));
                editButton.setId("edit");
                editButton.setEnabled(!grid.getEditor().isOpen());
                actionButtons.add(editButton);

                final Button deleteButton = new Button(VaadinIcon.DEL.create(), e -> removeItem(item));
                deleteButton.setEnabled(!grid.getEditor().isOpen());
                deleteButton.setId("delete");
                actionButtons.add(deleteButton);

                return new Div(editButton, deleteButton);
            });
            actionsColumn.setFrozen(true);
            //PENDING find minimum needed width automaticly
            actionsColumn.setWidth("120px");

            final Button addButton = new Button(VaadinIcon.PLUS.create(), e -> {
                final ObjectBean newItem = createItem();
                addItem(newItem);
                editItem(newItem);
            });
            addButton.setId("add");
            actionButtons.add(addButton);
            actionsColumn.setHeader(addButton);

            actionsColumn.setEditorComponent(item -> {
                Button saveButton = new Button(VaadinIcon.CHECK.create(), e -> grid.getEditor().save());
                saveButton.setId("save");

                Button cancelButton = new Button(VaadinIcon.CLOSE.create(), e -> grid.getEditor().cancel());
                //TODO handle new row with cancel; some more work needed to correctly 
//                Button cancelButton = new Button(VaadinIcon.CLOSE.create(), e -> {
//                    final BinderValidationStatus<T> validate = getEditor().getBinder().validate();
//                    if (validate.isOk()) {
//                        getEditor().cancel();
//                    }else{
//                        removeItem(item);
//                    }
//                });
                cancelButton.setId("cancel");

                return new Div(saveButton, cancelButton);
            });

            grid.getEditor().addOpenListener(e -> actionButtons.stream().forEach(button -> button.setEnabled(!grid.getEditor().isOpen())));
            grid.getEditor().addCloseListener(e -> actionButtons.stream().forEach(button -> button.setEnabled(!grid.getEditor().isOpen())));
            grid.getEditor().addSaveListener(e -> saveItem(e.getItem()));

            // Add a keypress listener that listens for an escape key up event.
            // Note! some browsers return key as Escape and some as Esc
            grid.getElement().addEventListener("keyup",
                    e -> grid.getEditor().cancel()).setFilter("event.key === 'Escape' || event.key === 'Esc'");

            return grid;
        }

        private void editItem(final ObjectBean newItem) {
            getContent().getEditor().setBinder(binder);
            getContent().getEditor().setBuffered(true);
            getContent().getEditor().editItem(newItem);
        }

//        private void buildColumns(NestedAttributeSchema parentAttributeSchema, SbpmEngine sbpmEngine, TaskInfo taskInfo) {
//            for (AttributeSchema attribute : parentAttributeSchema.getAttributes()) {
//                new ComponentFactory.FormHelper(sbpmEngine, null).createForm(taskInfo, parentAttributeSchema);
//
//                ValueProvider<ObjectBean, Object> getter = bean -> bean.get(attribute);
//                Setter<ObjectBean, Object> setter = (ObjectBean bean, Object vaue) -> bean.set(attribute, vaue);
//                addColumn(getter, setter,
//                        new ComponentFactory(sbpmEngine).createEditorComponent(taskInfo, attribute))
//                        .setHeader(attribute.getName());
//            }
//        }
//
//        public <V, C extends Component & HasValue<?, V>> Column<ObjectBean> addColumn(ValueProvider<ObjectBean, V> getter, Setter<ObjectBean, V> setter, C editorComponent) {
//            binder.forField(editorComponent)
//                    .bind(getter, setter);
//            return grid.addColumn(getter)
//                    .setEditorComponent(editorComponent);
//        }
        private ObjectBean createItem() {
            return new ObjectBean(nestedAttributeSchema, new AttributeStore(nestedAttributeSchema));
        }

        public void setItems(List<ObjectBean> objectBeans) {
            interalStore.clear();
            interalStore.addAll(objectBeans);
            getContent().getDataProvider().refreshAll();
        }

        private void addItem(ObjectBean objectBean) {
            interalStore.add(objectBean);
            getContent().getDataProvider().refreshAll();
            getContent().getDataProvider().refreshItem(objectBean);
        }

        private void saveItem(ObjectBean item) {
            updateStore(interalStore);
        }

        private void removeItem(ObjectBean objectBean) {
            interalStore.remove(objectBean);
            getContent().getDataProvider().refreshAll();
            getContent().getDataProvider().refreshItem(objectBean);
            updateStore(interalStore);
        }

        private void updateStore(Collection<ObjectBean> newItems) {
            //TODO
        }
    }
}
