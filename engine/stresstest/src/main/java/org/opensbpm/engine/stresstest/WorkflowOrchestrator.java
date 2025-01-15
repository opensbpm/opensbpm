package org.opensbpm.engine.stresstest;

import org.springframework.context.ConfigurableApplicationContext;

public interface WorkflowOrchestrator {
     void execute(ConfigurableApplicationContext context) ;

}
