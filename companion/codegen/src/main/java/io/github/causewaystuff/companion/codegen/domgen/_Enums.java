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

import org.apache.causeway.commons.internal.assertions._Assert;
import org.apache.causeway.commons.internal.base._NullSafe;
import org.apache.causeway.commons.internal.base._Strings;

import lombok.experimental.UtilityClass;

import io.github.causewaystuff.companion.applib.jpa.EnumConverter;
import io.github.causewaystuff.companion.applib.jpa.EnumWithCode;
import io.github.causewaystuff.companion.codegen.model.Schema;
import io.micronaut.sourcegen.javapoet.ArrayTypeName;
import io.micronaut.sourcegen.javapoet.ClassName;
import io.micronaut.sourcegen.javapoet.FieldSpec;
import io.micronaut.sourcegen.javapoet.MethodSpec;
import io.micronaut.sourcegen.javapoet.ParameterizedTypeName;
import io.micronaut.sourcegen.javapoet.TypeName;
import io.micronaut.sourcegen.javapoet.TypeSpec;
import io.micronaut.sourcegen.javapoet.TypeSpec.Builder;

@UtilityClass
class _Enums {

    TypeSpec enumForJpaColumn(final TypeName columnType, final List<Schema.EnumConstant> enumConsts) {
        var enumBuilder = enumBuilder(columnType.box(), enumConsts);
        var preview = enumBuilder.build();
        //add inner Converter Class (JPA only)
        return enumBuilder
            .addType(jpaConverter(ClassName.bestGuess(preview.name), columnType.box()))
            .build();
    }

    TypeSpec enumForJdoColumn(final TypeName columnType, final List<Schema.EnumConstant> enumConsts) {
        return enumBuilder(columnType.box(), enumConsts)
            .build();
    }

    // -- HELPER

    private Builder enumBuilder(final TypeName columnType, final List<Schema.EnumConstant> enumConsts) {
        _Assert.assertFalse(_NullSafe.isEmpty(enumConsts));
        var field = enumConsts.get(0).parentField();

        var builder = TypeSpec.enumBuilder(_Strings.capitalize(field.name()))
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(_Annotations.getter())
                .addAnnotation(_Annotations.accessorsFluent())
                .addAnnotation(_Annotations.requiredArgsConstructor())
                .addSuperinterface(ParameterizedTypeName.get(
                    ClassName.get(EnumWithCode.class),
                    columnType))
                .addField(FieldSpec.builder(columnType, "code", Modifier.PRIVATE, Modifier.FINAL)
                    .build())
                .addField(FieldSpec.builder(ClassName.get(String.class), "title", Modifier.PRIVATE, Modifier.FINAL)
                    .build())
                ;
        enumConsts.forEach(enumConst->{
            var description = _Strings.nonEmpty(enumConst.description());
            var arg0Template = columnType.isPrimitive()
                    || columnType.isBoxedPrimitive()
                    ? "$1L"
                    : "$1S";
            builder
                .addEnumConstant(enumConst.asJavaName(),
                        TypeSpec.anonymousClassBuilder(arg0Template + ", $2S", enumConst.matchOn(), enumConst.name())
                            .addJavadoc(description.orElse("no description"))
                            .build());

        });
        return builder;
    }

    TypeSpec jpaConverter(final TypeName enumType, final TypeName columnType) {
        return TypeSpec.classBuilder("Converter")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
            .addSuperinterface(ParameterizedTypeName.get(
                ClassName.get(EnumConverter.class),
                enumType,
                columnType))
            .addMethod(MethodSpec.methodBuilder("values")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(_Annotations.override())
                .returns(ArrayTypeName.of(enumType))
                .addCode("return $1L.values();", enumType)
                .build())
            .build();
    }

}
