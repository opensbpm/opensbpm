package org.opensbpm.vaadinui.backend;

import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import org.opensbpm.engine.api.EngineService.ObjectRequest;
import org.opensbpm.engine.api.ModelNotFoundException;
import org.opensbpm.engine.api.SearchFilter.SearchFilterBuilder;
import org.opensbpm.engine.api.UserNotFoundException;
import org.opensbpm.engine.api.instance.*;
import org.opensbpm.engine.api.model.ProcessModelInfo;
import org.opensbpm.engine.client.EngineServiceClient;
import org.opensbpm.engine.server.api.EngineResource.ProcessModelResource;
import org.opensbpm.engine.server.api.EngineResource.ProcessInstanceResource;
import org.opensbpm.engine.server.api.EngineResource.TaskResource;

import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

@SpringComponent
@VaadinSessionScope
public class SbpmEngine {

    private EngineServiceClient engineServiceClient;

    private EngineServiceClient getEngineServiceClient() {
        if (engineServiceClient == null) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            OidcIdToken idToken = ((DefaultOidcUser) authentication.getPrincipal()).getIdToken();
            engineServiceClient = new EngineServiceClient("https://cloud-dev.opensbpm.org") {
                @Override
                protected String getAuthenticationToken() {
                    return idToken.getTokenValue();
                }
            };
        }
        return engineServiceClient;
    }

    public ProcessModelInfo uploadModel(InputStream inputStream) {
        return getEngineServiceClient().onProcessModelResource(resource -> resource.create(inputStream));
    }

    public List<ProcessModelInfo> getProcessModels() {
        //TODO findAllByStates(EnumSet.of(ProcessModelState.ACTIVE)
        return getEngineServiceClient().onProcessModelResource(resource -> resource.search(
                new SearchFilterBuilder()
                        .build()
        ).getProcessModelInfos());
    }

    public List<ProcessModelInfo> findStartableProcessModels() throws UserNotFoundException {
        return getEngineServiceClient().onEngineModelResource(resource -> resource.index().getProcessModelInfos());
    }

    public TaskInfo startProcess(ProcessModelInfo processModel) throws UserNotFoundException, ModelNotFoundException {
        return getEngineServiceClient().onEngineModelResource(resource -> resource.start(processModel.getId()));
    }

    public Boolean executeTask(TaskRequest taskRequest) throws UserNotFoundException, TaskNotFoundException, TaskOutOfDateException {
        getEngineServiceClient().onEngineTaskResource(resource -> {
            resource.submit(taskRequest.getId(), taskRequest);
            return null;
        });
        return true;
    }

    public Stream<TaskInfo> getTasks(String filter) throws UserNotFoundException {
        return getAllTasks().filter(taskInfo -> taskInfo.getProcessName().contains(filter));
    }

    public Task getTask(Long taskId) throws TaskOutOfDateException, TaskNotFoundException, UserNotFoundException {
        TaskInfo taskInfo = getAllTasks()
                .filter(task -> Objects.equals(task.getId(), taskId))
                .findFirst()
                .orElseThrow(() -> new TaskOutOfDateException(String.valueOf(taskId)));
        TaskResponse taskResponse = getEngineServiceClient().onEngineTaskResource(resource-> resource.retrieve(taskInfo.getId()));
        return new Task(taskInfo, taskResponse);
    }

    private Stream<TaskInfo> getAllTasks() throws UserNotFoundException {
        return getEngineServiceClient().onEngineTaskResource(resource -> resource.index(0,50).getTaskInfos()).stream();
    }

    public TaskInfo getNextTask(TaskInfo taskInfo) throws UserNotFoundException {
        return getAllTasks()
                .filter(task -> task.getProcessId().equals(taskInfo.getProcessId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No next Task found for ProcessId " + taskInfo.getProcessId()));
    }

    public AutocompleteResponse getAutocompleteResponse(TaskInfo taskInfo, ObjectRequest objectRequest, String queryString) throws TaskOutOfDateException, TaskNotFoundException, UserNotFoundException {
        //return engineService.getAutocompleteResponse(getCurrentUserToken(), taskInfo, objectRequest, queryString);
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
