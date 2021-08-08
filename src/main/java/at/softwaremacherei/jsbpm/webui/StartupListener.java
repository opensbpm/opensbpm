/*
 * StartupListener.java
 *
 * Created on 25.04.2020,11:19:59
 *
 */
package at.softwaremacherei.jsbpm.webui;

import at.softwaremacherei.jsbpm.engine.api.ModelService;
import at.softwaremacherei.jsbpm.engine.api.TaskProviderService;
import at.softwaremacherei.jsbpm.engine.api.taskprovider.TaskProviderInfo;
import at.softwaremacherei.jsbpm.engine.api.taskprovider.TaskProviderInfo.ProviderResource;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import at.softwaremacherei.jsbpm.examples.ExampleModels;
import static at.softwaremacherei.jsbpm.examples.ExampleModels.findResource;
import at.softwaremacherei.jsbpm.jasperreports.JasperReportsProvider;
import java.io.InputStream;
import org.springframework.transaction.annotation.Transactional;
import at.softwaremacherei.jsbpm.xmlmodel.ProcessModel;
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
        storeModel(ExampleModels.getDienstreiseantrag());
        storeModel(ExampleModels.findResource("Dienstreiseantrag_Angestellte.xml"));
        storeModel(ExampleModels.getRechungslegung());
        for (ProviderResource jasperreport : getRechungslegungReports()) {
            TaskProviderInfo taskProviderInfo = taskProviderService.getProviders().stream()
                    .filter(providerInfo -> providerInfo.getName().equals("JasperReports"))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Taskprovider 'JasperReports' not found"));

            taskProviderService.addResource(taskProviderInfo, jasperreport);
        }
        storeModel(ExampleModels.getRechungslegungWizard());
    }

    public static ProviderResource[] getRechungslegungReports() {
        return new ProviderResource[]{
            new ProviderResource("rechnung", "application/jrxml", findResource("jasperreports/rechnung.jrxml")),
            new ProviderResource("rechnung_subreport1", "application/jrxml", findResource("jasperreports/rechnung_subreport1.jrxml"))
        };
    }

    private void storeModel(InputStream inputStream) throws JAXBException {
        modelService.save(new ProcessModel().unmarshal(inputStream));
    }

}
