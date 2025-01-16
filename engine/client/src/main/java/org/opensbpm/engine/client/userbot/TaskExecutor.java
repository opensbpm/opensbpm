package org.opensbpm.engine.client.userbot;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import org.opensbpm.engine.api.instance.*;
import org.opensbpm.engine.client.EngineServiceClient;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

class TaskExecutor {
    private static final Logger LOGGER = Logger.getLogger(TaskExecutor.class.getName());

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
