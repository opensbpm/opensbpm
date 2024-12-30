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
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Arrays.asList;

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
    private final ExecutorService taskExecutorService = Executors.newFixedThreadPool(10);
    //
    private final EngineServiceClient engineServiceClient;
    //
    private UserToken userToken;
    private List<TaskInfo> startedProcesses = Collections.emptyList();
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

    private TaskResource newTaskResource() {
        return engineServiceClient.newEngineResource().getTaskResource(getUserId());
    }

    public void start() {
        LOGGER.info("User[" + getUserToken().getName() + "] with Roles " + getUserToken().getRoles().stream()
                .map(RoleToken::getName)
                .collect(Collectors.joining(",")));

        startedProcesses = getProcessModelResource().index().getProcessModelInfos().stream()
                .flatMap(model -> {
                    LOGGER.info("User[" + getUserToken().getName() + "] starting process " + model.getName());
                    return IntStream.range(0, 500).boxed()
                            .map(idx -> getProcessModelResource().start(model.getId()));
                })
                .toList();
        tasks.addAll(startedProcesses);

        tasksFetcher = new Timer("TasksFetcher for " + getUserToken().getName());
        tasksFetcher.schedule(new TimerTask() {
            @Override
            public void run() {
                LOGGER.finest("User[" + getUserToken().getName() + "] fetching tasks");
                newTaskResource().index().getTaskInfos().stream()
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
                        new TaskExecutor(getUserToken(), engineServiceClient).execute(taskInfo);
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

    private static class TaskExecutor {
        private final UserToken userToken;
        private final EngineServiceClient engineServiceClient;

        public TaskExecutor(UserToken userToken, EngineServiceClient engineServiceClient) {
            this.userToken = userToken;
            this.engineServiceClient = engineServiceClient;
        }

        private TaskResource newTaskResource() {
            return engineServiceClient.newEngineResource().getTaskResource(userToken.getId());
        }

        public void execute(TaskInfo taskInfo) {
            try {
                Task task = new Task(taskInfo, newTaskResource().retrieve(taskInfo.getId()));
                if (task.getNextStates() == null || task.getNextStates().isEmpty()) {
                    LOGGER.log(Level.SEVERE, "no nextState for {0}", task);
                } else {
                    execute(task, Utils::randomState, Utils::createRandomValue);
                }
            } catch (ProcessingException ex) {
                LOGGER.log(Level.FINE, ex.getMessage() /*, ex*/);
            } catch (ClientErrorException ex) {
                LOGGER.log(Level.SEVERE, "User[" + userToken.getName() + "] task " + taskInfo + ": " + ex.getMessage() /*, ex*/);
                if(ex.getResponse().getStatus() == 401){
                    engineServiceClient.refreshToken();
                }
            } catch (Throwable ex) {
                //TODO handle uncaught exceptions correctly
                LOGGER.log(Level.SEVERE, "User[" + userToken.getName() + "] task " + taskInfo + ": " + ex.getMessage(), ex);
            }
        }

        private void execute(Task task, Function<Task, NextState> stateValue, Function<SimpleAttributeSchema, Serializable> fieldValue) {
            task.getSchemas().forEach(objectSchema -> {
                Map<Long, Serializable> attributeData = task.getObjectData(objectSchema).getData();
                setValues(objectSchema.getAttributes(), attributeData, fieldValue);
            });

            NextState nextState = stateValue.apply(task);
            LOGGER.log(Level.FINEST, "User[{0}]: executing task {0}", new Object[]{
                    userToken.getName(),
                    task
            });
            newTaskResource().submit(task.getId(), task.createTaskRequest(nextState));
            LOGGER.log(Level.INFO, "User[{0}]: task {1} successfully changed to state {2}", new Object[]{
                    userToken.getName(),
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

    }

    public List<ProcessInfo> getActiveProcesses() {
        return startedProcesses.stream()
                .map(task -> {
                    try {
                        return getProcessInstanceResource().retrieve(task.getProcessId());
                    } catch (ClientErrorException ex) {
                        LOGGER.log(Level.SEVERE, "User[" + userToken.getName() + "] process " + task.getProcessId() + ": " + ex.getMessage() /*, ex*/);
                        if(ex.getResponse().getStatus() == 401){
                            engineServiceClient.refreshToken();
                        }
                        return null;
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
