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

import jakarta.annotation.security.PermitAll;
import org.opensbpm.engine.api.taskprovider.TaskProviderInfo.ProviderResource;
import org.opensbpm.engine.server.api.dto.taskprovider.TaskProviderInfos;
import org.opensbpm.engine.server.api.dto.taskprovider.TaskProviderInfos.ProviderResources;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

//@Api(value = "TaskProvider")
@PermitAll
@Path(value = "engine/providers")
@Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
public interface TaskProviderResource {

    @GET
    TaskProviderInfos getProviders();

    @GET
    @Path("/{providerName}")
    ProviderResources getResources(@PathParam("providerName") String providerName);

    @POST
    @Path("/{providerName}")
    void addResource(@PathParam("providerName") String providerName, ProviderResource providerResource);

}
