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
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.lang.Nullable;

import org.apache.causeway.commons.collections.Can;
import org.apache.causeway.commons.functional.IndexedFunction;
import org.apache.causeway.commons.internal.assertions._Assert;
import org.apache.causeway.commons.internal.base._Strings;
import org.apache.causeway.commons.io.TextUtils;
import org.apache.causeway.commons.io.YamlUtils;

import lombok.val;
import lombok.experimental.UtilityClass;

import io.github.causewaystuff.commons.base.types.internal.ObjectRef;
import io.github.causewaystuff.commons.base.types.internal.SneakyRef;
import io.github.causewaystuff.companion.codegen.model.Schema.Domain;
import io.github.causewaystuff.companion.codegen.model.Schema.Entity;
import io.github.causewaystuff.companion.codegen.model.Schema.EntityField;
import io.github.causewaystuff.companion.codegen.model.Schema.EnumConstant;
import io.github.causewaystuff.companion.codegen.model.Schema.Field;
import io.github.causewaystuff.companion.codegen.model.Schema.Viewmodel;
import io.github.causewaystuff.companion.codegen.model.Schema.VmField;

@UtilityClass
class _Parser {

    record ParserHint(
            @Nullable String sourceFileName) {
        static ParserHint empty() {
            return new ParserHint(null);
        }
        String name(final String name) {
            return _Strings.isNotEmpty(name)
                    ? name
                    : _Strings.isNotEmpty(sourceFileName)
                        ? sourceFileName
                        : name;
        }
    }

    Domain parseSchema(final String yaml) {
        return parseSchema(yaml, ParserHint.empty());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    Domain parseSchema(final String yaml, final ParserHint parserHint) {
        val viewmodels = new TreeMap<String, Schema.Viewmodel>();
        val entities = new TreeMap<String, Schema.Entity>();
        YamlUtils.tryRead(Map.class, yaml)
        .ifFailureFail()
        .getValue()
        .map(map->(Map<String, Map>)map)
        .ifPresent(map->{
            map.entrySet().stream()
            .map(entry->_Parser.parseDomainObjects(entry, parserHint))
            .flatMap(Can::stream)
            .forEach(domainObj->{
                switch (domainObj) {
                case Schema.Viewmodel viewmodel ->
                    viewmodels.put(viewmodel.fqn(), viewmodel);
                case Schema.Entity entity ->
                    entities.put(entity.fqn(), entity);
                }
            });
        });
        var domain = new Domain(viewmodels, entities);
        return domain;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    Can<Schema.DomainObject> parseDomainObjects(
            final Map.Entry<String, Map> entry,
            final ParserHint parserHint) {
        return switch (entry.getKey()) {
            case "viewmodel" -> Can.of(parseViewmodel(entry, parserHint));
            case "entity" -> Can.of(parseEntity(entry, parserHint));
            case "viewmodels" ->
                String.class.isInstance(entry.getValue())
                    ? Can.empty()
                    : parseViewmodels((List)entry.getValue());
            case "entities" ->
                String.class.isInstance(entry.getValue())
                    ? Can.empty()
                    : parseEntities((List)entry.getValue());
            default ->
                throw new IllegalArgumentException("Unexpected domain-object type: " + entry.getKey());
        };
    }

    @SuppressWarnings("rawtypes")
    private Can<Schema.DomainObject> parseViewmodels(
            final List<Map> viewmodels) {
        return viewmodels.stream()
                .map(entityAsMap->Map.of("", entityAsMap).entrySet().iterator().next())
                .map(entityAsMapEntry->
                    parseViewmodel(entityAsMapEntry, ParserHint.empty()))
                .collect(Can.toCan());
    }

    @SuppressWarnings("rawtypes")
    private Can<Schema.DomainObject> parseEntities(
            final List<Map> entities) {
        return entities.stream()
                .map(entityAsMap->Map.of("", entityAsMap).entrySet().iterator().next())
                .map(entityAsMapEntry->
                    parseEntity(entityAsMapEntry, ParserHint.empty()))
                .collect(Can.toCan());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    Viewmodel parseViewmodel(
            final Map.Entry<String, Map> entry,
            final ParserHint parserHint) {
        val map = entry.getValue();
        val fieldsAsMap = (Map<String, Map>)map.get("fields");
        final String namespace = (String)map.get("namespace");
        final String name = _Strings.nonEmpty(parserHint.name((String)map.get("name")))
                .orElseGet(()->
                    entry.getKey().startsWith(namespace)
                        ? entry.getKey().substring(namespace.length() + 1)
                        : entry.getKey()
                );
        val viewmodel = new Viewmodel(
                ObjectRef.empty(),
                Optional.ofNullable((String)map.get("generator")).orElse("class"),
                name,
                namespace,
                (String)map.get("title"),
                (String)map.get("icon"),
                parseNullableBoolean((Boolean)map.get("iconService")),
                (String)map.get("named"),
                parseMultilineString((String)map.get("description")),
                new ArrayList<>());
        fieldsAsMap.entrySet().stream()
                .map(IndexedFunction.zeroBased((index, innerEntry)->parseField(viewmodel, index, innerEntry)))
                .forEach(viewmodel.fields()::add);
        return viewmodel;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    Entity parseEntity(
            final Map.Entry<String, Map> entry,
            final ParserHint parserHint) {
        val map = entry.getValue();
        val fieldsAsMap = (Map<String, Map>)map.get("fields");
        final String namespace = (String)map.get("namespace");
        final String name = _Strings.nonEmpty(parserHint.name((String)map.get("name")))
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
                (String)map.get("named"),
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

    VmField parseField(final Viewmodel parent, final int ordinal,
            @SuppressWarnings("rawtypes") final Map.Entry<String, Map> entry) {
        val map = entry.getValue();
        return new VmField(SneakyRef.of(parent),
                ordinal,
                entry.getKey(),
                (String)map.get("type"),
                (boolean)Optional.ofNullable((Boolean)map.get("required")).orElse(true),
                (boolean)Optional.ofNullable((Boolean)map.get("plural")).orElse(false),
                (String)map.get("elementType"),
                PropertyLayoutSpec.fromMap(map),
                parseMultilineStringTrimmed((String)map.get("enum")),
                Multiline.parseMultilineString((String)map.get("description")));
    }

    EntityField parseField(final Entity parent, final int ordinal,
            @SuppressWarnings("rawtypes") final Map.Entry<String, Map> entry) {
        val map = entry.getValue();
        return new EntityField(SneakyRef.of(parent),
                ordinal,
                entry.getKey(),
                (String)map.get("column"),
                (String)map.get("column-type"),
                (Boolean)map.get("required"),
                (Boolean)map.get("unique"),
                (boolean)Optional.ofNullable((Boolean)map.get("plural")).orElse(false),
                (String)map.get("elementType"),
                PropertyLayoutSpec.fromMap(map),
                parseMultilineStringTrimmed((String)map.get("enum")),
                parseMultilineStringTrimmed((String)map.get("discriminator")),
                parseMultilineStringTrimmed((String)map.get("foreignKeys")),
                Multiline.parseMultilineString((String)map.get("description")));
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

    private static boolean parseNullableBoolean(final Boolean bool) {
        return Boolean.TRUE.equals(bool);
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
