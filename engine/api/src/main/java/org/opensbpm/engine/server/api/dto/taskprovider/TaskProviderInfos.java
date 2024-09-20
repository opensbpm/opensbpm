/*******************************************************************************
 * Copyright (C) 2020 Stefan Sedelmaier
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
package org.opensbpm.engine.server.api.dto.taskprovider;

import org.opensbpm.engine.api.taskprovider.TaskProviderInfo;
import org.opensbpm.engine.api.taskprovider.TaskProviderInfo.ProviderResource;
import org.opensbpm.engine.utils.StreamUtils;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@XmlRootElement
@XmlAccessorType(value = XmlAccessType.FIELD)
public final class TaskProviderInfos {

    private List<TaskProviderInfo> taskProviders;

    public TaskProviderInfos() {
    }

    public TaskProviderInfos(Collection<TaskProviderInfo> taskProviders) {
        this.taskProviders = new ArrayList<>(taskProviders);
    }

    public List<TaskProviderInfo> getTaskProviders() {
        return StreamUtils.emptyOrUnmodifiableList(taskProviders);
    }

    @XmlRootElement
    @XmlAccessorType(value = XmlAccessType.FIELD)
    public static class ProviderResources {

        private List<ProviderResource> providerResources;

        public ProviderResources() {
        }

        public ProviderResources(List<ProviderResource> providerResources) {
            this.providerResources = Collections.unmodifiableList(providerResources);
        }

        public List<ProviderResource> getProviderResources() {
            return StreamUtils.emptyOrUnmodifiableList(providerResources);
        }

    }

}
