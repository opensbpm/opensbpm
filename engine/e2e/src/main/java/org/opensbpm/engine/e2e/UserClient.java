package org.opensbpm.engine.e2e;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.ProcessingException;
import org.opensbpm.engine.api.instance.*;
import org.opensbpm.engine.client.Credentials;
import org.opensbpm.engine.client.EngineServiceClient;
import org.opensbpm.engine.server.api.EngineResource.ProcessModelResource;
import org.opensbpm.engine.server.api.EngineResource.ProcessInstanceResource;
import org.opensbpm.engine.server.api.EngineResource.TaskResource;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

class UserClient {
    private static final Logger LOGGER = Logger.getLogger(UserClient.class.getName());
    private final static TaskInfo TASK_EMPTY = new TaskInfo();

    public static UserClient of(Configuration configuration, String userName, String password) {
        return new UserClient(
                configuration,
                Credentials.of(userName, password.toCharArray())
        );
    }

    //
    private final BlockingQueue<TaskInfo> tasks = new LinkedBlockingDeque<>();
    private final Set<TaskInfo> processedTasks = new HashSet<>();
    private final ExecutorService taskExecutorService = Executors.newWorkStealingPool(1);
    //
    private final EngineServiceClient engineServiceClient;
    //
    private UserToken userToken;
    private List<TaskInfo> startedProcesses;
    private Timer tasksFetcher;


    private UserClient(Configuration configuration, Credentials credentials) {
        if (configuration.hasAuthUrl()) {
            engineServiceClient = EngineServiceClient.create(configuration.getAuthUrl(), configuration.getUrl(), credentials);
        } else {
            engineServiceClient = EngineServiceClient.create(configuration.getUrl(), credentials);
        }
    }

    public UserToken getUserToken() {
        if (userToken == null)
            userToken = engineServiceClient.getUserResource().info();
        return userToken;
    }

    private Long getUserId() {
        return getUserToken().getId();
    }

    private ProcessModelResource getProcessModelResource() {
        return engineServiceClient.getEngineResource().getProcessModelResource(getUserId());
    }

    private ProcessInstanceResource getProcessInstanceResource() {
        return engineServiceClient.getEngineResource().getProcessInstanceResource(getUserId());
    }

    private TaskResource getTaskResource() {
        return engineServiceClient.getEngineResource().getTaskResource(getUserId());
    }

    void start() {
        LOGGER.info("User[" + getUserToken().getName() + "] with Roles " + getUserToken().getRoles().stream()
                .map(RoleToken::getName)
                .collect(Collectors.joining(",")));

        startedProcesses = getProcessModelResource().index().getProcessModelInfos().parallelStream()
                .map(model -> {
                    LOGGER.info("User[" + getUserToken().getName() + "] starting process " + model.getName());
                    return getProcessModelResource().start(model.getId());
                })
                .toList();
        tasks.addAll(startedProcesses);

        tasksFetcher = new Timer("TasksFetcher for " + getUserToken().getName());
        tasksFetcher.schedule(new TimerTask() {
            @Override
            public void run() {
                LOGGER.finest("User[" + getUserToken().getName() + "] fetching tasks");
                getTaskResource().index().getTaskInfos().stream()
                        .filter(taskInfo -> !tasks.contains(taskInfo) && !processedTasks.contains(taskInfo))
                        .forEach(taskInfo -> {
                            try {
                                tasks.put(taskInfo);
                            } catch (InterruptedException ex) {
                                LOGGER.info("User[" + getUserToken().getName() + "]: " + ex.getMessage());
                                // Restore interrupted state...
                                Thread.currentThread().interrupt();
                            }
                        });
            }
        }, 50, 50);

        taskExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    TaskInfo taskInfo;
                    while ((taskInfo = tasks.take()) != null) {
                        processedTasks.add(taskInfo);
                        executeTask(taskInfo);
                    }
                } catch (InterruptedException ex) {
                    LOGGER.log(Level.SEVERE, ex.getMessage());
                    // Restore interrupted state...
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

    public void stop() throws InterruptedException, ExecutionException {
        LOGGER.info("User[" + getUserToken().getName() + "] closing client");
        tasksFetcher.cancel();
        taskExecutorService.shutdown();
    }

    public void executeTask(TaskInfo taskInfo) {
        try {
            Task task = new Task(taskInfo, getTaskResource().retrieve(taskInfo.getId()));
            if (task.getNextStates() == null || task.getNextStates().isEmpty()) {
                LOGGER.log(Level.SEVERE, "no nextState for {0}", task);
            } else {
                executeTask(task, Utils::randomState, Utils::createRandomValue);
            }
        } catch (ProcessingException ex) {
            LOGGER.log(Level.FINE, ex.getMessage() /*, ex*/);
        } catch (ClientErrorException ex) {
            LOGGER.log(Level.SEVERE, "User[" + this.getUserToken().getName() + "] task " + taskInfo + ": " + ex.getMessage() /*, ex*/);
        } catch (Throwable ex) {
            //TODO handle uncaught exceptions correctly
            LOGGER.log(Level.SEVERE, "User[" + this.getUserToken().getName() + "] task " + taskInfo + ": " + ex.getMessage(), ex);
        }
    }

    private void executeTask(Task task, Function<Task, NextState> stateValue, Function<SimpleAttributeSchema, Serializable> fieldValue) {
        task.getSchemas().forEach(objectSchema -> {
            Map<Long, Serializable> attributeData = task.getObjectData(objectSchema).getData();
            setValues(objectSchema.getAttributes(), attributeData, fieldValue);
        });

        NextState nextState = stateValue.apply(task);
        LOGGER.log(Level.FINEST, "User[{0}]: executing task {0}", new Object[]{
                this.getUserToken().getName(),
                task
        });
        getTaskResource().submit(task.getId(), task.createTaskRequest(nextState));
        LOGGER.log(Level.INFO, "User[{0}]: task {1} successfully changed to state {2}", new Object[]{
                this.getUserToken().getName(),
                task.getStateName(),
                nextState.getName()
        });
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

    public List<ProcessInfo> getActiveProcesses() {
        return startedProcesses.stream()
                .map(task -> {
                    try {
                        return getProcessInstanceResource().retrieve(task.getProcessId());
                    } catch (Exception ex) {
                        LOGGER.info("User[" + getUserToken().getName() + "] process " + task.getProcessId() + ": " + ex.getMessage());
                        //LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .filter(processInfo -> processInfo.getState() == ProcessInstanceState.ACTIVE)
                .toList();
    }
}
