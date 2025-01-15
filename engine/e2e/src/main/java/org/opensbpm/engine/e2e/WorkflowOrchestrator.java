package org.opensbpm.engine.e2e;

import org.opensbpm.engine.client.userbot.Statistics;
import org.opensbpm.engine.e2e.statistics.WebdavUploader;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Component
public class WorkflowOrchestrator {
    private static final Logger LOGGER = Logger.getLogger(WorkflowOrchestrator.class.getName());

    private final AppParameters appParameters;
    private final WorkflowExecution execution;
    private final WebdavUploader uploaderService;

    public WorkflowOrchestrator(AppParameters appParameters, WorkflowExecution execution, WebdavUploader uploaderService) {
        this.appParameters = appParameters;
        this.execution = execution;
        this.uploaderService = uploaderService;
    }

    public void execute() {
        LocalDateTime startTime = LocalDateTime.now();
        List<Statistics> statistics = execution.execute();
        LocalDateTime endTime = LocalDateTime.now();

        Duration duration = Duration.between(startTime, endTime);
        StringBuilder builder = new StringBuilder()
                .append("node_count,")
                .append("pod_count,")
                .append("test_start,")
                .append("test_end,")
                .append("test_duration,")
                .append("test_task_count,")
                .append("process_count,")
                .append("process_start,")
                .append("process_end,")
                .append("process_duration,")
                .append("process_task_count\n");
        long testTaskCount = statistics.stream()
                .mapToLong(statistic -> statistic.getCount())
                .sum();
        String data = statistics.stream()
                .map(statistic ->
                        new StringBuilder()
                                .append(appParameters.getNodeCount()).append(",")
                                .append(appParameters.getPodCount()).append(",")
                                .append(asString(startTime)).append(",")
                                .append(asString(endTime)).append(",")
                                .append(duration.toMillis()).append(",")
                                .append(testTaskCount).append(",")
                                .append(appParameters.getProcessCount()).append(",")
                                .append(asString(statistic.getStartTime())).append(",")
                                .append(asString(statistic.getEndTime())).append(",")
                                .append(statistic.getDuration().toMillis()).append(",")
                                .append(statistic.getCount())
                                .toString()
                )
                .collect(Collectors.joining("\n"));
        builder.append(data).append("\n");
        String statData = builder.toString();

        LOGGER.info("statistics: \n" + statData);
        uploaderService.uploadStatistic(appParameters, statData);

        LOGGER.info("Everything done");
    }

    private static String asString(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

}
