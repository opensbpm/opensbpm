package at.softwaremacherei.jsbpm.webui.ui.views;

import java.util.Optional;

import org.springframework.context.annotation.Scope;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;

import at.softwaremacherei.jsbpm.engine.api.EngineService;
import at.softwaremacherei.jsbpm.engine.api.ModelNotFoundException;
import at.softwaremacherei.jsbpm.engine.api.UserNotFoundException;
import at.softwaremacherei.jsbpm.engine.api.UserTokenService;
import at.softwaremacherei.jsbpm.engine.api.instance.UserToken;
import at.softwaremacherei.jsbpm.engine.api.model.ProcessModelInfo;
import at.softwaremacherei.jsbpm.engine.api.model.ProcessModelState;
import at.softwaremacherei.jsbpm.springauthentication.SpringAuthentication;
import at.softwaremacherei.jsbpm.webui.ui.MainLayout;

@Component
@Scope("prototype")
@RouteAlias(value = "", layout = MainLayout.class)
@Route(value = "workflows", layout = MainLayout.class)
@PageTitle("Workflows | SBPM Engine")
public class WorkflowsView extends VerticalLayout {

    private final EngineService engineService;
    private final UserTokenService userTokenService;

    private Grid<ProcessModelInfo> grid = new Grid<>(ProcessModelInfo.class);
    private TextField filterText = new TextField();

    public WorkflowsView(
            EngineService engineService,
            UserTokenService userTokenService) {
        this.engineService = engineService;
        this.userTokenService = userTokenService;

        // addClassName("workflows-view");
        addClassName("list-view");
        setSizeFull();
        configureGrid();

        add(getToolBar(), grid);
        updateList();
    }

    private HorizontalLayout getToolBar() {
        filterText.setPlaceholder("Filter by name...");
        filterText.setClearButtonVisible(true);
        filterText.setValueChangeMode(ValueChangeMode.LAZY);
        filterText.addValueChangeListener(e -> updateList());

        HorizontalLayout toolbar = new HorizontalLayout(filterText);
        toolbar.addClassName("toolbar");
        return toolbar;
    }

    private void configureGrid() {
        grid.addClassName("contact-grid");
        grid.setSizeFull();
        // grid.removeColumnByKey("company");
        grid.setColumns("name", "version", "state");
        grid.addColumn(processModel -> {
            ProcessModelState state = processModel.getState();
            return state == null ? "-" : state.toString();
        }).setHeader("State");

        grid.getColumns().forEach(col -> col.setAutoWidth(true));

        grid.setItemDetailsRenderer(new ComponentRenderer<>(
                processModel -> {
                    Button start = new Button("Start");
                    start.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                    start.addClickShortcut(Key.ENTER);
                    start.addClickListener(click -> startProcess(processModel));
                    if (Optional.ofNullable(processModel.getDescription())
                            .filter(description -> !description.isEmpty())
                            .isPresent()) {
                        return new VerticalLayout(
                                new Pre(processModel.getDescription()),
                                new HorizontalLayout(start));
                    } else
                        return new HorizontalLayout(start);
                }));
    }

    private void updateList() {
        try {
            UserToken userToken = getCurrentUserToken();
            grid.setItems(engineService.findStartableProcessModels(userToken).stream()
                    .filter(processModel -> processModel.getName().contains(filterText.getValue())));
        } catch (UserNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void startProcess(ProcessModelInfo modelInfo) {
        try {
            engineService.startProcess(getCurrentUserToken(), modelInfo);
        } catch (UserNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ModelNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private UserToken getCurrentUserToken() throws UserNotFoundException {
        return userTokenService
                .retrieveToken(SpringAuthentication.of(SecurityContextHolder.getContext().getAuthentication()));
    }

}
