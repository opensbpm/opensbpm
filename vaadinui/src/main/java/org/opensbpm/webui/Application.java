package org.opensbpm.webui;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;

/**
 * The entry point of the Spring Boot application.
 */
@SpringBootApplication(exclude = ErrorMvcAutoConfiguration.class)
@PWA(name = "OpenSBPM Vaadin Demo",
        shortName = "OpenSBPM",
        offlineResources = {
                "./styles/offline.css",
                "./images/offline.png"
        }
        /*enableInstallPrompt = false*/)
@Theme("my-theme")
public class Application implements AppShellConfigurator {

    public static void main(String[] args) {
        SslConfiguration.trustAll();
        SpringApplication.run(Application.class, args);
    }

}
