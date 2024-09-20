package org.opensbpm.engine.e2e;

import jakarta.ws.rs.ProcessingException;
import org.opensbpm.engine.api.instance.*;
import org.opensbpm.engine.client.Credentials;
import org.opensbpm.engine.client.EngineServiceClient;
import org.opensbpm.engine.server.api.EngineResource;

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
    private final EngineServiceClient engineServiceClient;
    private final ArrayBlockingQueue<TaskInfo> tasksQueue;
    //
    private UserToken userToken;
    private List<Long> startedProcesses;
    private Future<Boolean> taskWatcher;
    private Timer tasksFetcher;


    private UserClient(Configuration configuration, Credentials credentials) {
        engineServiceClient = EngineServiceClient.create(configuration.getUrl(), credentials);
        tasksQueue = new ArrayBlockingQueue<>(20);
    }

    public synchronized EngineServiceClient getEngineServiceClient() {
        return engineServiceClient;
    }

    public UserToken getUserToken() {
        if (userToken == null)
            userToken = getEngineServiceClient().getUserResource().info();
        return userToken;
    }

    private Long getUserId() {
        return getUserToken().getId();
    }

    private EngineResource.ProcessModelResource getProcessModelResource() {
        return getEngineServiceClient().getEngineResource().getProcessModelResource(getUserId());
    }

    private EngineResource.ProcessInstanceResource getProcessInstanceResource() {
        return getEngineServiceClient().getEngineResource().getProcessInstanceResource(getUserId());
    }

    private EngineResource.TaskResource getTaskResource() {
        return getEngineServiceClient().getEngineResource().getTaskResource(getUserId());
    }

    void start() {
        System.out.println("User: " + getUserToken().getName() + " Roles: " + getUserToken().getRoles().stream()
                .map(RoleToken::getName)
                .collect(Collectors.joining(",")));

        tasksFetcher = new Timer("TasksFetcher for " + getUserToken().getName());
        tasksFetcher.schedule(new TimerTask() {
            @Override
            public void run() {
                LOGGER.finer("fetching tasks for user " + getUserToken().getName());
                putAll(getTaskResource().index().getTaskInfos());
            }
        }, 100, 750);

        List<TaskInfo> taskInfos = getProcessModelResource().index().getProcessModelInfos().parallelStream()
                .map(model -> {
                    LOGGER.info("starting process " + model.getName() + " for user " + getUserToken().getName());
                    return getProcessModelResource().start(model.getId());
                })
                .toList();

        startedProcesses = taskInfos.stream()
                .map(TaskInfo::getProcessId)
                .toList();
        putAll(taskInfos);

        ExecutorService singleExecutor = Executors.newWorkStealingPool(1);
        taskWatcher = singleExecutor.submit(() -> {
            while (true) {
                TaskInfo taskInfo = tasksQueue.take();
                if (TASK_EMPTY == taskInfo) {
                    //got some poison...
                    return true;
                }
                executeTask(taskInfo);
            }
        });
        singleExecutor.shutdown();

    }

    private void putAll(List<TaskInfo> taskInfos) {
        taskInfos.forEach(taskInfo -> {
            try {
                tasksQueue.put(taskInfo);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void stop() throws InterruptedException, ExecutionException {
        if (taskWatcher != null) {
            //offer some poison
            tasksQueue.put(TASK_EMPTY);
            //after poison, wait for correct shutdown
            taskWatcher.get();
        }

        tasksFetcher.cancel();
        tasksFetcher.purge();
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
        } catch (Throwable ex) {
            //TODO handle uncaught exceptions correctly
            LOGGER.log(Level.SEVERE, "User[" + this.getUserToken().getName() + "," + taskInfo + "]:" + ex.getMessage(), ex);
        }
    }

    private void executeTask(Task task, Function<Task, NextState> stateValue, Function<SimpleAttributeSchema, Serializable> fieldValue){
        task.getSchemas().forEach(objectSchema -> {
            Map<Long, Serializable> attributeData = task.getObjectData(objectSchema).getData();
            setValues(objectSchema.getAttributes(), attributeData, fieldValue);
        });

        NextState nextState = stateValue.apply(task);
        LOGGER.log(Level.INFO, "executing task {0}", task);
        getTaskResource().submit(task.getId(), task.createTaskRequest(nextState));
        LOGGER.log(Level.INFO, "successfull changed to state {0}", nextState);
    }

    private void setValues(List<AttributeSchema> attributes, Map<Long, Serializable> attributeData, Function<SimpleAttributeSchema, Serializable> fieldValue) {
        attributes.stream()
                .forEach(attributeSchema -> {
                    attributeSchema.accept(new AttributeSchemaVisitor<Void>() {
                        @Override
                        public Void visitSimple(SimpleAttributeSchema attributeSchema) {
                            attributeData.put(attributeSchema.getId(), fieldValue.apply(attributeSchema));
                            return null;
                        }

                        @Override
                        public Void visitReference(ReferenceAttributeSchema attributeSchema) {
//                            setValues(attributeSchema.getAttributes(),
//                                    (Map<Long, Serializable>) attributeData.get(attributeSchema.getId()),
//                                    fieldValue);
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
                .map(processId -> getProcessInstanceResource().retrieve(processId))
                .filter(processInfo -> processInfo.getState() == ProcessInstanceState.ACTIVE)
                .toList();
    }
}
