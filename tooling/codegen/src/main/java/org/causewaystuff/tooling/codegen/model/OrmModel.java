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
package org.causewaystuff.tooling.codegen.model;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.causewaystuff.commons.types.internal.ObjectRef;
import org.causewaystuff.commons.types.internal.SneakyRef;
import org.causewaystuff.tooling.codegen.domgen.LicenseHeader;

import org.causewaystuff.tooling.javapoet.ClassName;
import org.causewaystuff.tooling.javapoet.TypeName;

import org.apache.causeway.applib.services.metamodel.objgraph.ObjectGraph;
import org.apache.causeway.commons.collections.Can;
import org.apache.causeway.commons.functional.IndexedFunction;
import org.apache.causeway.commons.internal.assertions._Assert;
import org.apache.causeway.commons.internal.base._NullSafe;
import org.apache.causeway.commons.internal.base._Strings;
import org.apache.causeway.commons.internal.exceptions._Exceptions;
import org.apache.causeway.commons.internal.primitives._Ints;
import org.apache.causeway.commons.io.DataSource;
import org.apache.causeway.commons.io.FileUtils;
import org.apache.causeway.commons.io.TextUtils;
import org.apache.causeway.commons.io.YamlUtils;

import lombok.SneakyThrows;
import lombok.val;
import lombok.experimental.UtilityClass;

/**
 * Read and write schema model from and to YAML format.
 */
@UtilityClass
public class OrmModel {

    public record Entity(
            ObjectRef<Schema> parentRef,
            String name,
            String namespace,
            String table,
            String superType,
            List<String> secondaryKey,
            /** Whether to suppress the generation of the <code>@Unique</code> annotation on this entity.*/
            boolean suppressUniqueConstraint,
            String title,
            String icon,
            boolean iconService,
            List<String> description,
            List<Field> fields) {
        @SuppressWarnings({ "rawtypes", "unchecked" })
        static Entity parse(final Map.Entry<String, Map> entry) {
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
                    .map(IndexedFunction.zeroBased((index, innerEntry)->Field.parse(entity, index, innerEntry)))
                    .forEach(entity.fields()::add);
            //validate
//            entity.secondaryKeyFields().forEach((final OrmModel.Field f)->_Assert.assertEquals(
//                    Can.empty(), Can.ofCollection(f.foreignKeys()),
//                        ()->String.format("invalid secondary key member %s#%s: must not have any foreign-keys",
//                                entity.name(), f.name())));
            return entity;
        }
        public Schema parentSchema() {
            return parentRef.getValue();
        }
        public String key() {
            return String.format("%s.%s", namespace, name);
        }
        public boolean hasSuperType() {
            return _Strings.isNotEmpty(superType);
        }
        /** fails if hasSuperType()==false */
        public String superTypeSimpleName() {
            _Assert.assertTrue(hasSuperType());
            var cutter = TextUtils.cutter(superType); // expected non-null
            return cutter.keepAfterLast(".").getValue();
        }
        /** fails if hasSuperType()==false */
        public String superTypeNamespace() {
            _Assert.assertTrue(hasSuperType());
            var cutter = TextUtils.cutter(superType); // expected non-null
            return cutter.keepBeforeLast(".").getValue();
        }
        public boolean hasSecondaryKey() {
            return secondaryKey.size()>0;
        }
        public List<Field> secondaryKeyFields() {
            return _NullSafe.stream(secondaryKey)
                    .map(fieldId->fields()
                            .stream()
                            .filter(field->field.column().equalsIgnoreCase(fieldId))
                            .findAny()
                            .orElseThrow(()->_Exceptions
                                    .noSuchElement("secondary-key field not found by column name '%s' in %s",
                                            fieldId, key())))
                    .collect(Collectors.toList());
        }
        String toYaml() {
            val yaml = new YamlWriter();
            yaml.write(key(), ":").nl();
            yaml.ind().write("namespace: ", namespace).nl();
            yaml.ind().write("table: ", table).nl();
            if(_Strings.isNotEmpty(superType)) {
                yaml.ind().write("superType: ", superType).nl();
            }
            yaml.ind().write("secondaryKey:").multilineStartIfNotEmtpy(secondaryKey).nl();
            secondaryKey.forEach(line->
                yaml.ind().ind().writeUpper(line).nl());
            {   // title
                var titleLines = TextUtils.readLines(title);
                if(titleLines.isCardinalityMultiple()) {
                    yaml.ind().write("title:").multilineStartIfNotEmtpy(titleLines.toList()).nl();
                    titleLines.forEach(line->
                        yaml.ind().ind().write(line).nl());
                } else {
                    yaml.ind().write("title: ", title).nl();
                }
            }
            if(suppressUniqueConstraint) {
                yaml.ind().write("suppressUniqueConstraint: ", "true").nl();
            }
            {   // icon
                var iconLines = TextUtils.readLines(icon);
                if(iconLines.isCardinalityMultiple()) {
                    yaml.ind().write("icon:").multilineStartIfNotEmtpy(iconLines.toList()).nl();
                    iconLines.forEach(line->
                        yaml.ind().ind().write(line).nl());
                } else {
                    yaml.ind().write("icon: ", icon).nl();
                }
            }
            if(iconService) {
                yaml.ind().write("iconService: ", "true").nl();
            }
            yaml.ind().write("description:").multilineStartIfNotEmtpy(description).nl();
            description.forEach(line->
                yaml.ind().ind().write(line).nl());
            yaml.ind().write("fields:").nl();
            fields.forEach(field->{
                yaml.ind().ind().write(field.name(), ":").nl();
                yaml.ind().ind().ind().write("column: ", field.column()).nl();
                yaml.ind().ind().ind().write("column-type: ", field.columnType()).nl();
                yaml.ind().ind().ind().write("required: ", ""+field.required()).nl();
                yaml.ind().ind().ind().write("unique: ", ""+field.unique()).nl();
                if(field.plural()) {
                    yaml.ind().ind().ind().write("plural: ", "true").nl();
                }
                if(_Strings.isNotEmpty(field.elementType())) {
                    yaml.ind().ind().ind().write("elementType: ", field.elementType()).nl();
                }
                if(field.isEnum()) {
                    yaml.ind().ind().ind().write("enum:").multilineStartIfNotEmtpy(field.enumeration).nl();
                    field.enumeration.forEach(line->
                        yaml.ind().ind().ind().ind().write(line).nl());
                }
                if(field.hasDiscriminator()) {
                    yaml.ind().ind().ind().write("discriminator:").multilineStartIfNotEmtpy(field.discriminator).nl();
                    field.discriminator.forEach(line->
                        yaml.ind().ind().ind().ind().writeUpper(line).nl());
                }
                if(field.hasForeignKeys()) {
                    yaml.ind().ind().ind().write("foreignKeys:").multilineStartIfNotEmtpy(field.foreignKeys).nl();
                    field.foreignKeys.forEach(line->
                        yaml.ind().ind().ind().ind().writeUpper(line).nl());
                }
                yaml.ind().ind().ind().write("description:").multilineStartIfNotEmtpy(field.description).nl();
                field.description.forEach(line->
                    yaml.ind().ind().ind().ind().write(line).nl());
            });
            return yaml.toString();
        }
        public String formatDescription(final String continuation) {
            if(isMultilineStringBlank(description)) {
                return "has no description";
            }
            return description()
                    .stream()
                    .map(String::trim)
                    .collect(Collectors.joining(continuation));
        }
        public Optional<OrmModel.Field> lookupFieldByColumnName(final String columnName) {
            return fields().stream()
                    .filter(f->f.column().equalsIgnoreCase(columnName))
                    .findFirst();
        }
        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Entity)) {
                return false;
            }
            return this.key().equals(((Entity) o).key());
        }
        @Override
        public int hashCode() {
            return key().hashCode();
        }
    }

    public record Field(
            SneakyRef<Entity> parentRef,
            int ordinal,
            String name,
            String column,
            String columnType,
            boolean required,
            boolean unique,
            boolean plural,
            String elementType,
            List<String> enumeration,
            List<String> discriminator,
            List<String> foreignKeys,
            List<String> description) {
        @SuppressWarnings("rawtypes")
        static Field parse(final Entity parent, final int ordinal, final Map.Entry<String, Map> entry) {
            val map = entry.getValue();
            return new Field(SneakyRef.of(parent),
                    ordinal,
                    entry.getKey(),
                    (String)map.get("column"),
                    (String)map.get("column-type"),
                    (Boolean)map.get("required"),
                    (Boolean)map.get("unique"),
                    (boolean)Optional.ofNullable((Boolean)map.get("plural")).orElse(false),
                    (String)map.get("elementType"),
                    parseMultilineStringTrimmed((String)map.get("enum")),
                    parseMultilineStringTrimmed((String)map.get("discriminator")),
                    parseMultilineStringTrimmed((String)map.get("foreignKeys")),
                    parseMultilineString((String)map.get("description")));
        }
        public Entity parentEntity() {
            return parentRef.value();
        }
        public String fqColumnName() {
            return parentEntity().table().toUpperCase() + "." + column().toUpperCase();
        }
        public TypeName asJavaType() {
            return _TypeMapping.dbToJava(columnType(), !required);
        }
        public TypeName asJavaEnumType() {
            return ClassName.get("", _Strings.capitalize(name()));
        }
        public boolean hasElementType() {
            return _Strings.isNotEmpty(elementType);
        }
        /** fails if hasElementType()==false */
        public String elementTypeSimpleName() {
            _Assert.assertTrue(hasElementType());
            var cutter = TextUtils.cutter(elementType); // expected non-null
            return cutter.keepAfterLast(".").getValue();
        }
        /** fails if hasElementType()==false */
        public String elementTypeNamespace() {
            _Assert.assertTrue(hasElementType());
            var cutter = TextUtils.cutter(elementType); // expected non-null
            return cutter.keepBeforeLast(".").getValue();
        }
        public boolean isMemberOfSecondaryKey() {
            return parentEntity().secondaryKeyFields()
                    .contains(this);
        }
        public boolean isEnum() {
            return enumeration.size()>0;
        }
        public List<EnumConstant> enumConstants() {
            return _NullSafe.stream(enumeration)
                    .map(IndexedFunction.zeroBased((index, ev)->EnumConstant.parse(this, index, ev)))
                    .collect(Collectors.toList());
        }
        public boolean requiredInTheUi() {
            // when enum and the enum also represents null,
            // then Optionality.MANDATORY is enforced (regardless of any required=false)
            return required()
                    || (isEnum()
                            && enumConstants().stream().anyMatch(EnumConstant::isRepresentingNull));
        }
        public boolean hasDiscriminator() {
            return discriminator.size()>0;
        }
        public List<Field> discriminatorFields() {
            return _NullSafe.stream(discriminator)
                    .map(fieldId->parentEntity().fields()
                            .stream()
                            .filter(field->field.column().equalsIgnoreCase(fieldId))
                            .findAny()
                            .orElseThrow(()->_Exceptions
                                    .noSuchElement("secondary-key field not found by column name '%s' in %s",
                                            fieldId, parentEntity().key())))
                    .collect(Collectors.toList());
        }
        public boolean hasForeignKeys() {
            return foreignKeys.size()>0;
        }
        public boolean isBooleanPrimitive() {
            return asJavaType().equals(TypeName.BOOLEAN);
        }
        public String getter() {
            return (isBooleanPrimitive() ? "is" : "get")
                    + _Strings.capitalize(name());
        }
        public String setter() {
            return "set"
                    + _Strings.capitalize(name());
        }
        public int maxLength() {

            if(_TypeMapping.isMaxLengthSuppressedFor(columnType())) {
                return -1;
            }

            val lengthLiteralOrColumnType = TextUtils.cutter(columnType())
                .keepAfter("(")
                .keepBeforeLast(")")
                .getValue();

            final int parsedMaxLength = _Ints.parseInt(lengthLiteralOrColumnType, 10).orElse(-1);

            //H2 max
            if(parsedMaxLength>1000000000) {
                return 1000000000;
            }

            return parsedMaxLength;
        }
        public String formatDescription(final String continuation, final String ... moreLines) {
            var descriptionLines = (isMultilineStringBlank(description())
                    ? Can.of("has no description")
                    : description()
                        .stream()
                        .map(String::trim)
                        .collect(Can.toCan()));
            val more = _NullSafe.stream(moreLines)
                .map(String::trim)
                .collect(Can.toCan());
            descriptionLines = descriptionLines.addAll(more);
            return descriptionLines.stream()
                    .collect(Collectors.joining(continuation));
        }
        public String sequence() {
            return "" + (ordinal + 1);
        }
        public Can<Field> foreignFields() {
            final Schema schema = parentEntity().parentSchema();
            return foreignKeys().stream()
                    .map(schema::lookupForeignKeyFieldElseFail)
                    .collect(Can.toCan());
        }
        public void withRequired(final boolean required) {
            var copy = new Field(parentRef,
                    ordinal,
                    name,
                    column,
                    columnType,
                    required,
                    unique,
                    plural,
                    elementType,
                    enumeration,
                    discriminator,
                    foreignKeys,
                    description);
            parentEntity().fields().replaceAll(f->f.ordinal() == this.ordinal()
                    ? copy
                    : f);
        }
        public void withUnique(final boolean unique) {
            var copy = new Field(parentRef,
                    ordinal,
                    name,
                    column,
                    columnType,
                    required,
                    unique,
                    plural,
                    elementType,
                    enumeration,
                    discriminator,
                    foreignKeys,
                    description);
            parentEntity().fields().replaceAll(f->f.ordinal() == this.ordinal()
                    ? copy
                    : f);
        }
    }

    public record EnumConstant(
            SneakyRef<Field> parentRef,
            int ordinal,
            String name,
            /**
             * non-null: empty string also matches on null in the DB
             */
            String matchOn,
            String description) {
        static EnumConstant parse(final Field field, final int ordinal, final String enumDeclarationLine) {
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
        public Field parentField() {
            return parentRef.value();
        }
        public String asJavaName() {
            var preprocessed = name.replaceAll("[^a-zA-Z0-9_]", " ").trim();
            //debug
            //System.err.printf("preprocessed: '%s'->'%s'%n", name, preprocessed);
            return _Strings.condenseWhitespaces(preprocessed, "_").toUpperCase();
        }
        public boolean isRepresentingNull() {
            return _Strings.isNullOrEmpty(matchOn);
        }
    }

    /**
     * Entity metadata by {@code <namespace>.<name>}.
     */
    public record Schema(Map<String, Entity> entities) {
        public static Schema of(final Iterable<Entity> entities) {
            val schema = new Schema(new TreeMap<String, OrmModel.Entity>());
            for(val entity: entities) {
                schema.entities().put(entity.key(), entity);
            }
            return schema;
        }
        @SuppressWarnings({ "rawtypes", "unchecked" })
        public static Schema fromYaml(final String yaml) {
            val entities = new TreeMap<String, OrmModel.Entity>();
            YamlUtils.tryRead(Map.class, yaml)
            .ifFailureFail()
            .getValue()
            .map(map->(Map<String, Map>)map)
            .ifPresent(map->{
                map.entrySet().stream()
                .map(Entity::parse)
                .forEach(entity->entities.put(entity.key(), entity));
            });
            var schema = new Schema(entities);
            return schema;
        }
        public Schema(final Map<String, Entity> entities){
            this.entities = entities;
            entities.values().forEach(e->e.parentRef.setValue(this));
        }
        public String toYaml() {
            val sb = new StringBuilder();
            for(val entity : entities().values()) {
                sb.append(entity.toYaml());
            }
            return sb.toString();
        }
        @SneakyThrows
        public void writeToFileAsYaml(final File file, final LicenseHeader licenseHeader) {
            val lic = licenseHeaderAsYaml(licenseHeader);
            val yaml = TextUtils.readLines(toYaml());
            TextUtils.writeLinesToFile(lic.addAll(yaml), file, StandardCharsets.UTF_8);
        }
        public Optional<OrmModel.Entity> lookupEntityByTableName(final String tableName) {
            return entities().values()
                    .stream()
                    .filter(e->e.table().equalsIgnoreCase(tableName))
                    .findFirst();
        }
        public Can<Entity> findEntitiesWithoutRelations(){
            val foreignKeyFields = // as table.column literal
                    entities().values().stream().flatMap(fe->fe.fields().stream())
                        .flatMap(ff->ff.foreignKeys().stream())
                        .map(String::toLowerCase)
                        .collect(Collectors.toSet());
            val entitiesWithoutRelations =  entities().values().stream()
                .filter(e->!e.fields().stream().anyMatch(f->f.hasForeignKeys()))
                .filter(e->!e.fields().stream().anyMatch(f->
                    foreignKeyFields.contains(e.table().toLowerCase() + "." + f.column().toLowerCase())))
                .sorted((a, b)->_Strings.compareNullsFirst(a.name(), b.name()))
                .collect(Can.toCan());
            return entitiesWithoutRelations;
        }
        // -- UTILITY
        @SneakyThrows
        public static Schema fromYamlFolder(final File rootDirectory) {
            val root = FileUtils.existingDirectoryElseFail(rootDirectory);
            val sb = new StringBuilder();
            FileUtils.searchFiles(root, dir->true, file->file.getName().endsWith(".yaml"))
                .stream()
                .map(DataSource::ofFile)
                .forEach(ds->{
                    sb.append(ds.tryReadAsStringUtf8().valueAsNonNullElseFail()).append("\n\n");
                });
            return fromYaml(sb.toString());
        }
        public void splitIntoFiles(final File rootDirectory, final LicenseHeader licenseHeader) {
            val dir0 = FileUtils.makeDir(rootDirectory);
            val dir1 = FileUtils.existingDirectoryElseFail(dir0);
            entities().values().forEach(entity->{
                val destFile = new File(dir1, entity.name() + ".yaml");
                val lic = licenseHeaderAsYaml(licenseHeader);
                val yaml = TextUtils.readLines(entity.toYaml());
                TextUtils.writeLinesToFile(lic.addAll(yaml), destFile, StandardCharsets.UTF_8);
            });
        }
        public ObjectGraph asObjectGraph() {
            return new _ObjectGraphFactory(this).create();
        }
        // -- HELPER
        private Optional<OrmModel.Field> lookupForeignKeyField(final String tableDotColumn) {
            val parts = _Strings.splitThenStream(tableDotColumn, ".")
                    .collect(Can.toCan());
            _Assert.assertEquals(2, parts.size(), ()->String.format(
                    "could not parse foreign key '%s'", tableDotColumn));
            val tableName = parts.getElseFail(0);
            val columnName = parts.getElseFail(1);
            return lookupEntityByTableName(tableName)
                    .flatMap(entity->entity.lookupFieldByColumnName(columnName));
        }
        private OrmModel.Field lookupForeignKeyFieldElseFail(final String tableDotColumn) {
            return lookupForeignKeyField(tableDotColumn)
                    .orElseThrow(()->_Exceptions.noSuchElement("foreign key not found '%s'", tableDotColumn));
        }
        private Can<String> licenseHeaderAsYaml(final LicenseHeader licenseHeader){
            return Can.of("-----------------------------------------------------------")
                    .addAll(TextUtils.readLines(licenseHeader.text()))
                    .add("-----------------------------------------------------------")
                    .map(s->"# " + s)
                    .add("");
        }
    }

    /**
     * JUnit support.
     */
    public Can<Schema> examples() {
        val entity = new Entity(
                ObjectRef.empty(),
                "Customer", "causewaystuff", "FOODS", "", List.of(), false, "name", "fa-pencil",
                false,
                List.of("Customer List and Aliases"),
                new ArrayList<OrmModel.Field>());
        val field = new Field(SneakyRef.of(entity), /*ordinal*/0, "name", "NAME", "nvarchar(100)", true, false, false,
                "",
                List.of(), List.of(), List.of(), List.of("aa", "bb", "cc"));
        entity.fields().add(field);
        return Can.of(
                Schema.of(List.of(entity)));
    }

    // -- HELPER

    private static class YamlWriter {
        final StringBuilder sb = new StringBuilder();
        @Override public String toString() { return sb.toString(); }
        YamlWriter multilineStartIfNotEmtpy(final List<?> list) {
            if(!_NullSafe.isEmpty(list)) sb.append(" |");
            return this;
        }
        YamlWriter write(final String ...s) {
            for(val str:s) sb.append(str);
            return this;
        }
        YamlWriter writeUpper(final String ...s) {
            for(val str:s) sb.append(str.toUpperCase());
            return this;
        }
        YamlWriter ind() {
            sb.append("  ");
            return this;
        }
        YamlWriter nl() {
            sb.append('\n');
            return this;
        }
    }

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

    private boolean isMultilineStringBlank(final List<String> lines) {
        return _NullSafe.size(lines)==0
            ? true
            : _Strings.isNullOrEmpty(lines.stream().collect(Collectors.joining("")).trim());
    }


}
