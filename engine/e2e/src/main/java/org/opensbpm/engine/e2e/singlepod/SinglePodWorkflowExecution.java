package org.opensbpm.engine.e2e.singlepod;

import org.opensbpm.engine.api.instance.ProcessInfo;
import org.opensbpm.engine.api.model.ProcessModelInfo;
import org.opensbpm.engine.client.Credentials;
import org.opensbpm.engine.client.EngineServiceClient;
import org.opensbpm.engine.e2e.AppParameters;
import org.opensbpm.engine.stresstest.UserClient;
import org.opensbpm.engine.e2e.WorkflowExecution;
import org.opensbpm.engine.stresstest.Statistics;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.lang.String.format;

@ConditionalOnProperty(value="e2e.execution", havingValue = "single", matchIfMissing = true)
@Component
public class SinglePodWorkflowExecution implements WorkflowExecution {

    private static final Logger LOGGER = Logger.getLogger(SinglePodWorkflowExecution.class.getName());

    private final ApplicationContext applicationContext;
    private final AppParameters appParameters;

    public SinglePodWorkflowExecution(ApplicationContext applicationContext, AppParameters appParameters) {
        this.applicationContext = applicationContext;
        this.appParameters = appParameters;
    }

    public List<Statistics> execute() {
        LOGGER.info("run on single pod");

        uploadModel();
        return executeSinglePod();
    }

    private void uploadModel() {
        try {
            EngineServiceClient adminClient = appParameters.createEngineServiceClient(Credentials.of("admin", "admin".toCharArray()));
            InputStream modelResource = SinglePodWorkflowExecution.class.getResourceAsStream("/models/" + "dienstreiseantrag.xml");
            ProcessModelInfo processModelInfo = adminClient.onProcessModelResource(processModelResource -> processModelResource.create(modelResource));
            LOGGER.info("ProcessModel " + processModelInfo.getName() + " uploaded");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private List<Statistics> executeSinglePod() {
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

        LOGGER.info("All started processes finished");

        return userClients.stream()
                .flatMap(userClient -> userClient.getStatistics().stream())
                .toList();
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
