package org.opensbpm.engine.e2e.multipod;

import org.opensbpm.engine.api.instance.ProcessInfo;
import org.opensbpm.engine.api.model.ProcessModelInfo;
import org.opensbpm.engine.client.Credentials;
import org.opensbpm.engine.client.EngineServiceClient;
import org.opensbpm.engine.e2e.AppParameters;
import org.opensbpm.engine.stresstest.Statistics;
import org.opensbpm.engine.stresstest.UserClient;
import org.opensbpm.engine.e2e.statistics.WebdavUploader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.stereotype.Component;

import jakarta.jms.JMSException;
import jakarta.jms.Session;
import jakarta.jms.Message;

import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.lang.String.format;

@ConditionalOnBean(MultiPodWorkflowExecution.class)
@Component
public class ControllerService {

    private static final Logger LOGGER = Logger.getLogger(ControllerService.class.getName());


    private final ApplicationContext applicationContext;
    private final AppParameters appParameters;

    private final JmsTemplate jmsTemplate;
    private final WebdavUploader uploaderService;
    private LocalDateTime startTime;

    public ControllerService(ApplicationContext applicationContext, AppParameters appParameters, JmsTemplate jmsTemplate, WebdavUploader uploaderService) {
        this.applicationContext = applicationContext;
        this.appParameters = appParameters;
        this.jmsTemplate = jmsTemplate;
        this.uploaderService = uploaderService;
    }

    public void onApplicationEvent(ContextRefreshedEvent event) {
        LOGGER.info("running");

        if (appParameters.isIndexed()) {
            if(appParameters.getJobCompletionIndex() == 0){
                //act as controller
                uploadModel();
                jmsTemplate.send("models", new MessageCreator() {
                    public Message createMessage(Session session) throws JMSException {
                        return session.createTextMessage("created");
                    }
                });


            }else{


            }
        }
    }

    private void uploadModel() {
        try {
            EngineServiceClient adminClient = appParameters.createEngineServiceClient(Credentials.of("admin", "admin".toCharArray()));
            InputStream modelResource = ControllerService.class.getResourceAsStream("/models/" + "dienstreiseantrag.xml");
            ProcessModelInfo processModelInfo = adminClient.onProcessModelResource(processModelResource -> processModelResource.create(modelResource));
            LOGGER.info("ProcessModel " + processModelInfo.getName() + " uploaded");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @JmsListener(destination = "user")
    public void finishedProcesses(List<Statistics> statistics) {
        System.out.println(statistics);
    }

    private void executeSinglePod() {
        startTime = LocalDateTime.now();

        Collection<UserClient> userClients = applicationContext.getBeansOfType(Credentials.class).values().stream()
                .map(credentials -> new UserClient(appParameters.createEngineServiceClient(credentials)))
                .toList();

        ExecutorService taskExecutorService = Executors.newWorkStealingPool();
        userClients.forEach(client -> taskExecutorService.submit(() -> client.startProcesses(appParameters.getProcessCount())));
        userClients.forEach(client -> taskExecutorService.submit(() -> client.startTaskFetcher()));
        taskExecutorService.shutdown();
        try {
            taskExecutorService.awaitTermination(1, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        boolean allFinished = false;
        while (!allFinished) {
            LOGGER.finest("Check started processes finished");
            allFinished = userClients.stream()
                    .mapToLong(userClient -> userClient.getActiveProcesses().size())
                    .sum() == 0;

            if (LOGGER.isLoggable(Level.FINEST)) {
                for (UserClient userClient : userClients) {
                    List<ProcessInfo> activeProcesses = userClient.getActiveProcesses();
                    if (!activeProcesses.isEmpty()) {
                        LOGGER.finest("User[" + userClient.getUserToken().getName() + "] has active processes " +
                                activeProcesses.stream()
                                        .map(processInfo -> asString(processInfo))
                                        .collect(Collectors.joining(","))
                        );
                    }
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                // Restore interrupted state...
                Thread.currentThread().interrupt();
            }
        }
        userClients.forEach(UserClient::stopTaskFetcher);

        LocalDateTime endTime = LocalDateTime.now();
        LOGGER.info("All started processes finished");

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
        List<Statistics> statistics = userClients.stream()
                .flatMap(userClient -> userClient.getStatistics().stream())
                .toList();
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

    private static String asString(ProcessInfo processInfo) {
        return format("%s started at %s by %s in state %s",
                processInfo.getProcessModelInfo().getName(),
                processInfo.getStartTime(),
                processInfo.getOwner().getName(),
                processInfo.getSubjects().stream()
                        .map(ProcessInfo.SubjectStateInfo::getStateName)
                        .collect(Collectors.joining(","))
        );
    }

}
