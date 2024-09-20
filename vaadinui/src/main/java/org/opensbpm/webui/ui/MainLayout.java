package org.opensbpm.webui.ui;

import com.vaadin.flow.component.Component;
import org.opensbpm.webui.ui.views.RolesView;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.PWA;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.opensbpm.webui.ui.views.ProcessModelsView;
import org.opensbpm.webui.ui.views.TasksView;
import org.opensbpm.webui.ui.views.WorkflowsView;

@CssImport("./styles/shared-styles.css")
public class MainLayout extends AppLayout {

    public MainLayout() {
        createHeader();
        createDrawer();
    }

    private void createHeader() {
        H1 logo = new H1("OpenSBPM:engine");
        logo.addClassName("logo");

        Anchor logout = new Anchor("/logout", "Log out");

        Text username = new Text("User:" + SecurityContextHolder.getContext().getAuthentication().getName());
        Text authorities = new Text("Authorities:" + SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(",")));

        HorizontalLayout header = new HorizontalLayout(new DrawerToggle(), logo, username, authorities, logout);
        header.addClassName("header");
        header.setWidth("100%");
        header.expand(logo);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        addToNavbar(header);
    }

    private void createDrawer() {
        List<Component> components = new ArrayList<>(
                Arrays.asList(
                        new RouterLink("Roles", RolesView.class),
                        new RouterLink("Workflows", WorkflowsView.class),
                        new RouterLink("Tasks", TasksView.class)
                )
        );
        if (true) {
            components.add(new RouterLink("Models", ProcessModelsView.class));
        }
        addToDrawer(new VerticalLayout(components.toArray(new Component[0])));
    }

}
