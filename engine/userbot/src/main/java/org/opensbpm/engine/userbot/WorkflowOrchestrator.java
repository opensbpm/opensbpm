package org.opensbpm.engine.userbot;

import org.springframework.context.ConfigurableApplicationContext;

public interface WorkflowOrchestrator {
     void execute(ConfigurableApplicationContext context) ;

}
