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
package org.causewaystuff.tooling.codegen.domgen;

import javax.lang.model.element.Modifier;

import org.causewaystuff.tooling.codegen.domgen.DomainGenerator.QualifiedType;
import org.causewaystuff.tooling.codegen.model.OrmModel.Entity;

import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.TypeSpec;

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
                .addModifiers(Modifier.PUBLIC).build());
    }

}
