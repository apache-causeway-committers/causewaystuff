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

import java.util.List;

import javax.lang.model.element.Modifier;

import org.causewaystuff.companion.codegen.model.OrmModel;
import org.causewaystuff.tooling.javapoet.ParameterSpec;
import org.causewaystuff.tooling.javapoet.TypeSpec;

import org.apache.causeway.applib.annotation.Optionality;
import org.apache.causeway.applib.annotation.PrecedingParamsPolicy;

import lombok.val;
import lombok.experimental.UtilityClass;

@UtilityClass
class _GenEntity_Params {

    TypeSpec generate(
            final DomainGenerator.Config config,
            final OrmModel.Entity entityModel) {

        val paramsRecord = TypeSpec.recordBuilder("Params")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .addJavadoc("Parameter model for @{link $1L}", entityModel.name())
                .addAnnotation(_Annotations.generated(_GenEntity_Params.class))
                .addRecordComponents(asParameterModelParams(config, entityModel.fields()))
                .build();
        return paramsRecord;
    }

    // -- HELPER

    private Iterable<ParameterSpec> asParameterModelParams(
            final DomainGenerator.Config config,
            final List<OrmModel.Field> fields,
            final Modifier ... modifiers) {
        return fields.stream()
                .map(field->
                    ParameterSpec.builder(
                            field.isEnum()
                                ? field.asJavaEnumType()
                                : field.hasForeignKeys()
                                    ? _Foreign.foreignClassName(field, field.foreignFields().getFirstElseFail(), config)
                                    : field.asJavaType(),
                            field.hasForeignKeys()
                                    ? _Foreign.resolvedFieldName(field)
                                    : field.name(),
                            modifiers)
                    .addJavadoc(field.formatDescription("\n"))
                    .addAnnotation(_Annotations.parameter(attr->attr
                            .precedingParamsPolicy(
                                field.hasDiscriminator()
                                    ? PrecedingParamsPolicy.RESET
                                    : PrecedingParamsPolicy.PRESERVE_CHANGES)
                            .optionality(
                                field.requiredInTheUi()
                                    ? Optionality.MANDATORY
                                    : Optionality.OPTIONAL)))
                    .addAnnotation(_Annotations.parameterLayout(attr->attr
                            .describedAs(field.formatDescription("\n"))
                            .multiLine(field.multiLine().orElse(0))))
                    .build())
                .toList();
    }

}
