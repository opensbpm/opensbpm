/*
 * StartupListener.java
 *
 * Created on 25.04.2020,11:19:59
 *
 */
package at.softwaremacherei.jsbpm.webui;

import at.softwaremacherei.jsbpm.engine.api.ModelService;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import at.softwaremacherei.jsbpm.examples.ExampleModels;
import at.softwaremacherei.jsbpm.xmlmodel.ProcessModel;
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

    @EventListener
    @Transactional
    public void onApplicationEvent(ContextRefreshedEvent event) throws JAXBException {
        storeModel(ExampleModels.getDienstreiseantrag());
        storeModel(ExampleModels.getRechungslegung());
        storeModel(ExampleModels.getRechungslegungWizard());
    }

    private void storeModel(InputStream inputStream) throws JAXBException {
        modelService.save(new ProcessModel().unmarshal(inputStream));
    }

}
