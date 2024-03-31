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

import io.github.causewaystuff.companion.codegen.model.OrmModel;
import io.github.causewaystuff.tooling.javapoet.ClassName;
import io.github.causewaystuff.tooling.javapoet.FieldSpec;
import io.github.causewaystuff.tooling.javapoet.TypeName;
import io.github.causewaystuff.tooling.javapoet.TypeSpec;

import org.apache.causeway.commons.internal.assertions._Assert;
import org.apache.causeway.commons.internal.base._NullSafe;
import org.apache.causeway.commons.internal.base._Strings;

import lombok.experimental.UtilityClass;

@UtilityClass
class _Enums {

    TypeSpec enumForColumn(final TypeName columnType, final List<OrmModel.EnumConstant> enumConsts) {
        _Assert.assertFalse(_NullSafe.isEmpty(enumConsts));
        var field = enumConsts.get(0).parentField();

        var builder = TypeSpec.enumBuilder(_Strings.capitalize(field.name()))
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(_Annotations.requiredArgsConstructor())
                .addField(FieldSpec.builder(columnType, "matchOn", Modifier.PRIVATE, Modifier.FINAL)
                        .addAnnotation(_Annotations.getter())
                        .build())
                .addField(FieldSpec.builder(ClassName.get(String.class), "title", Modifier.PRIVATE, Modifier.FINAL)
                        .addAnnotation(_Annotations.getter())
                        .addAnnotation(_Annotations.accessorsFluent())
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
        return builder.build();
    }

}
