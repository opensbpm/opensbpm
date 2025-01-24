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
import jakarta.annotation.security.PermitAll;
import org.opensbpm.engine.server.api.dto.instance.Audits;
import org.opensbpm.engine.server.api.dto.instance.Processes;
import org.opensbpm.engine.api.SearchFilter;
import org.opensbpm.engine.api.instance.ProcessInfo;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

//@Api(value = "Process Instance")
@PermitAll
@Path("engine/instances")
public interface ProcessInstanceResource {

    @GET
    Processes search(@QueryParam(value = "search") SearchFilter searchFilter);

    @GET
    @Path(value = "/{instanceId}")
    ProcessInfo retrieve(@PathParam(value = "instanceId") Long instanceId);

    @GET
    @Path(value = "/{instanceId}/audits")
    Audits retrieveAudit(@PathParam(value = "instanceId") Long instanceId);

    @Operation(summary = "stop process",
            description = "Immediately stop process with given id")
    @POST
    @Path(value = "/{instanceId}/stop")
    ProcessInfo stop(@PathParam("instanceId") Long instanceId);

}
