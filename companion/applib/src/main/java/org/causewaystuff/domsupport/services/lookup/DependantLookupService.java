/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.causewaystuff.domsupport.services.lookup;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.causeway.commons.collections.Can;

public interface DependantLookupService {

    /**
     * @param <D> dependent type
     * @param <M> mixin type
     * @param <L> local type
     * @param dependantType - type to lookup/enumerate
     * @param dependantAssociationMixinClass mixin that contributes an association, that potentially matches
     * @param dependantAssociationMixinGetter
     * @param localEntity
     */
    <D, M, L> List<D> findDependants(
            Class<D> dependantType,
            Class<M> dependantAssociationMixinClass,
            Function<M, L> dependantAssociationMixinGetter,
            L localEntity);


    Can<?> findAllDependants(Object entity);

    default String findAllDependantsAsMultilineString(final Object entity) {
        var result = findAllDependants(entity)
            .stream()
            .map(Object::toString)
            .collect(Collectors.joining("\n"));
        return result.isEmpty()
                ? "no dependants found"
                : result;
    }

}
