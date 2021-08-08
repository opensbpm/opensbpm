package at.softwaremacherei.jsbpm.webui.ui.views.model;

import at.softwaremacherei.jsbpm.engine.api.instance.AttributeSchema;
import at.softwaremacherei.jsbpm.engine.api.instance.NestedAttributeSchema;
import at.softwaremacherei.jsbpm.engine.api.instance.ObjectBean;
import at.softwaremacherei.jsbpm.engine.api.instance.AttributeStore;
import at.softwaremacherei.jsbpm.webui.ui.views.model.EmbeddedGrid.GridEditor;
import com.vaadin.flow.component.AbstractCompositeField;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.Setter;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.function.ValueProvider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EmbeddedGrid extends AbstractCompositeField<GridEditor, EmbeddedGrid, List<ObjectBean>> {

    private final NestedAttributeSchema nestedAttributeSchema;

    public EmbeddedGrid(NestedAttributeSchema nestedAttributeSchema) {
        super(null);
        this.nestedAttributeSchema = nestedAttributeSchema;

        for (AttributeSchema attribute : nestedAttributeSchema.getAttributes()) {
            getContent().addColumn(
                    bean -> bean.get(attribute),
                    (ObjectBean bean, Object vaue) -> bean.set(attribute, vaue),
                    ComponentFactory.createEditorComponent(attribute))
                    .setHeader(attribute.getName());
        }

    }

    @Override
    protected GridEditor initContent() {
        return new GridEditor();
    }

    @Override
    protected void setPresentationValue(List<ObjectBean> objectBeans) {
        getContent().setItems(objectBeans);
    }

    public class GridEditor extends Composite<Div> {

        private final Binder<ObjectBean> binder;
        private final Grid<ObjectBean> grid;
//        private final Button addRowButton;
        private transient List<ObjectBean> interalStore = new ArrayList<>();

        public GridEditor() {
            super();
            grid = new Grid<ObjectBean>();
            grid.setDataProvider(DataProvider.ofCollection(interalStore));

            binder = new Binder<>(ObjectBean.class);
            grid.getEditor().setBinder(binder);
            grid.getEditor().setBuffered(true);
            grid.setSelectionMode(SelectionMode.NONE);
            grid.addItemClickListener(evt -> grid.getEditor().editItem(evt.getItem()));

            Set<Button> actionButtons = new HashSet<>();
            Grid.Column<ObjectBean> actionsColumn = grid.addComponentColumn(item -> {
                final Button editButton = new Button(VaadinIcon.EDIT.create(), e -> grid.getEditor().editItem(item));
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
                grid.getEditor().editItem(newItem);
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
            getElement().addEventListener("keyup",
                    e -> grid.getEditor().cancel()).setFilter("event.key === 'Escape' || event.key === 'Esc'");

//            addRowButton = new Button(VaadinIcon.PLUS.create(),
//                    evt -> addItem(createItem())
//            );
            getContent().add(grid/*, addRowButton*/);
        }

        public <V, C extends Component & HasValue<?, V>> Column<ObjectBean> addColumn(ValueProvider<ObjectBean, V> getter, Setter<ObjectBean, V> setter, C editorComponent) {
            binder.forField(editorComponent).bind(getter, setter);
            return grid.addColumn(getter)
                    .setEditorComponent(editorComponent);
        }

        private ObjectBean createItem() {
            return new ObjectBean(nestedAttributeSchema, new AttributeStore(nestedAttributeSchema));
        }

        public void setItems(List<ObjectBean> objectBeans) {
            interalStore.clear();
            interalStore.addAll(objectBeans);
            grid.getDataProvider().refreshAll();
        }

        private void addItem(ObjectBean objectBean) {
            interalStore.add(objectBean);
            grid.getDataProvider().refreshAll();
            grid.getDataProvider().refreshItem(objectBean);
        }

        private void saveItem(ObjectBean item) {
            updateStore(interalStore);
        }

        private void removeItem(ObjectBean objectBean) {
            interalStore.remove(objectBean);
            grid.getDataProvider().refreshAll();
            grid.getDataProvider().refreshItem(objectBean);
            updateStore(interalStore);
        }

        private void updateStore(Collection<ObjectBean> newItems) {
            //TODO
        }
    }
}
