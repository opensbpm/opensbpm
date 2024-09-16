package org.opensbpm.webui;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Import;

import org.opensbpm.engine.core.EngineConfig;
import org.opensbpm.plugins.jasperreports.JasperReportsConfig;

import javax.net.ssl.*;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;

/**
 * The entry point of the Spring Boot application.
 */
@SpringBootApplication(exclude = ErrorMvcAutoConfiguration.class)
@Import({EngineConfig.class,
    JasperReportsConfig.class
})
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
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                    public void checkClientTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }
                }
        };

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            // Create all-trusting host name verifier
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };
            // Install the all-trusting host verifier
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        } catch (GeneralSecurityException e) {
        }

        SpringApplication.run(Application.class, args);
    }

}
