/** *****************************************************************************
 * Copyright (C) 2022 Stefan Sedelmaier
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
 *****************************************************************************
 */
package org.opensbpm.engine.service.services;

import org.opensbpm.engine.api.instance.ProcessInfo;
import org.opensbpm.engine.api.model.ProcessModelInfo;
import org.opensbpm.engine.api.model.ProcessModelState;
import org.opensbpm.engine.api.SearchFilter;
import org.opensbpm.engine.api.ModelService;
import org.opensbpm.engine.api.model.definition.ProcessDefinition;
import org.opensbpm.engine.api.ModelNotFoundException;
import org.opensbpm.engine.api.ModelService.ModelRequest;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;
import jakarta.xml.bind.JAXBException;
import org.opensbpm.engine.server.api.ProcessModelResource;
import org.opensbpm.engine.server.api.dto.model.ProcessModels;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.opensbpm.engine.xmlmodel.ProcessDefinitionConverter;
import org.opensbpm.engine.xmlmodel.ProcessModel;

@Component
public class ProcessModelResourceService implements ProcessModelResource {

    private static final Logger LOGGER = Logger.getLogger(ProcessModelResourceService.class.getName());

    @Autowired
    private ModelService modelService;

    @Override
    public ProcessModels search(SearchFilter searchFilter) {
//        SearchSpecificationBuilder<ProcessModel> searchSpecificationBuilder = new SearchSpecificationBuilder<ProcessModel>(searchFilter) {
//            @Override
//            protected Specification<ProcessModel> createSpecification(Criteria criteria) {
//                if ("state".equals(criteria.getKey())) {
//                    return ProcessModelSpecifications.withStates(
//                            Collections.singleton(ProcessModelState.valueOf(criteria.getValue().toString()))
//                    );
//                }
//                return super.createSpecification(criteria);
//            }
//        };
//        return ProcessModels.of(modelService.findAllInfos(searchSpecificationBuilder.build()));
        return ProcessModels.of(modelService.findAllByStates(EnumSet.allOf(ProcessModelState.class)));
    }

    @Override
    public ProcessModelInfo create(InputStream processStream) {
        try {
            return modelService.save(new ProcessModel().unmarshal(processStream));
        } catch (JAXBException ex) {
            throw new WebApplicationException(ex.getMessage(), ex, Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ProcessModelInfo retrieve(Long modelId) {
        try {
            ProcessDefinition definition = modelService.retrieveDefinition(ModelRequest.of(modelId));
            return new ProcessModelInfo(
                    -1l,
                    definition.getName(),
                    String.valueOf(definition.getVersion()),
                    definition.getDescription(),
                    definition.getState(),
                    LocalDateTime.now(),
                    Collections.emptyList()
            );
        } catch (ModelNotFoundException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            throw new NotFoundException(ex.getMessage(), ex);
        }
    }

    @Override
    public void updateState(Long modelId, ProcessModelState state) {
        try {
            modelService.updateState(ModelRequest.of(modelId), state);
        } catch (ModelNotFoundException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            throw new NotFoundException(ex.getMessage(), ex);
        }
    }

    @Override
    public void delete(Long modelId) {
        try {
            modelService.delete(ModelRequest.of(modelId));
        } catch (ModelNotFoundException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            throw new NotFoundException(ex.getMessage(), ex);
        }
    }

}
