package org.opensbpm.engine.stresstestworker;

import org.springframework.context.ConfigurableApplicationContext;

public interface WorkflowOrchestrator {
     void execute(ConfigurableApplicationContext context) ;

}
