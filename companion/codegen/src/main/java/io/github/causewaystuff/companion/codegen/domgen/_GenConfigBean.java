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

import io.github.causewaystuff.companion.codegen.domgen.DomainGenerator.DomainModel;
import io.github.causewaystuff.companion.codegen.domgen.DomainGenerator.QualifiedType;
import io.micronaut.sourcegen.javapoet.ClassName;
import io.micronaut.sourcegen.javapoet.TypeSpec;

import org.jspecify.annotations.NonNull;

import lombok.experimental.UtilityClass;

@UtilityClass
class _GenConfigBean {

    public QualifiedType qualifiedType(
            final DomainGenerator.@NonNull Config config,
            final @NonNull DomainModel domainModel) {

        var packageName = config.fullPackageName(config.entitiesModulePackageName());

        final ClassName nameOfClassToGenerate =
                ClassName.get(packageName, "ModuleConfig");
                //ClassName.get(packageName, config.entitiesModuleClassSimpleName() + "Config");

        var typeModelBuilder = TypeSpec.classBuilder(nameOfClassToGenerate)
                .addAnnotation(_Annotations.generated(_GenConfigBean.class))
                .addAnnotation(_Annotations.spring.configuration())
                .addModifiers(Modifier.PUBLIC)
                ;
        return new QualifiedType(
                packageName,
                typeModelBuilder.build());
    }

}
