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

import io.github.causewaystuff.companion.codegen.model.OrmModel;
import io.github.causewaystuff.tooling.javapoet.ClassName;
import io.github.causewaystuff.tooling.javapoet.FieldSpec;
import io.github.causewaystuff.tooling.javapoet.MethodSpec;
import io.github.causewaystuff.tooling.javapoet.TypeSpec;

import org.apache.causeway.applib.annotation.Nature;

import lombok.experimental.UtilityClass;

@UtilityClass
class _GenEntity_Unresolvable {

    TypeSpec generate(
            final DomainGenerator.Config config,
            final OrmModel.Entity entityModel) {
        var unresolvableClass = TypeSpec.classBuilder("Unresolvable")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .superclass(ClassName.get("", entityModel.name()))
                .addJavadoc("Placeholder @{link ViewModel} for @{link $1L} "
                        + "in case of an unresolvable secondary key.", entityModel.name())
                .addSuperinterface(ClassName.get(org.apache.causeway.applib.ViewModel.class))
                .addAnnotation(_Annotations.generated(_GenEntity_Unresolvable.class))
                .addAnnotation(_Annotations.domainObject(Nature.VIEW_MODEL))
                .addAnnotation(_Annotations.domainObjectLayout(
                        String.format("Unresolvable %s", entityModel.name()),
                        "skull .unresolvable-color"))
                .addAnnotation(_Annotations.named(config.fullLogicalName(entityModel.namespace())
                        + "." + entityModel.name() + ".Unresolvable"))
                .addAnnotation(_Annotations.requiredArgsConstructor())
                .addField(FieldSpec.builder(ClassName.get(String.class), "viewModelMemento", Modifier.PRIVATE, Modifier.FINAL)
                        .addAnnotation(_Annotations.getterWithOverride())
                        .addAnnotation(_Annotations.accessorsFluent())
                        .build())
                .addMethod(MethodSpec.methodBuilder("title")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(_Annotations.override())
                        .returns(ClassName.get(String.class))
                        .addCode("return viewModelMemento;")
                        .build())
                .build();
        return unresolvableClass;
    }

}
