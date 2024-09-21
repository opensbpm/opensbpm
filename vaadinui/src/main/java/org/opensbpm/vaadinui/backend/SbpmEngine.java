package org.opensbpm.vaadinui.backend;

import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import org.opensbpm.engine.api.EngineService.ObjectRequest;
import org.opensbpm.engine.api.ModelNotFoundException;
import org.opensbpm.engine.api.SearchFilter.SearchFilterBuilder;
import org.opensbpm.engine.api.UserNotFoundException;
import org.opensbpm.engine.api.instance.AutocompleteResponse;
import org.opensbpm.engine.api.instance.TaskInfo;
import org.opensbpm.engine.api.instance.TaskNotFoundException;
import org.opensbpm.engine.api.instance.TaskOutOfDateException;
import org.opensbpm.engine.api.instance.TaskRequest;
import org.opensbpm.engine.api.instance.UserToken;
import org.opensbpm.engine.api.model.ProcessModelInfo;
import org.opensbpm.engine.client.EngineServiceClient;
import org.opensbpm.engine.server.api.EngineResource.ProcessModelResource;
import org.opensbpm.engine.server.api.EngineResource.ProcessInstanceResource;
import org.opensbpm.engine.server.api.EngineResource.TaskResource;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.opensbpm.engine.api.instance.Task;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

@SpringComponent
@VaadinSessionScope
public class SbpmEngine {

    private EngineServiceClient engineServiceClient;
    private UserToken userToken;

    private EngineServiceClient getEngineServiceClient() {
        if (engineServiceClient == null) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            OidcIdToken idToken = ((DefaultOidcUser) authentication.getPrincipal()).getIdToken();
            engineServiceClient = new EngineServiceClient("https://opensbpm.local/api/services", () -> idToken.getTokenValue());
        }
        return engineServiceClient;
    }

    private UserToken getUserToken() {
        if (userToken == null){
            userToken = getEngineServiceClient().getUserResource().info();
        }
        return userToken;
    }

    private ProcessModelResource getProcessModelResource() {
        return getEngineServiceClient().getEngineResource().getProcessModelResource(getUserToken().getId());
    }

    private ProcessInstanceResource getProcessInstanceResource() {
        return getEngineServiceClient().getEngineResource().getProcessInstanceResource(getUserToken().getId());
    }
    private TaskResource getTaskResource() {
        return getEngineServiceClient().getEngineResource().getTaskResource(getUserToken().getId());
    }

    public ProcessModelInfo uploadModel(InputStream inputStream){
        return getEngineServiceClient().getProcessModelResource().create(inputStream);
    }

    public List<ProcessModelInfo> getProcessModels(){
        //TODO findAllByStates(EnumSet.of(ProcessModelState.ACTIVE)
        return getEngineServiceClient().getProcessModelResource().search(
                new SearchFilterBuilder()
                        .build()
        ).getProcessModelInfos();
    }

    public List<ProcessModelInfo> findStartableProcessModels() throws UserNotFoundException {
        return getProcessModelResource().index().getProcessModelInfos();
    }

    public TaskInfo startProcess(ProcessModelInfo processModel) throws UserNotFoundException, ModelNotFoundException {
        return getProcessModelResource().start(processModel.getId());
    }

    public Boolean executeTask(TaskRequest taskRequest) throws UserNotFoundException, TaskNotFoundException, TaskOutOfDateException {
        getTaskResource().submit(taskRequest.getId(), taskRequest);
        return true;
    }

    public Stream<TaskInfo> getTasks(String filter) throws UserNotFoundException {
        return getAllTasks().filter(taskInfo -> taskInfo.getProcessName().contains(filter));
    }

    public Task getTask(Long taskId) throws TaskOutOfDateException, TaskNotFoundException, UserNotFoundException {
        TaskInfo taskInfo = getAllTasks().filter(task -> Objects.equals(task.getId(), taskId))
                .findFirst()
                .orElseThrow(() -> new TaskOutOfDateException(String.valueOf(taskId)));
        return new Task(taskInfo, getTaskResource().retrieve(taskInfo.getId()));
    }

    private Stream<TaskInfo> getAllTasks() throws UserNotFoundException {
        return getTaskResource().index().getTaskInfos().stream();
    }

    public TaskInfo getNextTask(TaskInfo taskInfo) throws UserNotFoundException {
        return getTaskResource().index().getTaskInfos().stream()
                .filter(task -> task.getProcessId().equals(taskInfo.getProcessId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No next Task found for ProcessId " + taskInfo.getProcessId()));
    }

    public AutocompleteResponse getAutocompleteResponse(TaskInfo taskInfo, ObjectRequest objectRequest, String queryString) throws TaskOutOfDateException, TaskNotFoundException, UserNotFoundException {
        //return engineService.getAutocompleteResponse(getCurrentUserToken(), taskInfo, objectRequest, queryString);
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
