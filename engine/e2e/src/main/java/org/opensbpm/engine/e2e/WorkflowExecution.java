package org.opensbpm.engine.e2e;

import org.opensbpm.engine.e2e.statistics.Statistics;

import java.util.List;

public interface WorkflowExecution {
    List<Statistics> execute();
}
