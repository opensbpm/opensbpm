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
package org.opensbpm.engine.e2e.user;

import org.opensbpm.engine.api.instance.NextState;
import org.opensbpm.engine.api.instance.Task;
import org.opensbpm.engine.api.model.Binary;
import org.opensbpm.engine.api.model.ProcessModelInfo;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.text.RandomStringGenerator;
import org.opensbpm.engine.api.instance.SimpleAttributeSchema;

public class Utils {

    public static String randomString(String prefix) {
        RandomStringGenerator generator = new RandomStringGenerator.Builder()
                .withinRange('a', 'z')
                .build();
        return prefix + "-" + generator.generate(20);
    }

    public static Serializable createRandomValue(SimpleAttributeSchema attributeSchema) {
        Serializable value;
        switch (attributeSchema.getFieldType()) {
            case STRING:
                value = Utils.randomString("");
                break;
            case NUMBER:
                value = RandomUtils.nextInt();
                break;
            case DECIMAL:
                value = RandomUtils.nextDouble();
                break;
            case DATE:
                value = LocalDate.now();
                break;
            case TIME:
                value = LocalTime.now();
                break;
            case BOOLEAN:
                value = RandomUtils.nextBoolean();
                break;
            case BINARY:
                value = new Binary("mimeType", new byte[]{});
                break;
//            case REFERENCE:
//                //this doesn't work with real "store"-plugins
//                value = ObjectReference.of("1", "ObjectReference").toMap();
//                break;
            default:
                throw new UnsupportedOperationException("FieldType " + attributeSchema.getFieldType() + " not implemented yet");
        }
        return value;
    }

    public static NextState randomState(Task task) {
        int nextStatesCount = task.getNextStates().size();
        int randomState = RandomUtils.nextInt(0, nextStatesCount);
        return task.getNextStates().get(randomState);
    }

    public static boolean isIn(List<ProcessModelInfo> processModels, ProcessModelInfo processModelInfo) {
        return processModels.stream()
                .anyMatch(model -> processModelInfo.getId().equals(model.getId()));
    }

    public static <T> String joinToString(Collection<T> collection, Function<T, String> mapper) {
        return joinToString(collection, mapper, ",");
    }

    public static <T> String joinToString(Collection<T> collection, Function<T, String> mapper, String delimiter) {
        return collection.stream()
                .map(mapper).collect(Collectors.joining(delimiter));
    }

    static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    private Utils() {
    }

}
