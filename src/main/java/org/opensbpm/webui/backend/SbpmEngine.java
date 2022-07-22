package org.opensbpm.webui.backend;

import org.opensbpm.engine.api.EngineService;
import org.opensbpm.engine.api.EngineService.ObjectRequest;
import org.opensbpm.engine.api.ModelNotFoundException;
import org.opensbpm.engine.api.UserNotFoundException;
import org.opensbpm.engine.api.UserTokenService;
import org.opensbpm.engine.api.instance.AutocompleteResponse;
import org.opensbpm.engine.api.instance.TaskInfo;
import org.opensbpm.engine.api.instance.TaskNotFoundException;
import org.opensbpm.engine.api.instance.TaskOutOfDateException;
import org.opensbpm.engine.api.instance.TaskRequest;
import org.opensbpm.engine.api.instance.UserToken;
import org.opensbpm.engine.api.model.ProcessModelInfo;
import org.opensbpm.webui.backend.authentication.SpringAuthentication;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Stream;
import org.opensbpm.engine.api.ModelService.ModelRequest;
import org.opensbpm.engine.api.instance.Task;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SbpmEngine {

    private final EngineService engineService;
    private final UserTokenService userTokenService;

    public SbpmEngine(EngineService engineService, UserTokenService userTokenService) {
        this.engineService = engineService;
        this.userTokenService = userTokenService;
    }

    private UserToken getCurrentUserToken() throws UserNotFoundException {
        return userTokenService.retrieveToken(SpringAuthentication.of(SecurityContextHolder.getContext().getAuthentication()));
    }

    public Collection<ProcessModelInfo> findStartableProcessModels() throws UserNotFoundException {
        return engineService.findStartableProcessModels(getCurrentUserToken());
    }

    public TaskInfo startProcess(ProcessModelInfo processModel) throws UserNotFoundException, ModelNotFoundException {
        return engineService.startProcess(getCurrentUserToken(), ModelRequest.of(processModel));
    }

    public Boolean executeTask(TaskRequest taskRequest) throws UserNotFoundException, TaskNotFoundException, TaskOutOfDateException {
        return engineService.executeTask(getCurrentUserToken(), taskRequest);
    }

    public Stream<TaskInfo> getTasks(String filter) throws UserNotFoundException {
        return getAllTasks()
                .filter(taskInfo -> taskInfo.getProcessName().contains(filter));
    }

    public Task getTask(Long taskId) throws TaskOutOfDateException, TaskNotFoundException, UserNotFoundException {
        TaskInfo taskInfo = getAllTasks()
                .filter(task -> Objects.equals(task.getId(), taskId))
                .findFirst()
                .orElseThrow(() -> new TaskOutOfDateException(String.valueOf(taskId)));
        return new Task(taskInfo, engineService.getTaskResponse(getCurrentUserToken(), taskInfo));
    }

    private Stream<TaskInfo> getAllTasks() throws UserNotFoundException {
        UserToken userToken = getCurrentUserToken();
        return engineService.getTasks(userToken).stream();
    }

    public TaskInfo getNextTask(TaskInfo taskInfo) throws UserNotFoundException {
        UserToken userToken = getCurrentUserToken();
        return engineService.getTasks(userToken).stream()
                .filter(task -> task.getProcessId().equals(taskInfo.getProcessId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No next Task found for ProcessId " + taskInfo.getProcessId()));
    }

    public AutocompleteResponse getAutocompleteResponse(TaskInfo taskInfo, ObjectRequest objectRequest, String queryString) throws TaskOutOfDateException, TaskNotFoundException, UserNotFoundException {
        return engineService.getAutocompleteResponse(getCurrentUserToken(), taskInfo, objectRequest, queryString);
    }

}
