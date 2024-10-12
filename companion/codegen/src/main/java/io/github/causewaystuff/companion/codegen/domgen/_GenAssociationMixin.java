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

import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.lang.model.element.Modifier;

import org.apache.causeway.applib.annotation.Snapshot;
import org.apache.causeway.applib.annotation.Where;
import org.apache.causeway.commons.collections.Can;
import org.apache.causeway.commons.internal.assertions._Assert;
import org.apache.causeway.commons.internal.exceptions._Exceptions;

import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.experimental.UtilityClass;

import io.github.causewaystuff.companion.applib.services.lookup.ForeignKeyLookupService;
import io.github.causewaystuff.companion.codegen.domgen.DomainGenerator.QualifiedType;
import io.github.causewaystuff.companion.codegen.model.Schema;
import io.github.causewaystuff.tooling.javapoet.ClassName;
import io.github.causewaystuff.tooling.javapoet.MethodSpec;
import io.github.causewaystuff.tooling.javapoet.ParameterizedTypeName;
import io.github.causewaystuff.tooling.javapoet.TypeSpec;

@UtilityClass
class _GenAssociationMixin {

    QualifiedType qualifiedType(
            final DomainGenerator.Config config,
            final Schema.EntityField fieldWithForeignKeys,
            final Can<Schema.EntityField> foreignFields) {

        val entityModel = fieldWithForeignKeys.parentEntity();
        val packageName = config.fullPackageName(entityModel.namespace()); // shared with entity and mixin

        val isPlural = fieldWithForeignKeys.plural();
        val distinctForeignEntities = foreignFields.stream()
                .map(Schema.EntityField::parentEntity)
                .distinct()
                .collect(Can.toCan());
        val useEitherPattern = foreignFields.size()==2
                && distinctForeignEntities.isCardinalityMultiple();

        val typeModelBuilder = TypeSpec.classBuilder(_Mixins.propertyMixinClassName(fieldWithForeignKeys))
                .addAnnotation(_Annotations.generated(_GenAssociationMixin.class))
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(isPlural
                        ? _Annotations.collection(attr->attr)
                        : _Annotations.property(attr->attr.snapshot(Snapshot.EXCLUDED)))
                .addAnnotation(
                        isPlural
                        ? _Annotations.collectionLayout(attr->attr
                                .describedAs(fieldWithForeignKeys.description().formatDescription("\n"))
                                .hiddenWhere(Where.NOWHERE))
                        : _Annotations.propertyLayout(attr->attr
                                .fieldSet("details")
                                .sequence(fieldWithForeignKeys.sequence() + ".1")
                                .describedAs(fieldWithForeignKeys.description().formatDescription("\n"))
                                .hiddenWhere(useEitherPattern
                                    ? Where.NOWHERE
                                    : Where.REFERENCES_PARENT),
                            null /*no override*/)
                )
                .addAnnotation(RequiredArgsConstructor.class)
                .addField(_Fields.inject(ForeignKeyLookupService.class, "foreignKeyLookup"))
                .addField(_Fields.mixee(ClassName.get(packageName, entityModel.name()), Modifier.FINAL, Modifier.PRIVATE))
                ;

        mixedInAssociation(config, fieldWithForeignKeys, foreignFields, Modifier.PUBLIC)
            .ifPresent(typeModelBuilder::addMethod);

        return new QualifiedType(
                packageName,
                typeModelBuilder.build());
    }

    // -- HELPER

    private Optional<MethodSpec> mixedInAssociation(
            final DomainGenerator.Config config,
            final Schema.EntityField field,
            final Can<Schema.EntityField> foreignFields,
            final Modifier ... modifiers) {

        val isPlural = field.plural();
        val localKeyGetter = field.getter();

        record Foreign(
                ClassName foreignEntity,
                String strictness,
                String foreignKeyGetter,
                int secondaryKeyCardinality,
                String significantArgument,
                boolean isSignificantArgumentReguired,
                String argList) {
        }

        final Can<Foreign> foreigners = foreignFields
                .map((final Schema.EntityField foreignField)->{
                    val foreignEntityClass = _Foreign.foreignClassName(field, foreignField, config);
                    var argList = Can.ofCollection(field.discriminatorFields())
                            .add(field)
                            .stream()
                            .map(Schema.EntityField::getter)
                            .map(getter->String.format("mixee.%s()", getter))
                            .collect(Collectors.toCollection(ArrayList::new));
                    val argCount = argList.size();
                    val significantArgument = argList.get(argCount-1);

                    final int foreignSecondaryKeyArgCount = foreignField.parentEntity().secondaryKey().size();

                    // fill up with null args
                    final int fillSize = foreignSecondaryKeyArgCount - argCount;
                    IntStream.range(0, fillSize)
                        .forEach(__->argList.add("null"));

                    val strictness = field.required()
                            ? "unique"
                            : "nullable";

                    return new Foreign(foreignEntityClass, strictness,
                            foreignField.getter(), foreignSecondaryKeyArgCount,
                            significantArgument,
                            field.required(),
                            argList.stream().collect(Collectors.joining(", ")));
                });

        if(foreigners.getCardinality().isZero()) {
            return Optional.empty();
        }

        val distinctForeignEntities = foreignFields.stream()
                .map(Schema.EntityField::parentEntity)
                .distinct()
                .collect(Can.toCan());

        final MethodSpec.Builder builder = MethodSpec.methodBuilder(isPlural
                    ? "coll"
                    : "prop")
                .addModifiers(modifiers)
                .addAnnotation(_Annotations.memberSupport())
                .returns(distinctForeignEntities.isCardinalityMultiple()
                        ? ClassName.OBJECT // common super type
                        : isPlural
                                ? ParameterizedTypeName.get(
                                        ClassName.get(Can.class),
                                        foreigners.getFirstElseFail().foreignEntity())
                                : foreigners.getFirstElseFail().foreignEntity());

        if(isPlural) {
            _Assert.assertTrue(distinctForeignEntities.isCardinalityOne(),
                    ()->"not implemented for multiple referenced foreign entity types");

            val foreignType = foreigners.getFirstElseFail().foreignEntity();
            builder.addCode("""
                return foreignKeyLookup.decodeLookupKeyList($1T.class, mixee.$2L())
                    .map(foreignKeyLookup::unique);
                """,
                foreignType,
                localKeyGetter);

            return Optional.of(builder.build());
        }

        switch(foreigners.size()) {
        case 1: {

            var foreigner = foreigners.getSingletonOrFail();

            if(foreigner.secondaryKeyCardinality()==0) {
                throw _Exceptions.unrecoverable("%s needs to implement a SecondaryKey", foreigner.foreignEntity());
            }

            if(foreigner.isSignificantArgumentReguired()) {
                builder.addCode("""
                        final var lookupKey = new $2T.SecondaryKey($3L);
                        return foreignKeyLookup.$1L(lookupKey);
                        """, foreigner.strictness(), foreigner.foreignEntity(), foreigner.argList()
                        );
            } else {
                builder.addCode("""
                        if($4L==null) return null;
                        final var lookupKey = new $2T.SecondaryKey($3L);
                        return foreignKeyLookup.$1L(lookupKey);
                        """, foreigner.strictness(), foreigner.foreignEntity(), foreigner.argList(),
                        foreigner.significantArgument()
                        );
            }
            break;

        }
        case 2: {
            val foreigner1 = foreigners.getElseFail(0);
            val foreigner2 = foreigners.getElseFail(1);
            if(distinctForeignEntities.isCardinalityOne()) {
                // SHARED FOREIGN ENTITY TYPE
                builder.addCode("""
                        return foreignKeyLookup.decodeLookupKeyList($1T.class, mixee.$2L())
                            .map(foreignKeyLookup::unique)
                            .getSingleton().orElse(null);
                        """, foreigner1.foreignEntity(),
                        localKeyGetter);
            } else {
                // TWO FOREIGN ENTITY TYPES

                builder.addCode("""
                        final int switchOn = foreignKeyLookup.switchOn(mixee);
                        switch(switchOn) {
                        case 1: {
                            if($7L==null) return null;
                            final var lookupKey = new $5T.SecondaryKey($3L);
                            return foreignKeyLookup.$1L(lookupKey);
                        }
                        case 2: {
                            if($8L==null) return null;
                            final var lookupKey = new $6T.SecondaryKey($4L);
                            return foreignKeyLookup.$2L(lookupKey);
                        }}
                        throw $9T.unexpectedCodeReach();
                        """,
                        foreigner1.strictness(),
                        foreigner2.strictness(),
                        foreigner1.argList(),
                        foreigner2.argList(),
                        foreigner1.foreignEntity(),
                        foreigner2.foreignEntity(),
                        foreigner1.significantArgument(),
                        foreigner2.significantArgument(),
                        _Exceptions.class);
            }
            break;
        }
        default:
            System.err.printf("WARNING: %d foreign key count not supported in %s; skipping mixin generation%n",
                    foreigners.size(),
                    foreignFields.map(f->f.parentEntity().name() + "::" + f.name()));
            return Optional.empty();
        };

        return Optional.of(builder.build());
    }

}
