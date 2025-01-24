package org.opensbpm.engine.userbot.client;

import org.opensbpm.engine.api.instance.*;
import org.opensbpm.engine.client.EngineServiceClient;
import org.opensbpm.engine.server.api.dto.instance.Audits;
import org.opensbpm.engine.userbot.AppParameters;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class UserBot {

    private static final Logger LOGGER = Logger.getLogger(UserBot.class.getName());

    private final Object lock = new Object();
    //
    private final Set<TaskInfo> processedTasks = Collections.synchronizedSet(new HashSet<>());
    //
    private final EngineServiceClient engineServiceClient;
    private final ExecutorService taskExecutorService;
    private final ScheduledExecutorService tasksFetcher;

    private ScheduledExecutorService processStarter;
    //
    private List<TaskInfo> startedProcesses = Collections.emptyList();
    private Collection<ProcessInfo> processInfos;


    public UserBot(EngineServiceClient engineServiceClient) {
        this.engineServiceClient = engineServiceClient;
        taskExecutorService = Executors.newWorkStealingPool();
        tasksFetcher = Executors.newScheduledThreadPool(1);

        LOGGER.info("User[" + getUserToken().getName() + "] with Roles " + getUserToken().getRoles().stream()
                .map(RoleToken::getName)
                .collect(Collectors.joining(",")));
    }

    public UserToken getUserToken() {
        return engineServiceClient.getUserToken();
    }

    public void startProcesses(AppParameters appParameters) {
        LOGGER.info("User[" + getUserToken().getName() + "] start processes");
        if (appParameters.getStatistics().getInterval() != null) {
            startedProcesses = new ArrayList<>();

            processStarter = Executors.newScheduledThreadPool(1);
            processStarter.scheduleWithFixedDelay(() -> {
                try {
                    synchronized (lock) {
                        List<TaskInfo> processes = startProcesses(appParameters.getStatistics().getProcesses());
                        startedProcesses.addAll(processes);
                        LOGGER.info("User[" + getUserToken().getName() + "] started " + startedProcesses.size() + " processes");
                    }
                } catch (Throwable ex) {
                    //TODO handle uncaught exceptions correctly
                    LOGGER.log(Level.SEVERE, "User[" + getUserToken().getName() + "] : " + ex.getMessage(), ex);
                }
                if (startedProcesses.size() >= appParameters.getStatistics().getInterval() * appParameters.getStatistics().getProcesses()) {
                    processStarter.shutdown();
                }
            }, 10, appParameters.getStatistics().getInterval(), TimeUnit.SECONDS);
        } else {
            synchronized (lock) {
                startedProcesses = startProcesses(appParameters.getStatistics().getProcesses());
                LOGGER.info("User[" + getUserToken().getName() + "] started " + startedProcesses.size() + " processes");
            }
        }
    }

    private List<TaskInfo> startProcesses(Integer processes) {
        return engineServiceClient.onEngineModelResource(modelResource -> modelResource.index().getProcessModelInfos()).stream()
                .flatMap(model -> IntStream.range(0, processes).boxed()
                        .parallel()
                        .map(idx -> {
                                    LOGGER.fine("User[" + getUserToken().getName() + "] starting process " + model.getName());
                                    TaskInfo taskInfo = engineServiceClient.onEngineModelResource(modelResource -> modelResource.start(model.getId()));
                                    processedTasks.add(taskInfo);
                                    taskExecutorService.submit(() -> {
                                        new TaskExecutor(getUserToken(), engineServiceClient).execute(taskInfo);
                                        processedTasks.remove(taskInfo);
                                    });
                                    return taskInfo;
                                }
                        )
                )
                .toList();
    }

    public void startTaskFetcher() {
        LOGGER.info("User[" + getUserToken().getName() + "] start tasks-fetcher");
        tasksFetcher.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try {
                    LOGGER.info("User[" + getUserToken().getName() + "] fetching tasks");

                    getTaskInfos().stream()
                            .filter(taskInfo -> processedTasks.add(taskInfo))
                            .forEach(taskInfo -> {
                                try {
                                    taskExecutorService.submit(() -> {
                                        new TaskExecutor(getUserToken(), engineServiceClient).execute(taskInfo);
                                        processedTasks.remove(taskInfo);
                                    });
                                } catch (RejectedExecutionException e) {
                                    LOGGER.warning("User[" + getUserToken().getName() + "] task-fetcher " + e.getMessage());
                                }
                            });
//                }
                } catch (Throwable ex) {
                    //TODO handle uncaught exceptions correctly
                    LOGGER.log(Level.SEVERE, "User[" + getUserToken().getName() + "] : " + ex.getMessage(), ex);
                }
            }

            private List<TaskInfo> getTaskInfos() {
                return engineServiceClient.onEngineTaskResource(taskResource -> taskResource.index().getTaskInfos());
            }

        }, 1, 500, TimeUnit.MILLISECONDS);
    }

    public void stopTaskFetcher() {
        LOGGER.info("User[" + getUserToken().getName() + "] stopping tasks-fetcher");
        tasksFetcher.shutdown();
        taskExecutorService.shutdown();
    }

    public List<TaskInfo> getStartedProcesses() {
        return startedProcesses;
    }

    public Collection<ProcessInfo> getStartedProcesses(boolean refresh) {
        if (processInfos == null || refresh)
            synchronized (lock) {
                processInfos = startedProcesses.stream()
                        .map(task -> engineServiceClient.onProcessInstanceResource(processInstanceResource
                                -> {
                            LOGGER.finest("retrieve info about" + task.getProcessId());
                            return processInstanceResource.retrieve(task.getProcessId());
                        }))
                        .toList();
            }
        return processInfos;
    }

    public List<ProcessInfo> getActiveProcesses() {
        return getStartedProcesses(true).stream()
                .filter(processInfo -> processInfo.getState() == ProcessInstanceState.ACTIVE)
                .toList();
    }

    public void killActiveProcesses() {
        List<ProcessInfo> processInfos = engineServiceClient.onProcessInstanceResource(processInstanceResource
                -> getActiveProcesses().stream()
                .map(process -> process.getId())
                .map(processId -> processInstanceResource.stop(processId))
                .toList());
        LOGGER.info("Killed " + processInfos.stream()
                .map(processInfo -> processInfo.toString())
                .collect(Collectors.joining("\n"))
        );
    }

    public List<Statistics> getStatistics() {
        return getStartedProcesses(false).stream()
                .map(processInfo
                                -> engineServiceClient.onProcessInstanceResource(processInstanceResource -> {
                            Audits audits = processInstanceResource.retrieveAudit(processInfo.getId());
                            return new Statistics(
                                    processInfo.getStartTime(),
                                    processInfo.getEndTime(),
                                    audits.getAuditTrails().size());
                        })
                )
                .toList();
    }

}
