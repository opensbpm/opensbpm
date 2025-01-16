package org.opensbpm.engine.stresstest.starter;

import org.opensbpm.engine.api.instance.ProcessInfo;
import org.opensbpm.engine.api.model.ProcessModelInfo;
import org.opensbpm.engine.client.Credentials;
import org.opensbpm.engine.client.EngineServiceClient;
import org.opensbpm.engine.client.userbot.UserBot;
import org.opensbpm.engine.stresstest.AppParameters;
import org.opensbpm.engine.stresstest.WorkflowOrchestrator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.lang.String.format;

@ConditionalOnProperty(value="opensbpm.starter", havingValue = "true")
@Component
public class StarterWorkflowOrchestrator implements WorkflowOrchestrator {
    private static final Logger LOGGER = Logger.getLogger(StarterWorkflowOrchestrator.class.getName());

    private final AppParameters appParameters;
    private final UserBot userBot;
    private final WebdavUploader webdavUploader;

    public StarterWorkflowOrchestrator(AppParameters appParameters, UserBot userBot, WebdavUploader webdavUploader) {
        this.appParameters = appParameters;
        this.userBot = userBot;
        this.webdavUploader = webdavUploader;
    }

    public void execute(ConfigurableApplicationContext context) {
        uploadModel();

        LocalDateTime startTime = LocalDateTime.now();
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() -> userBot.startProcesses(appParameters.getStatistics().getProcesses()));
        executorService.shutdown();
        userBot.startTaskFetcher();

        waitFinished();

        userBot.stopTaskFetcher();

        LocalDateTime endTime = LocalDateTime.now();

        webdavUploader.uploadStatistic(startTime, endTime, userBot.getStatistics());

        SpringApplication.exit(context);
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
        int activeCount = Integer.MAX_VALUE;
        while (activeCount > 0) {
            List<ProcessInfo> activeProcesses = userBot.getActiveProcesses();
            if(activeCount == activeProcesses.size()) {
                LOGGER.info("Still " + activeProcesses.size() + " processes running:\n"+
                        activeProcesses.stream()
                                .map(processInfo -> asString(processInfo))
                                .collect(Collectors.joining(",\n"))
                );
                userBot.killActiveProcesses();

            }else {
                LOGGER.info("Still " + activeProcesses.size() + " processes running");
            }
            activeCount = activeProcesses.size();

            try {
                Thread.sleep(2000);
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
                        .map(subjectStateInfo -> {
                            return "{"+subjectStateInfo.getSubjectName()+"->"+ subjectStateInfo.getStateName()+"}";
                        })
                        .collect(Collectors.joining(","))
        );
    }

}
