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
package org.opensbpm.engine.service.services;

import org.opensbpm.engine.api.TaskProviderService;
import org.opensbpm.engine.api.taskprovider.TaskProviderInfo;
import org.opensbpm.engine.api.taskprovider.TaskProviderInfo.ProviderResource;
import java.util.ArrayList;

import org.opensbpm.engine.server.api.TaskProviderResource;
import org.opensbpm.engine.server.api.dto.taskprovider.TaskProviderInfos;
import org.opensbpm.engine.server.api.dto.taskprovider.TaskProviderInfos.ProviderResources;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TaskProviderResourceService implements TaskProviderResource {

    @Autowired
    private TaskProviderService taskProviderService;

    @Override
    public TaskProviderInfos getProviders() {
        return new TaskProviderInfos(taskProviderService.getProviders());
    }

    @Override
    public ProviderResources getResources(String providerName) {
        return new ProviderResources(new ArrayList<>(taskProviderService.getResources(new TaskProviderInfo(providerName))));
    }

    @Override
    public void addResource(String providerName, ProviderResource providerResource) {
        taskProviderService.addResource(new TaskProviderInfo(providerName), providerResource);
    }
}
