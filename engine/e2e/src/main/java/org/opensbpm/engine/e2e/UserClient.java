package org.opensbpm.engine.e2e;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import org.opensbpm.engine.api.instance.*;
import org.opensbpm.engine.client.Credentials;
import org.opensbpm.engine.client.EngineServiceClient;
import org.opensbpm.engine.server.api.dto.instance.Audits;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

class UserClient {

    private static final Logger LOGGER = Logger.getLogger(UserClient.class.getName());

    public static UserClient of(Configuration configuration, Credentials credentials, ExecutorService executorService) {
        return new UserClient(
                configuration,
                credentials, executorService
        );
    }

    private final Object lock = new Object();
    //
    private final Set<TaskInfo> processedTasks = Collections.synchronizedSet(new HashSet<>());
    //
    private final Configuration configuration;
    private final EngineServiceClient engineServiceClient;
    private final ExecutorService taskExecutorService;
    //
    private List<TaskInfo> startedProcesses = Collections.emptyList();
    private Collection<ProcessInfo> processInfos;
    private Timer tasksFetcher;

    private UserClient(Configuration configuration, Credentials credentials, ExecutorService executorService) {
        this.configuration = configuration;
        engineServiceClient = this.configuration.createEngineServiceClient(credentials);
        this.taskExecutorService = executorService;

        LOGGER.info("User[" + getUserToken().getName() + "] with Roles " + getUserToken().getRoles().stream()
                .map(RoleToken::getName)
                .collect(Collectors.joining(",")));

    }

    public UserToken getUserToken() {
        return engineServiceClient.getUserToken();
    }

    public void startProcesses() {
        synchronized (lock) {
            startedProcesses = engineServiceClient.onEngineModelResource(modelResource-> modelResource.index().getProcessModelInfos()).stream()
                    .flatMap(model -> IntStream.range(0, configuration.getProcessCount()).boxed()
                            .parallel()
                            .map(idx
                                            -> {
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
            LOGGER.info("User[" + getUserToken().getName() + "] started "+startedProcesses.size()+" processes");
        }
    }

    public void startTaskFetcher() {
        tasksFetcher = new Timer("TasksFetcher for " + getUserToken().getName());
        tasksFetcher.schedule(new TimerTask() {
            @Override
            public void run() {
                LOGGER.finest("User[" + getUserToken().getName() + "] fetching tasks");
                engineServiceClient.onEngineTaskResource(taskResource -> taskResource.index(0,50).getTaskInfos()).stream()
                        .filter(taskInfo -> processedTasks.add(taskInfo))
                        .forEach(taskInfo -> {
                            taskExecutorService.submit(() -> {
                                new TaskExecutor(getUserToken(), engineServiceClient).execute(taskInfo);
                                processedTasks.remove(taskInfo);
                            });
                        });
            }
        }, 0, 500);
    }

    public void stopTaskFetcher() {
        LOGGER.info("User[" + getUserToken().getName() + "] stopping task-fetcher");
        tasksFetcher.cancel();
    }

    private static class TaskExecutor {

        private final UserToken userToken;
        private final EngineServiceClient engineServiceClient;

        public TaskExecutor(UserToken userToken, EngineServiceClient engineServiceClient) {
            this.userToken = userToken;
            this.engineServiceClient = engineServiceClient;
        }

        public void execute(TaskInfo taskInfo) {
            try {
                engineServiceClient.onEngineTaskResource(taskResource -> {
                    Task task = new Task(taskInfo, taskResource.retrieve(taskInfo.getId()));
                    if (task.getNextStates() == null || task.getNextStates().isEmpty()) {
                        LOGGER.log(Level.SEVERE, "no nextState for {0}", task);
                    } else {
                        Function<Task, NextState> stateValue = Utils::randomState;
                        Function<SimpleAttributeSchema, Serializable> fieldValue = Utils::createRandomValue;
                        task.getSchemas().forEach(objectSchema -> {
                            Map<Long, Serializable> attributeData = task.getObjectData(objectSchema).getData();
                            setValues(objectSchema.getAttributes(), attributeData, fieldValue);
                        });

                        NextState nextState = stateValue.apply(task);
                        LOGGER.log(Level.FINEST, "User[{0}]: executing task {0}", new Object[]{
                                userToken.getName(),
                                task
                        });
                        taskResource.submit(task.getId(), task.createTaskRequest(nextState));
                        LOGGER.log(Level.INFO, "User[{0}]: task {1} successfully changed to state {2}", new Object[]{
                                userToken.getName(),
                                task.getStateName(),
                                nextState.getName()
                        });
                    }
                    return null;
                });
            } catch (ProcessingException ex) {
                LOGGER.log(Level.FINER, ex.getMessage() /*, ex*/);
            } catch (WebApplicationException ex) {
                LOGGER.log(Level.FINE, "User[" + userToken.getName() + "] task " + taskInfo + ": " + ex.getMessage() /*, ex*/);
            } catch (Throwable ex) {
                //TODO handle uncaught exceptions correctly
                LOGGER.log(Level.SEVERE, "User[" + userToken.getName() + "] task " + taskInfo + ": " + ex.getMessage(), ex);
            }
        }

        private void setValues(List<AttributeSchema> attributes, Map<Long, Serializable> attributeData, Function<SimpleAttributeSchema, Serializable> fieldValue) {
            attributes.stream()
                    .forEach(attributeSchema -> {
                        attributeSchema.accept(new AttributeSchemaVisitor<Void>() {
                            @Override
                            public Void visitSimple(SimpleAttributeSchema attributeSchema) {
                                attributeData.put(attributeSchema.getId(), Utils.createRandomValue(attributeSchema));
                                return null;
                            }

                            @Override
                            public Void visitNested(NestedAttributeSchema attributeSchema) {
                                setValues(attributeSchema.getAttributes(),
                                        (Map<Long, Serializable>) attributeData.get(attributeSchema.getId()),
                                        fieldValue);
                                return null;
                            }

                            @Override
                            public Void visitIndexed(IndexedAttributeSchema attributeSchema) {
                                List<Map<Long, Serializable>> nestedList = (List<Map<Long, Serializable>>) attributeData.get(attributeSchema.getId());
                                if (nestedList == null) {
                                    nestedList = new ArrayList<>();
                                }
                                HashMap<Long, Serializable> data = new HashMap<>();
                                nestedList.add(data);
                                setValues(attributeSchema.getAttributes(),
                                        data,
                                        fieldValue);
                                return null;
                            }
                        });
                    });
        }

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

    public Stream<Statistics> getStatistics() {
        return getStartedProcesses(false).stream()
                .map(processInfo
                                -> engineServiceClient.onProcessInstanceResource(processInstanceResource -> {
                            Audits audits = processInstanceResource.retrieveAudit(processInfo.getId());
                            return new Statistics(
                                    processInfo.getStartTime(),
                                    processInfo.getEndTime(),
                                    audits.getAuditTrails().size());
                        })
                );
    }

}
