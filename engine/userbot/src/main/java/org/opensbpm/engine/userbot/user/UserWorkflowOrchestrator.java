package org.opensbpm.engine.userbot.user;

import org.opensbpm.engine.userbot.client.UserBot;
import org.opensbpm.engine.userbot.WorkflowOrchestrator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

@ConditionalOnProperty(value="opensbpm.starter", havingValue = "false", matchIfMissing = true)
@Component
public class UserWorkflowOrchestrator implements WorkflowOrchestrator {
    private static final Logger LOGGER = Logger.getLogger(UserWorkflowOrchestrator.class.getName());

    private final UserBot userBot;

    public UserWorkflowOrchestrator(UserBot userBot) {
        this.userBot = userBot;
    }

    public void execute(ConfigurableApplicationContext context) {
        userBot.startTaskFetcher();
    }

}
