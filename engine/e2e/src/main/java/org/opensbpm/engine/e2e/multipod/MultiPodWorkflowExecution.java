package org.opensbpm.engine.e2e.multipod;

import org.opensbpm.engine.client.userbot.Statistics;
import org.opensbpm.engine.e2e.WorkflowExecution;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@ConditionalOnProperty(value="e2e.execution", havingValue = "multi")
@Component
public class MultiPodWorkflowExecution implements WorkflowExecution {
    @Override
    public List<Statistics> execute() {
        return List.of();
    }
}
