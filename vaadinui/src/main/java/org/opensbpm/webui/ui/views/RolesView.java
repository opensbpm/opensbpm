package org.opensbpm.webui.ui.views;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.security.PermitAll;
import org.opensbpm.engine.api.UserNotFoundException;
import org.opensbpm.engine.api.instance.TaskInfo;
import org.opensbpm.webui.backend.RolesService;
import org.opensbpm.webui.backend.RolesService.RoleInfo;
import org.opensbpm.webui.backend.SbpmEngine;
import org.opensbpm.webui.ui.MainLayout;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@Scope("prototype")
@Route(value = "roles", layout = MainLayout.class)
@PageTitle("Roles | OpenSBPM Vaadin Demo")
@PermitAll
public class RolesView extends VerticalLayout {

    private final transient RolesService rolesService;

    private final Grid<RoleInfo> grid = new Grid<>();
    private final TextField filterText = new TextField();

    public RolesView(RolesService rolesService) {
        this.rolesService = Objects.requireNonNull(rolesService, "RolesService must be non null");
    }

    @PostConstruct
    public void postConstruct() {
        //addClassName("workflows-view");
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

        //Button addContactButton = new Button("Add contact", click -> addContact());
        HorizontalLayout toolbar = new HorizontalLayout(filterText/*, addContactButton*/);
        toolbar.addClassName("toolbar");
        return toolbar;
    }

    private void configureGrid() {
        grid.addClassName("contact-grid");
        grid.setSizeFull();

        grid.addColumn(RoleInfo::getRoleName).setHeader("RoleName");
        grid.getColumns().forEach(col -> col.setAutoWidth(true));
    }

    private void updateList() {
        grid.setItems(new ListDataProvider<>(
                rolesService.getRoles()
        ));
    }

}
