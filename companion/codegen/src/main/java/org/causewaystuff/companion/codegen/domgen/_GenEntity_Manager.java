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

import org.causewaystuff.companion.applib.services.search.SearchService;
import org.causewaystuff.companion.codegen.model.OrmModel;
import org.causewaystuff.tooling.javapoet.ClassName;
import org.causewaystuff.tooling.javapoet.CodeBlock;
import org.causewaystuff.tooling.javapoet.FieldSpec;
import org.causewaystuff.tooling.javapoet.TypeSpec;

import org.apache.causeway.applib.ViewModel;
import org.apache.causeway.applib.annotation.Editing;
import org.apache.causeway.applib.annotation.Nature;
import org.apache.causeway.applib.annotation.Optionality;
import org.apache.causeway.commons.internal.base._Strings;

import lombok.experimental.UtilityClass;

@UtilityClass
class _GenEntity_Manager {

    TypeSpec generate(
            final DomainGenerator.Config config,
            final OrmModel.Entity entityModel) {

        var managerViewmodel = TypeSpec.classBuilder("Manager")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .addJavadoc("Manager Viewmodel for @{link $1L}", entityModel.name())
                .addSuperinterface(ClassName.get(ViewModel.class))
                .addAnnotation(_Annotations.generated(_GenEntity_Manager.class))
                .addAnnotation(_Annotations.named(config.fullLogicalName(entityModel.namespace())
                        + "." + entityModel.name() + ".Manager"))
                .addAnnotation(_Annotations.domainObject(Nature.VIEW_MODEL))
                .addAnnotation(_Annotations.domainObjectLayout(
                        entityModel.formatDescription("\n"),
                        entityModel.icon()))
                .addAnnotation(_Annotations.allArgsConstructor())
                .addField(FieldSpec.builder(SearchService.class, "searchService", Modifier.PUBLIC, Modifier.FINAL)
                        .build())
                .addField(FieldSpec.builder(String.class, "search", Modifier.PRIVATE)
                        .addAnnotation(_Annotations.property(attr->attr
                                .optionality(Optionality.OPTIONAL)
                                .editing(Editing.ENABLED)))
                        .addAnnotation(_Annotations.propertyLayout(attr->attr
                                .fieldSetId("searchBar")))
                        .addAnnotation(_Annotations.getter())
                        .addAnnotation(_Annotations.setter())
                        .build())
                .addMethod(_Methods.objectSupport("title",
                        CodeBlock.of("""
                                return "Manage $1L";""", _Strings.asNaturalName.apply(entityModel.name())),
                        Modifier.PUBLIC))
                .addMethod(_Methods.managerSearch(entityModel.name()))
                .addMethod(_Methods.viewModelMemento(CodeBlock.of("return getSearch();")))
                .build();

        return managerViewmodel;
    }

}
