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

import java.util.stream.Collectors;

import javax.lang.model.element.Modifier;

import org.apache.causeway.applib.annotation.Navigable;
import org.apache.causeway.applib.annotation.Snapshot;
import org.apache.causeway.applib.annotation.Where;
import org.apache.causeway.applib.fa.FontAwesomeLayers;
import org.apache.causeway.commons.collections.Can;
import org.apache.causeway.commons.io.DataSource;

import lombok.experimental.UtilityClass;

import io.github.causewaystuff.tooling.javapoet.ClassName;
import io.github.causewaystuff.tooling.javapoet.CodeBlock;
import io.github.causewaystuff.tooling.javapoet.MethodSpec;
import io.github.causewaystuff.tooling.javapoet.ParameterizedTypeName;

@UtilityClass
class _Methods {

    MethodSpec toString(final CodeBlock code) {
        return MethodSpec.methodBuilder("toString")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(_Annotations.override())
            .returns(ClassName.get("java.lang", "String"))
            .addCode(code)
            .build();
    }

    MethodSpec objectSupport(final String methodName, final CodeBlock code, final Modifier ... modifiers) {
        return MethodSpec.methodBuilder(methodName)
            .addModifiers(modifiers)
            .addAnnotation(_Annotations.objectSupport())
            .returns(ClassName.get("java.lang", "String"))
            .addCode(code)
            .build();
    }

    MethodSpec iconFaLayers(final Modifier ... modifiers) {
        return MethodSpec.methodBuilder("iconFaLayers")
            .addModifiers(modifiers)
            .addAnnotation(_Annotations.objectSupport())
            .returns(ClassName.get(FontAwesomeLayers.class))
            .addCode("return iconFaService.iconFaLayers(this);")
            .build();
    }

    MethodSpec viewModelMemento(final CodeBlock code) {
        return MethodSpec.methodBuilder("viewModelMemento")
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addAnnotation(_Annotations.override())
            .returns(ClassName.get("java.lang", "String"))
            .addCode(code)
            .build();
    }

    MethodSpec managerSearch(final String entityName) {
        return MethodSpec.methodBuilder("getListOf" + entityName)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addAnnotation(_Annotations.collection(attr->attr))
            .returns(ParameterizedTypeName.get(
                    ClassName.get(java.util.List.class),
                    ClassName.get("", entityName)))
            .addCode("""
                    return searchService.search($1L.class, $1L::title, search);""",
                    entityName)
            .build();
    }

    MethodSpec navigableParent(final String entityName) {
        return MethodSpec.methodBuilder("getNavigableParent")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(_Annotations.property(attr->attr
                    .snapshot(Snapshot.EXCLUDED)))
            .addAnnotation(_Annotations.propertyLayout(attr->attr
                    .hiddenWhere(Where.EVERYWHERE)
                    .navigable(Navigable.PARENT)))
            .addAnnotation(_Annotations.notPersistent())
            .returns(ClassName.get("", entityName + ".Manager"))
            .addCode("""
                    return new $1L.Manager(searchService, "");""",
                    entityName)
            .build();
    }

    // public static DataSource schemaSource() {
    //     return DataSource.ofResource(MyModule.class, "/companion-schema.yaml");
    // }
    MethodSpec schemaSource(final String methodName, final ClassName ownerClassName, final String resource,
            final Modifier ... modifiers) {
        return MethodSpec.methodBuilder(methodName)
            .addModifiers(modifiers)
            .returns(ClassName.get(DataSource.class))
            .addCode("""
                    return DataSource.ofResource($1L.class, $2S);""",
                    ownerClassName,
                    resource
                    )
            .build();
    }

    MethodSpec classList(final String methodName, final Can<String> classNames, final Modifier ... modifiers) {
        return MethodSpec.methodBuilder(methodName)
            .addModifiers(modifiers)
            .returns(ParameterizedTypeName.get(
                    ClassName.get(Can.class),
                    ParameterizedTypeName.get(
                            ClassName.get(Class.class),
                            ClassName.get("", "?"))))
            .addCode("""
                    return Can.of($1L);""",
                    classNames.stream()
                        .map(name->name + ".class")
                        .collect(Collectors.joining(",\n")))
            .build();
    }

}
