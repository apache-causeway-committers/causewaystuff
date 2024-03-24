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
package org.causewaystuff.companion.codegen.domgen;

import javax.lang.model.element.Modifier;

import org.causewaystuff.companion.codegen.domgen.DomainGenerator.DomainModel;
import org.causewaystuff.companion.codegen.domgen.DomainGenerator.QualifiedType;
import org.causewaystuff.tooling.javapoet.ClassName;
import org.causewaystuff.tooling.javapoet.TypeSpec;

import lombok.NonNull;
import lombok.val;
import lombok.experimental.UtilityClass;

@UtilityClass
class _GenConfigBean {

    public QualifiedType qualifiedType(
            final @NonNull DomainGenerator.Config config,
            final @NonNull DomainModel domainModel) {

        val packageName = config.fullPackageName(config.entitiesModulePackageName());

        final ClassName nameOfClassToGenerate =
                ClassName.get(packageName, "ModuleConfig");
                //ClassName.get(packageName, config.entitiesModuleClassSimpleName() + "Config");

        val typeModelBuilder = TypeSpec.classBuilder(nameOfClassToGenerate)
                .addAnnotation(_Annotations.generated(_GenConfigBean.class))
                .addAnnotation(_Annotations.configuration())
                .addModifiers(Modifier.PUBLIC)
                ;
        return new QualifiedType(
                packageName,
                typeModelBuilder.build());
    }
    

}
