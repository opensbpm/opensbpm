package org.opensbpm.engine.stresstestworker.starter;

import org.opensbpm.engine.api.instance.ProcessInfo;
import org.opensbpm.engine.api.model.ProcessModelInfo;
import org.opensbpm.engine.client.Credentials;
import org.opensbpm.engine.client.EngineServiceClient;
import org.opensbpm.engine.stresstest.UserClient;
import org.opensbpm.engine.stresstestworker.AppParameters;
import org.opensbpm.engine.stresstestworker.WorkflowOrchestrator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.lang.String.format;

@ConditionalOnProperty(value="opensbpm.starter", havingValue = "true")
@Component
public class StarterWorkflowOrchestrator implements WorkflowOrchestrator {
    private static final Logger LOGGER = Logger.getLogger(StarterWorkflowOrchestrator.class.getName());

    private final AppParameters appParameters;
    private final UserClient userClient;
    private final WebdavUploader webdavUploader;

    public StarterWorkflowOrchestrator(AppParameters appParameters, UserClient userClient, WebdavUploader webdavUploader) {
        this.appParameters = appParameters;
        this.userClient = userClient;
        this.webdavUploader = webdavUploader;
    }

    public void execute(ConfigurableApplicationContext context) {
        uploadModel();

        LocalDateTime startTime = LocalDateTime.now();
        userClient.startProcesses(appParameters.getStatistics().getProcesses());
        userClient.startTaskFetcher();

        waitFinished();

        userClient.stopTaskFetcher();

        LocalDateTime endTime = LocalDateTime.now();

        webdavUploader.uploadStatistic(startTime, endTime, userClient.getStatistics());
    }

    private void uploadModel() {
        try (InputStream modelResource = StarterWorkflowOrchestrator.class.getResourceAsStream("/models/" + "dienstreiseantrag.xml")) {
            EngineServiceClient adminClient = appParameters.createEngineServiceClient(Credentials.of("admin", "admin".toCharArray()));
            ProcessModelInfo processModelInfo = adminClient.onProcessModelResource(processModelResource -> processModelResource.create(modelResource));
            LOGGER.info("ProcessModel " + processModelInfo.getName() + " uploaded");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void waitFinished() {
        boolean starterFinished = false;
        while (!starterFinished) {
            LOGGER.finest("Check started processes finished");
            starterFinished = userClient.getActiveProcesses().isEmpty();

            if (LOGGER.isLoggable(Level.FINEST)) {
                List<ProcessInfo> activeProcesses = userClient.getActiveProcesses();
                if (!activeProcesses.isEmpty()) {
                    LOGGER.finest("User[" + userClient.getUserToken().getName() + "] has active processes " +
                            activeProcesses.stream()
                                    .map(processInfo -> asString(processInfo))
                                    .collect(Collectors.joining(","))
                    );
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
