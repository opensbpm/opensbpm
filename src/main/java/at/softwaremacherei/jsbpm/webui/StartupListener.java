/*
 * StartupListener.java
 *
 * Created on 25.04.2020,11:19:59
 *
 */
package at.softwaremacherei.jsbpm.webui;

import org.opensbpm.engine.api.ModelService;
import org.opensbpm.engine.api.TaskProviderService;
import org.opensbpm.engine.api.taskprovider.TaskProviderInfo;
import org.opensbpm.engine.api.taskprovider.TaskProviderInfo.ProviderResource;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import org.springframework.transaction.annotation.Transactional;
import org.opensbpm.engine.xmlmodel.ProcessModel;
import javax.xml.bind.JAXBException;
import org.springframework.context.event.EventListener;

@Component
public class StartupListener {

    private static final Logger LOGGER = Logger.getLogger(StartupListener.class.getSimpleName());

    @Autowired
    private ModelService modelService;

    @Autowired
    private TaskProviderService taskProviderService;

    @EventListener
    @Transactional
    public void onApplicationEvent(ContextRefreshedEvent event) throws JAXBException {
        storeModel("Rechnungslegung_Kunden.xml");
        storeModel("Rechnungslegung_Wizard.xml");
        storeModel("Rechnungslegung.xml");
        for (ProviderResource jasperreport : getRechungslegungReports()) {
            TaskProviderInfo taskProviderInfo = taskProviderService.getProviders().stream()
                    .filter(providerInfo -> providerInfo.getName().equals("JasperReports"))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Taskprovider 'JasperReports' not found"));

            taskProviderService.addResource(taskProviderInfo, jasperreport);
        }
    }

    public static ProviderResource[] getRechungslegungReports() {
        return new ProviderResource[]{
            new ProviderResource("rechnung", "application/jrxml", loadResource("jasperreports/rechnung.jrxml")),
            new ProviderResource("rechnung_subreport1", "application/jrxml", loadResource("jasperreports/rechnung_subreport1.jrxml"))
        };
    }

    private static InputStream loadResource(String resource) {
        return StartupListener.class.getResourceAsStream("/at/softwaremacherei/jsbpm/webui/models/" + resource);
    }

    private void storeModel(String model) throws JAXBException {
        modelService.save(new ProcessModel().unmarshal(loadResource(model)));
    }

}
