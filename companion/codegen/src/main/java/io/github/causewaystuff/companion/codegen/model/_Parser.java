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
package io.github.causewaystuff.companion.codegen.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.lang.Nullable;

import org.apache.causeway.applib.annotation.Where;
import org.apache.causeway.commons.functional.IndexedFunction;
import org.apache.causeway.commons.internal.assertions._Assert;
import org.apache.causeway.commons.internal.base._Strings;
import org.apache.causeway.commons.io.TextUtils;
import org.apache.causeway.commons.io.YamlUtils;

import lombok.val;
import lombok.experimental.UtilityClass;

import io.github.causewaystuff.commons.base.types.internal.ObjectRef;
import io.github.causewaystuff.commons.base.types.internal.SneakyRef;
import io.github.causewaystuff.companion.codegen.model.OrmModel.Entity;
import io.github.causewaystuff.companion.codegen.model.OrmModel.EnumConstant;
import io.github.causewaystuff.companion.codegen.model.OrmModel.Field;
import io.github.causewaystuff.companion.codegen.model.OrmModel.Schema;

@UtilityClass
class _Parser {

    @SuppressWarnings({ "rawtypes", "unchecked" })
    Schema parseSchema(final String yaml) {
        val entities = new TreeMap<String, OrmModel.Entity>();
        YamlUtils.tryRead(Map.class, yaml)
        .ifFailureFail()
        .getValue()
        .map(map->(Map<String, Map>)map)
        .ifPresent(map->{
            map.entrySet().stream()
            .map(_Parser::parseEntity)
            .forEach(entity->entities.put(entity.key(), entity));
        });
        var schema = new Schema(entities);
        return schema;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    Entity parseEntity(final Map.Entry<String, Map> entry) {
        val map = entry.getValue();
        val fieldsAsMap = (Map<String, Map>)map.get("fields");
        final String namespace = (String)map.get("namespace");
        final String name = _Strings.nonEmpty((String)map.get("name"))
                .orElseGet(()->
                    entry.getKey().startsWith(namespace)
                        ? entry.getKey().substring(namespace.length() + 1)
                        : entry.getKey()
                );
        val entity = new Entity(
                ObjectRef.empty(),
                name,
                namespace,
                (String)map.get("table"),
                parseNullableStringTrimmed((String)map.get("superType")),
                parseMultilineStringTrimmed((String)map.get("secondaryKey")),
                parseNullableBoolean((Boolean)map.get("suppressUniqueConstraint")),
                (String)map.get("title"),
                (String)map.get("icon"),
                parseNullableBoolean((Boolean)map.get("iconService")),
                parseMultilineString((String)map.get("description")),
                new ArrayList<>());
        fieldsAsMap.entrySet().stream()
                .map(IndexedFunction.zeroBased((index, innerEntry)->parseField(entity, index, innerEntry)))
                .forEach(entity.fields()::add);
        //validate
//        entity.secondaryKeyFields().forEach((final OrmModel.Field f)->_Assert.assertEquals(
//                Can.empty(), Can.ofCollection(f.foreignKeys()),
//                    ()->String.format("invalid secondary key member %s#%s: must not have any foreign-keys",
//                            entity.name(), f.name())));
        return entity;
    }

    Field parseField(final Entity parent, final int ordinal,
            @SuppressWarnings("rawtypes") final Map.Entry<String, Map> entry) {
        val map = entry.getValue();
        return new Field(SneakyRef.of(parent),
                ordinal,
                entry.getKey(),
                (String)map.get("column"),
                (String)map.get("column-type"),
                (Boolean)map.get("required"),
                (Boolean)map.get("unique"),
                (boolean)Optional.ofNullable((Boolean)map.get("plural")).orElse(false),
                parseNullableIntegerWithBounds((Integer)map.get("multiLine"), 2, 1000),
                (String)map.get("elementType"),
                parseNullableWhere((String)map.get("hiddenWhere")),
                parseMultilineStringTrimmed((String)map.get("enum")),
                parseMultilineStringTrimmed((String)map.get("discriminator")),
                parseMultilineStringTrimmed((String)map.get("foreignKeys")),
                parseMultilineString((String)map.get("description")));
    }

    EnumConstant parseEnum(final Field field, final int ordinal, final String enumDeclarationLine) {
        // syntax: <matcher>:<enum-value-name>:<description>
        // 1:NOT_FOUND:Item was not found
        var cutter = TextUtils.cutter(enumDeclarationLine);
        _Assert.assertTrue(cutter.contains(":"));
        var matchOn = cutter.keepBefore(":").getValue();
        cutter = cutter.keepAfter(":");
        var hasDescription = cutter.contains(":");
        var name = cutter.keepBefore(":").getValue();
        cutter = cutter.keepAfter(":");
        String description = hasDescription ? cutter.getValue() : null;
        return new EnumConstant(SneakyRef.of(field), ordinal, name, matchOn, description);
    }

    // -- HELPER

    private static Where parseNullableWhere(final String whereLiteral) {
        return _Strings.isNullOrEmpty(whereLiteral)
                ? null
                : Where.valueOf(whereLiteral);
    }

    private static boolean parseNullableBoolean(final Boolean bool) {
        return Boolean.TRUE.equals(bool);
    }

    private static OptionalInt parseNullableIntegerWithBounds(
            final @Nullable Integer value, final int lowerBound, final int upperBound) {
        return value==null
                || value < lowerBound
                || value > upperBound
                ? OptionalInt.empty()
                : OptionalInt.of(value);
    }

    private static List<String> parseMultilineString(final String input) {
        return _Strings.splitThenStream(input, "\n")
            .filter(_Strings::isNotEmpty)
            .collect(Collectors.toList());
    }

    private static List<String> parseMultilineStringTrimmed(final String input) {
        return _Strings.splitThenStream(input, "\n")
            .filter(_Strings::isNotEmpty)
            .map(String::trim)
            .collect(Collectors.toList());
    }

    private static String parseNullableStringTrimmed(final String input) {
        return Optional.ofNullable(input).stream()
            .map(String::trim)
            .filter(_Strings::isNotEmpty)
            .findFirst()
            .orElse(null);
    }

}
