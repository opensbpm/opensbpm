package org.opensbpm;

import org.opensbpm.authentication.SpringAuthentication;
import org.opensbpm.engine.api.EngineService;
import org.opensbpm.engine.api.EngineService.ObjectRequest;
import org.opensbpm.engine.api.ModelNotFoundException;
import org.opensbpm.engine.api.ModelService.ModelRequest;
import org.opensbpm.engine.api.UserNotFoundException;
import org.opensbpm.engine.api.UserTokenService;
import org.opensbpm.engine.api.UserTokenService.TokenRequest;
import org.opensbpm.engine.api.instance.*;
import org.opensbpm.engine.api.model.ProcessModelInfo;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Stream;

@RestController
public class SbpmEngine {

    private final EngineService engineService;
    private final UserTokenService userTokenService;

    public SbpmEngine(EngineService engineService, UserTokenService userTokenService) {
        this.engineService = engineService;
        this.userTokenService = userTokenService;
    }

    private UserToken getCurrentUserToken() throws UserNotFoundException {
        TokenRequest tokenRequest = SpringAuthentication.of(SecurityContextHolder.getContext().getAuthentication());
        return userTokenService.retrieveToken(tokenRequest);
    }

    @GetMapping("/models")
    public Collection<ProcessModelInfo> findStartableProcessModels() throws UserNotFoundException {
        return engineService.findStartableProcessModels(getCurrentUserToken());
    }

    @PostMapping("/models")
    public TaskInfo startProcess(ProcessModelInfo processModel) throws UserNotFoundException, ModelNotFoundException {
        return engineService.startProcess(getCurrentUserToken(), ModelRequest.of(processModel));
    }

    @PostMapping("/tasks")
    public Boolean executeTask(TaskRequest taskRequest) throws UserNotFoundException, TaskNotFoundException, TaskOutOfDateException {
        return engineService.executeTask(getCurrentUserToken(), taskRequest);
    }

    @GetMapping("/tasks")
    public Stream<TaskInfo> getTasks(String filter) throws UserNotFoundException {
        return getAllTasks()
                .filter(taskInfo -> taskInfo.getProcessName().contains(filter));
    }

    @GetMapping("/tasks/{taskId}")
    public Task getTask(@PathVariable Long taskId) throws TaskOutOfDateException, TaskNotFoundException, UserNotFoundException {
        TaskInfo taskInfo = getAllTasks()
                .filter(task -> Objects.equals(task.getId(), taskId))
                .findFirst()
                .orElseThrow(() -> new TaskOutOfDateException(String.valueOf(taskId)));
        return new Task(taskInfo, engineService.getTaskResponse(getCurrentUserToken(), taskInfo));
    }

    private Stream<TaskInfo> getAllTasks() throws UserNotFoundException {
        return engineService.getTasks(getCurrentUserToken()).stream();
    }

    //@GetMapping("/tasks/{taskId}")
    public TaskInfo getNextTask(TaskInfo taskInfo) throws UserNotFoundException {
        return engineService.getTasks(getCurrentUserToken()).stream()
                .filter(task -> task.getProcessId().equals(taskInfo.getProcessId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No next Task found for ProcessId " + taskInfo.getProcessId()));
    }

    public AutocompleteResponse getAutocompleteResponse(TaskInfo taskInfo, ObjectRequest objectRequest, String queryString) throws TaskOutOfDateException, TaskNotFoundException, UserNotFoundException {
        return engineService.getAutocompleteResponse(getCurrentUserToken(), taskInfo, objectRequest, queryString);
    }

}
