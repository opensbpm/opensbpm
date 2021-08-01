package at.softwaremacherei.jsbpm.webui.ui.views;

import org.springframework.context.annotation.Scope;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import at.softwaremacherei.jsbpm.engine.api.EngineService;
import at.softwaremacherei.jsbpm.engine.api.ModelNotFoundException;
import at.softwaremacherei.jsbpm.engine.api.UserNotFoundException;
import at.softwaremacherei.jsbpm.engine.api.UserTokenService;
import at.softwaremacherei.jsbpm.engine.api.instance.UserToken;
import at.softwaremacherei.jsbpm.engine.api.model.ProcessModelInfo;
import at.softwaremacherei.jsbpm.engine.api.model.ProcessModelState;
import at.softwaremacherei.jsbpm.springauthentication.SpringAuthentication;
import at.softwaremacherei.jsbpm.webui.ui.MainLayout;
import at.softwaremacherei.jsbpm.webui.ui.views.model.ProcessModelInfoForm;
import at.softwaremacherei.jsbpm.webui.ui.views.model.ProcessModelInfoForm.CloseEvent;
import at.softwaremacherei.jsbpm.webui.ui.views.model.ProcessModelInfoForm.SaveEvent;

@Component
@Scope("prototype")
//@RouteAlias(value = "")
@Route(value = "workflows", layout = MainLayout.class)
@PageTitle("Workflows | SBPM Engine")
public class WorkflowsView extends VerticalLayout {

    private final EngineService engineService;
    private final UserTokenService userTokenService;
    
    private ProcessModelInfoForm form;
    private Grid<ProcessModelInfo> grid = new Grid<>(ProcessModelInfo.class);
    private TextField filterText = new TextField();

    public WorkflowsView(
                EngineService engineService,
                UserTokenService userTokenService) {
        this.engineService =engineService;
        this.userTokenService = userTokenService;
        
        //addClassName("workflows-view");
        addClassName("list-view");
        setSizeFull();
        configureGrid();


        form = new ProcessModelInfoForm();
        form.addListener(SaveEvent.class, this::saveContact);
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

    private void saveContact(SaveEvent evt) {
        try {
            engineService.startProcess(getCurrentUserToken(), evt.getProcessModelInfo());
        } catch (UserNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ModelNotFoundException e) {
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
        grid.setColumns("name", "version", "state");
        grid.addColumn(processModel -> {
           ProcessModelState state = processModel.getState();
           return state == null ? "-" : state.toString();
        }).setHeader("State");

        grid.getColumns().forEach(col -> col.setAutoWidth(true));

        //TODO 
        grid.asSingleSelect().addValueChangeListener(evt -> showProcessModel(evt.getValue()));
    }

    private void showProcessModel(ProcessModelInfo modelInfo) {
        form.setProcessModelInfo(modelInfo);
        form.setVisible(true);
        addClassName("editing");
    }

    private void closeEditor() {
        form.setProcessModelInfo(null);
        form.setVisible(false);
        removeClassName("editing");
    }

    private void updateList() {
        try {
            UserToken userToken = getCurrentUserToken();
            grid.setItems(engineService.findStartableProcessModels(userToken).stream()
                .filter(processModel -> processModel.getName().contains(filterText.getValue()))
            );
        } catch (UserNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        //grid.setItems(contactService.findAll(filterText.getValue()));
    }

    private UserToken getCurrentUserToken() throws UserNotFoundException {
        return userTokenService.retrieveToken(SpringAuthentication.of(SecurityContextHolder.getContext().getAuthentication()));
    }

}
