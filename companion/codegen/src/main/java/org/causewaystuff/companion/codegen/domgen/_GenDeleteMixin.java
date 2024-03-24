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

import org.causewaystuff.companion.applib.services.lookup.DependantLookupService;
import org.causewaystuff.companion.applib.services.lookup.ForeignKeyLookupService;
import org.causewaystuff.companion.applib.services.search.SearchService;
import org.causewaystuff.companion.codegen.domgen.DomainGenerator.QualifiedType;
import org.causewaystuff.companion.codegen.model.OrmModel;
import org.causewaystuff.tooling.javapoet.ClassName;
import org.causewaystuff.tooling.javapoet.MethodSpec;
import org.causewaystuff.tooling.javapoet.ParameterSpec;
import org.causewaystuff.tooling.javapoet.TypeSpec;

import org.apache.causeway.applib.annotation.ActionLayout.Position;
import org.apache.causeway.applib.annotation.LabelPosition;
import org.apache.causeway.applib.annotation.SemanticsOf;
import org.apache.causeway.applib.services.repository.RepositoryService;

import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.experimental.UtilityClass;

@UtilityClass
class _GenDeleteMixin {

    QualifiedType qualifiedType(
            final DomainGenerator.Config config,
            final OrmModel.Entity entityModel) {

        val packageName = config.fullPackageName(entityModel.namespace());

        val typeModelBuilder = TypeSpec.classBuilder(_Mixins.customMixinClassName(entityModel, "delete"))
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(_Annotations.action(attr->attr.semantics(SemanticsOf.IDEMPOTENT_ARE_YOU_SURE)))
                .addAnnotation(_Annotations.actionLayout(attr->attr
                        .fieldSetId("details")
                        .sequence("9")
                        .describedAs("Delete this " + entityModel.name())
                        .position(Position.PANEL)))
                .addAnnotation(RequiredArgsConstructor.class)
                .addField(_Fields.inject(DependantLookupService.class, "dependantService"))
                .addField(_Fields.inject(ForeignKeyLookupService.class, "foreignKeyLookup"))
                .addField(_Fields.inject(RepositoryService.class, "repositoryService"))
                .addField(_Fields.inject(SearchService.class, "searchService"))
                .addField(_Fields.mixee(ClassName.get(packageName, entityModel.name()), Modifier.FINAL, Modifier.PRIVATE))
                .addMethod(MethodSpec.methodBuilder("act")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(_Annotations.memberSupport())
                        .returns(ClassName.get("", entityModel.name() + ".Manager"))
                        .addParameter(ParameterSpec.builder(ClassName.get(String.class), "dependants")
                                .addAnnotation(_Annotations.parameterLayout(args->args
                                        .multiLine(12)
                                        .labelPosition(LabelPosition.TOP)))
                                .build())
                        .addCode("""
                                repositoryService.remove(mixee);
                                foreignKeyLookup.clearCache(mixee.getClass());
                                return new $1L.Manager(searchService, "");""",
                                entityModel.name())
                        .build())
                .addMethod(MethodSpec.methodBuilder("default0Act")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(_Annotations.memberSupport())
                        .returns(ClassName.get(String.class))
                        .addCode("""
                                return dependantService.findAllDependantsAsMultilineString(mixee);""")
                        .build())
                ;

        return new QualifiedType(
                packageName,
                typeModelBuilder.build());
    }

}
