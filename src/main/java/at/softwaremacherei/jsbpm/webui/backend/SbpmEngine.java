package at.softwaremacherei.jsbpm.webui.backend;

import at.softwaremacherei.jsbpm.engine.api.EngineService;
import at.softwaremacherei.jsbpm.engine.api.EngineService.ObjectRequest;
import at.softwaremacherei.jsbpm.engine.api.ModelNotFoundException;
import at.softwaremacherei.jsbpm.engine.api.UserNotFoundException;
import at.softwaremacherei.jsbpm.engine.api.UserTokenService;
import at.softwaremacherei.jsbpm.engine.api.instance.AutocompleteResponse;
import at.softwaremacherei.jsbpm.engine.api.instance.Task;
import at.softwaremacherei.jsbpm.engine.api.instance.TaskInfo;
import at.softwaremacherei.jsbpm.engine.api.instance.TaskNotFoundException;
import at.softwaremacherei.jsbpm.engine.api.instance.TaskOutOfDateException;
import at.softwaremacherei.jsbpm.engine.api.instance.TaskRequest;
import at.softwaremacherei.jsbpm.engine.api.instance.UserToken;
import at.softwaremacherei.jsbpm.engine.api.model.ProcessModelInfo;
import at.softwaremacherei.jsbpm.springauthentication.SpringAuthentication;
import java.util.Collection;
import java.util.stream.Stream;
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
        return engineService.startProcess(getCurrentUserToken(), processModel);
    }

    public Boolean executeTask(TaskRequest taskRequest) throws UserNotFoundException, TaskNotFoundException, TaskOutOfDateException {
        return engineService.executeTask(getCurrentUserToken(), taskRequest);
    }

    public Stream<TaskInfo> getTasks(String filter) throws UserNotFoundException {
        UserToken userToken = getCurrentUserToken();
        return engineService.getTasks(userToken).stream()
                .filter(taskInfo -> taskInfo.getProcessName().contains(filter));
    }

    public Task getTask(TaskInfo taskInfo) throws UserNotFoundException, TaskNotFoundException, TaskOutOfDateException {
        return new Task(taskInfo, engineService.getTaskResponse(getCurrentUserToken(), taskInfo));
    }

    public TaskInfo getNextTask(TaskInfo taskInfo) throws UserNotFoundException {
        UserToken userToken = getCurrentUserToken();
        return engineService.getTasks(userToken).stream()
                .filter(task -> task.getProcessId().equals(taskInfo.getProcessId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No next Task found for ProcessId " + taskInfo.getProcessId()));
    }

    public AutocompleteResponse getAutocompleteResponse(TaskInfo taskInfo, ObjectRequest objectRequest, String queryString) throws UserNotFoundException, TaskNotFoundException, TaskOutOfDateException {
        return engineService.getAutocompleteResponse(getCurrentUserToken(), taskInfo, objectRequest, queryString);
    }

}
