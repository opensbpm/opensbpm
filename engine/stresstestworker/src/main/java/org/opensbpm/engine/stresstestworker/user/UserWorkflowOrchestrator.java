package org.opensbpm.engine.stresstestworker.user;

import org.opensbpm.engine.stresstest.UserClient;
import org.opensbpm.engine.stresstestworker.WorkflowOrchestrator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

@ConditionalOnProperty(value="opensbpm.starter", havingValue = "false", matchIfMissing = true)
@Component
public class UserWorkflowOrchestrator implements WorkflowOrchestrator {
    private static final Logger LOGGER = Logger.getLogger(UserWorkflowOrchestrator.class.getName());

    private final UserClient userClient;

    public UserWorkflowOrchestrator(UserClient userClient) {
        this.userClient = userClient;
    }

    public void execute(ConfigurableApplicationContext context) {
        userClient.startTaskFetcher();

        while (true) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
