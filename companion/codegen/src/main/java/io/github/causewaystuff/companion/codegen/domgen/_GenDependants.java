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

import io.github.causewaystuff.companion.applib.decorate.CollectionTitleDecorator;
import io.github.causewaystuff.companion.applib.services.lookup.DependantLookupService;
import io.github.causewaystuff.companion.codegen.domgen.DomainGenerator.QualifiedType;
import io.github.causewaystuff.companion.codegen.model.Schema;
import io.github.causewaystuff.companion.codegen.model.Schema.Entity;

import org.springframework.context.annotation.Configuration;
import io.github.causewaystuff.tooling.javapoet.AnnotationSpec;
import io.github.causewaystuff.tooling.javapoet.ClassName;
import io.github.causewaystuff.tooling.javapoet.MethodSpec;
import io.github.causewaystuff.tooling.javapoet.ParameterizedTypeName;
import io.github.causewaystuff.tooling.javapoet.TypeSpec;

import org.apache.causeway.commons.collections.Can;

import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.experimental.UtilityClass;

@UtilityClass
class _GenDependants {

    QualifiedType qualifiedType(
            final DomainGenerator.Config config,
            final Schema.Entity entityModel,
            final Can<DependantMixinSpec> mixinSpecs) {

        val packageName = config.fullPackageName(entityModel.namespace());

        val typeModelBuilder = TypeSpec.classBuilder(entityModel.name() + "Deps")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(_Annotations.generated(_GenDependants.class))
                .addAnnotation(AnnotationSpec.builder(Configuration.class)
                        .build());

        // inner mixin classes

        mixinSpecs.forEach(mixinSpec->{
            Schema.Field fieldWithForeignKeys = mixinSpec.fieldWithForeignKeys();
            Can<Schema.Field> foreignFields = mixinSpec.foreignFields();
            ClassName propertyMixinClassName = mixinSpec.propertyMixinClassName();
            val localEntity = mixinSpec.localEntity();

            val innerMixin = TypeSpec.classBuilder(mixinSpec.mixinClassName())
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addAnnotation(_Annotations.collection(attr->attr))
                .addAnnotation(_Annotations.collectionLayout(attr->attr.tableDecorator(CollectionTitleDecorator.class)))
                .addAnnotation(RequiredArgsConstructor.class)
                .addField(_Fields.inject(DependantLookupService.class, "dependantLookup"))
                .addField(_Fields.mixee(ClassName.get(packageName, localEntity.name()), Modifier.FINAL, Modifier.PRIVATE))
                .addMethod(mixedInCollection(config, localEntity, fieldWithForeignKeys, foreignFields, propertyMixinClassName,
                        Modifier.PUBLIC))
                ;

            typeModelBuilder.addType(innerMixin.build());
        });

        // static method that provides all mixin classes we generated above
        typeModelBuilder.addMethod(_Methods.classList("mixinClasses",
                mixinSpecs.map(DependantMixinSpec::mixinClassName), Modifier.PUBLIC, Modifier.STATIC));

        return new QualifiedType(
                packageName,
                typeModelBuilder.build());
    }

    static record DependantMixinSpec(
        Schema.Field fieldWithForeignKeys,
                // all sharing the same foreignEntity, as guaranteed by the caller
        Can<Schema.Field> foreignFields,
        ClassName propertyMixinClassName) {
        /**
         * entity this mixin contributes to
         */
        Entity localEntity() {
            return foreignFields.getFirstElseFail().parentEntity();
        }
        String mixinClassName() {
            return _Mixins.collectionMixinClassName(localEntity(), fieldWithForeignKeys);
        }
    }

    // -- HELPER

    private MethodSpec mixedInCollection(
            final DomainGenerator.Config config,
            final Schema.Entity localEntity, // entity this mixin contributes to
            final Schema.Field fieldWithForeignKeys,
            final Can<Schema.Field> foreignFields,
            final ClassName associationMixinClassName,
            final Modifier ... modifiers) {

        val dependantEntity = fieldWithForeignKeys.parentEntity();
        val dependantType = config.javaPoetClassName(dependantEntity);

        final MethodSpec.Builder builder = MethodSpec.methodBuilder("coll")
                .addModifiers(modifiers)
                .addAnnotation(_Annotations.memberSupport())
                .returns(ParameterizedTypeName.get(ClassName.get(java.util.List.class), dependantType))
                .addCode("""
                        return dependantLookup.findDependants(
                            $1T.class,
                            $2T.class,
                            $2T::$3L,
                            mixee);
                        """,
                        dependantType,
                        associationMixinClassName,
                        fieldWithForeignKeys.plural()
                            ? "coll"
                            : "prop");
        return builder.build();
    }

}
