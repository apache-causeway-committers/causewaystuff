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

import io.github.causewaystuff.companion.codegen.model.Schema;

import org.apache.causeway.commons.internal.base._Strings;

import lombok.experimental.UtilityClass;

@UtilityClass
class _Mixins {

    String customMixinClassName(final Schema.Entity entityModel, final String memberName) {
        return entityModel.name() + "_" + memberName;
    }

    String propertyMixinClassName(final Schema.EntityField field) {
        return customMixinClassName(field.parentEntity(),
                _Foreign.resolvedFieldName(field));
    }

    String collectionMixinClassName(
            final Schema.Entity localEntity,
            final Schema.EntityField dependantField) {
        var dependantEntity = dependantField.parentEntity();
        return localEntity.name()
                + "_dependent"
                + _Strings.capitalize(dependantEntity.name())
                + "MappedBy"
                + _Strings.capitalize(_Foreign.resolvedFieldName(dependantField));
    }

}
