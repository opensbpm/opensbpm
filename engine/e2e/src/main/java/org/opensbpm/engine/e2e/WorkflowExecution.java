package org.opensbpm.engine.e2e;

import org.opensbpm.engine.client.userbot.Statistics;

import java.util.List;

public interface WorkflowExecution {
    List<Statistics> execute();
}
