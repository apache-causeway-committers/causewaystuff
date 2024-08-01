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

import javax.lang.model.element.Modifier;

import io.github.causewaystuff.companion.applib.services.lookup.ISecondaryKey;
import io.github.causewaystuff.companion.codegen.model.Schema;
import io.github.causewaystuff.tooling.javapoet.ClassName;
import io.github.causewaystuff.tooling.javapoet.MethodSpec;
import io.github.causewaystuff.tooling.javapoet.ParameterSpec;
import io.github.causewaystuff.tooling.javapoet.ParameterizedTypeName;
import io.github.causewaystuff.tooling.javapoet.TypeSpec;

import lombok.experimental.UtilityClass;

@UtilityClass
class _GenEntity_SecondaryKey {

    TypeSpec generate(
            final DomainGenerator.Config config,
            final Schema.Entity entityModel) {

        var secondaryKeyRecord = TypeSpec.recordBuilder("SecondaryKey")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .addSuperinterface(ParameterizedTypeName.get(
                        ClassName.get(ISecondaryKey.class),
                        ClassName.get("", entityModel.name())))
                .addJavadoc("SecondaryKey for @{link $1L}", entityModel.name())
                .addAnnotation(_Annotations.generated(_GenEntity_SecondaryKey.class))
                .addRecordComponents(asSecondaryKeyParams(entityModel.secondaryKeyFields()))
                .addMethod(MethodSpec.methodBuilder("correspondingClass")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(_Annotations.override())
                        .returns(ParameterizedTypeName.get(
                                ClassName.get(Class.class),
                                ClassName.get("", entityModel.name())))
                        .addCode("return $1L.class;", entityModel.name())
                        .build())
                .addMethod(MethodSpec.methodBuilder("unresolvable")
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .addAnnotation(_Annotations.override())
                        .returns(ClassName.get("", "Unresolvable"))
                        .addCode("""
                                return new Unresolvable(String.format("UNRESOLVABLE %s%s",
                                    correspondingClass().getSimpleName(),
                                    this.toString().substring(12)));""")
                        .build())
                .build();

        return secondaryKeyRecord;
    }

    // -- HELPER

    private Iterable<ParameterSpec> asSecondaryKeyParams(
            final List<Schema.EntityField> fields,
            final Modifier ... modifiers) {
        return fields.stream()
                .map(field->
                    ParameterSpec.builder(field.asJavaType(), field.name(), modifiers)
                    .addJavadoc(field.formatDescription("\n"))
                    .build())
                .toList();
    }

}
