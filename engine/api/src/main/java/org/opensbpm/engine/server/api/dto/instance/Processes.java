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
package org.opensbpm.engine.server.api.dto.instance;

import org.opensbpm.engine.api.instance.ProcessInfo;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Collections;
import java.util.List;

import static org.opensbpm.engine.utils.StreamUtils.emptyOrUnmodifiableList;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public final class Processes {

    private List<ProcessInfo> processInfos;

    public Processes() {
        //JAXB consructor
    }

    public Processes(List<ProcessInfo> processInfos) {
        this.processInfos = Collections.unmodifiableList(processInfos);
    }

    public List<ProcessInfo> getProcessInfos() {
        return emptyOrUnmodifiableList(processInfos);
    }

}
