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
package io.github.causewaystuff.companion.codegen.domgen;

import java.util.List;

import io.github.causewaystuff.companion.codegen.model.Schema;
import io.micronaut.sourcegen.javapoet.ClassName;

import org.apache.causeway.commons.internal.base._Strings;

import lombok.experimental.UtilityClass;

@UtilityClass
class _Foreign {

    ClassName foreignClassName(final Schema.EntityField field, final Schema.EntityField foreignField, final DomainGenerator.Config config) {
        var foreignEntity = foreignField.parentEntity();
        var foreignEntityClass = field.hasElementType()
                ? ClassName.get(config.fullPackageName(field.elementTypeNamespace()), field.elementTypeSimpleName())
                : ClassName.get(foreignEntity.packageNameResolved(), foreignEntity.name());
        return foreignEntityClass;
    }

    private final static List<String> knownPropertyNameSuffixes = List.of(
            "Code",
            "LookupKey");
    String resolvedFieldName(final Schema.EntityField field) {
        var mixedInPropertyName = knownPropertyNameSuffixes.stream()
                .filter(field.name()::endsWith)
                .findFirst()
                .map(suffix->_Strings.substring(field.name(), 0, -suffix.length()))
                .orElseGet(()->field.name() + "Obj");
        return mixedInPropertyName;
    }

}
