package org.opensbpm.engine.stresstest.starter;

import org.opensbpm.engine.client.userbot.Statistics;
import org.opensbpm.engine.stresstest.AppParameters;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@ConditionalOnBean(StarterWorkflowOrchestrator.class)
@Component
public class WebdavUploader {
    private static final Logger LOGGER = Logger.getLogger(WebdavUploader.class.getName());
    private final AppParameters appParameters;

    public WebdavUploader(AppParameters appParameters) {
        this.appParameters = appParameters;
    }

    public void uploadStatistic(LocalDateTime startTime, LocalDateTime endTime, List<Statistics> statistics) {
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
                                .append(appParameters.getStatistics().getNodes()).append(",")
                                .append(appParameters.getStatistics().getPods()).append(",")
                                .append(asString(startTime)).append(",")
                                .append(asString(endTime)).append(",")
                                .append(duration.toMillis()).append(",")
                                .append(testTaskCount).append(",")
                                .append(appParameters.getStatistics().getProcesses()).append(",")
                                .append(asString(statistic.getStartTime())).append(",")
                                .append(asString(statistic.getEndTime())).append(",")
                                .append(statistic.getDuration().toMillis()).append(",")
                                .append(statistic.getCount())
                                .toString()
                )
                .collect(Collectors.joining("\n"));
        builder.append(data).append("\n");
        String statData = builder.toString();

        uploadData(statData);

        LOGGER.info("Everything done");
    }

    private void uploadData(String statData) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
        String fileName = String.format("stresstest_%s_%s.csv",
                LocalDateTime.now().format(formatter),
                appParameters.getStatistics().getProcesses()
        );

        RestTemplate restTemplate = new RestTemplateBuilder()
                .basicAuthentication(appParameters.getStatistics().getUsername(), appParameters.getStatistics().getPassword())
                .build();
        restTemplate.put(String.format("%s/%s", appParameters.getStatistics().getUrl(), fileName), statData);
    }

    private static String asString(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

}
