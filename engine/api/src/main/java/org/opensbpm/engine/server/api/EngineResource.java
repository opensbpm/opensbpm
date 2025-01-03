/*******************************************************************************
 * Copyright (C) 2024 Stefan Sedelmaier
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.opensbpm.engine.server.api;

import io.swagger.v3.oas.annotations.Operation;
import org.opensbpm.engine.api.instance.ProcessInfo;
import org.opensbpm.engine.api.instance.TaskInfo;
import org.opensbpm.engine.api.instance.TaskRequest;
import org.opensbpm.engine.api.instance.TaskResponse;
import org.opensbpm.engine.api.model.ProcessModelInfo;
import org.opensbpm.engine.server.api.dto.instance.Processes;
import org.opensbpm.engine.server.api.dto.instance.Tasks;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.opensbpm.engine.server.api.dto.model.ProcessModels;

//@Api(value = "Engine", authorizations = {
//    @Authorization(value = "basicAuth")})
@PermitAll
@Path("engine")
@Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
public interface EngineResource {

    @Path("/{userId}/models")
    ProcessModelResource getProcessModelResource(@PathParam(value = "userId") Long userId);

    @Path("/{userId}/instances")
    ProcessInstanceResource getProcessInstanceResource(@PathParam(value = "userId") Long userId);

    @Path("/{userId}/tasks")
    TaskResource getTaskResource(@PathParam(value = "userId") Long userId);

    public interface ProcessModelResource {

        @Operation(summary = "Retrieve all executable process models",
                description = "Retrieve all process models the given user can start")
        @GET
        ProcessModels index();

        @GET
        @Path(value = "/{modelId}")
        ProcessModelInfo retrieve(
                @PathParam(value = "modelId") Long modelId);

        @POST
        @Path(value = "/{modelId}/start")
        TaskInfo start(@PathParam(value = "modelId") Long modelId);
    }

    public interface ProcessInstanceResource {

        @GET
        Processes index();

        @GET
        @Path(value = "/{instanceId}")
        ProcessInfo retrieve(@PathParam(value = "instanceId") Long instanceId);
    }

    public interface TaskResource {

        @GET
        Tasks index();

        @GET
        @Path(value = "/{taskId}")
        TaskResponse retrieve(@PathParam(value = "taskId") Long taskId);

        //TODO create useful response-object
        @POST
        @Path(value = "/{taskId}")
        void submit(@PathParam(value = "taskId") Long taskId, TaskRequest taskRequest);
    }

}
