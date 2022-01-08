package org.opensbpm.webui.ui;

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

import org.opensbpm.webui.ui.views.TasksView;
import org.opensbpm.webui.ui.views.WorkflowsView;

@PWA(name = "SBPM Engine", shortName = "SBPM", offlineResources = {
    "./styles/offline.css",
    "./images/offline.png"
}, enableInstallPrompt = false)
@CssImport("./styles/shared-styles.css")
public class MainLayout extends AppLayout {

    public MainLayout() {
        createHeader();
        createDrawer();
    }

    private void createHeader() {
        H1 logo = new H1("SBPM Engine");
        logo.addClassName("logo");

        Anchor logout = new Anchor("/logout", "Log out");

        Text username = new Text("User:" + SecurityContextHolder.getContext().getAuthentication().getName());

        HorizontalLayout header = new HorizontalLayout(new DrawerToggle(), logo, username, logout);
        header.addClassName("header");
        header.setWidth("100%");
        header.expand(logo);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        addToNavbar(header);
    }

    private void createDrawer() {
        addToDrawer(new VerticalLayout(
                new RouterLink("Workflows", WorkflowsView.class),
                new RouterLink("Tasks", TasksView.class)
        ));
    }

}
