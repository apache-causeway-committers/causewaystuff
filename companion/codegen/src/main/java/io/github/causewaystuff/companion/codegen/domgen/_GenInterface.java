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

import javax.lang.model.element.Modifier;

import io.github.causewaystuff.companion.codegen.domgen.DomainGenerator.QualifiedType;
import io.github.causewaystuff.companion.codegen.model.Schema.Entity;
import io.github.causewaystuff.tooling.javapoet.ClassName;
import io.github.causewaystuff.tooling.javapoet.TypeSpec;

import lombok.val;
import lombok.experimental.UtilityClass;

@UtilityClass
class _GenInterface {

    public QualifiedType qualifiedType(
            final DomainGenerator.Config config,
            final Entity superTypeHolder) {

        val packageName = config.fullPackageName(superTypeHolder.superTypeNamespace());
        val superTypeName = ClassName.get(packageName, superTypeHolder.superTypeSimpleName());

        return new QualifiedType(
                packageName,
                TypeSpec.interfaceBuilder(superTypeName)
                    .addAnnotation(_Annotations.generated(_GenInterface.class))
                    .addModifiers(Modifier.PUBLIC).build());
    }

}
