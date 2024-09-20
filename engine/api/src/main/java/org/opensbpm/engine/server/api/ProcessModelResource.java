/** *****************************************************************************
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
 * ****************************************************************************
 */
package org.opensbpm.engine.server.api;

import jakarta.annotation.security.PermitAll;
import org.opensbpm.engine.api.model.ProcessModelInfo;
import org.opensbpm.engine.api.model.ProcessModelState;
import org.opensbpm.engine.server.api.dto.model.ProcessModels;
import org.opensbpm.engine.api.SearchFilter;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.io.InputStream;
import jakarta.xml.bind.annotation.XmlRootElement;

//Api(value = "Process Model")
@PermitAll
@Path(value = "engine/models")
@Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
public interface ProcessModelResource {

    @GET
    ProcessModels search(@QueryParam(value = "search") SearchFilter searchFilter);

    @POST
    //@Consumes(MediaType.APPLICATION_OCTET_STREAM)
    ProcessModelInfo create(InputStream processStream);

    @GET
    @Path("/{modelId}")
    ProcessModelInfo retrieve(@PathParam("modelId") Long modelId);

    @PUT
    @Path(value = "/{modelId}/state")
    void updateState(@PathParam("modelId") Long modelId, ProcessModelState state);

    @DELETE
    @Path("/{modelId}")
    void delete(@PathParam("modelId") Long id);

}
