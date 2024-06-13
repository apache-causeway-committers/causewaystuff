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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.lang.Nullable;

import org.apache.causeway.applib.services.metamodel.objgraph.ObjectGraph;
import org.apache.causeway.commons.collections.Can;
import org.apache.causeway.commons.functional.IndexedFunction;
import org.apache.causeway.commons.internal.assertions._Assert;
import org.apache.causeway.commons.internal.base._NullSafe;
import org.apache.causeway.commons.internal.base._Strings;
import org.apache.causeway.commons.internal.exceptions._Exceptions;
import org.apache.causeway.commons.internal.primitives._Ints;
import org.apache.causeway.commons.io.TextUtils;

import lombok.val;
import lombok.experimental.UtilityClass;

import io.github.causewaystuff.commons.base.types.internal.ObjectRef;
import io.github.causewaystuff.commons.base.types.internal.SneakyRef;
import io.github.causewaystuff.companion.codegen.domgen.LicenseHeader;
import io.github.causewaystuff.tooling.javapoet.ClassName;
import io.github.causewaystuff.tooling.javapoet.TypeName;

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
        public String formatDescription(final String continuation) {
            if(isMultilineStringBlank(description)) return "has no description";
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
            if (this == o) return true;
            if (!(o instanceof Entity)) return false;
            return this.key().equals(((Entity) o).key());
        }
        @Override
        public int hashCode() {
            return key().hashCode();
        }
        // -- YAML IO
        static Entity parse(@SuppressWarnings("rawtypes") final Map.Entry<String, Map> entry) {
            return _Parser.parseEntity(entry);
        }
        String toYaml() {
            return _Writer.toYaml(this);
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
            OptionalInt multiLine,
            String elementType,
            List<String> enumeration,
            List<String> discriminator,
            List<String> foreignKeys,
            List<String> description) {
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
            if(_TypeMapping.isMaxLengthSuppressedFor(columnType())) return -1;
            val lengthLiteralOrColumnType = TextUtils.cutter(columnType())
                .keepAfter("(")
                .keepBeforeLast(")")
                .getValue();
            final int parsedMaxLength = _Ints.parseInt(lengthLiteralOrColumnType, 10).orElse(-1);
            //H2 max
            if(parsedMaxLength>1000000000) return 1000000000;
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
                    multiLine,
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
                    multiLine,
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
        public static Schema of(final @Nullable Stream<Entity> entities) {
            val schema = new Schema(new TreeMap<String, OrmModel.Entity>());
            if(entities!=null) {
                entities.forEach(entity->schema.entities().put(entity.key(), entity));
            }
            return schema;
        }
        public Schema(final Map<String, Entity> entities){
            this.entities = entities;
            entities.values().forEach(e->e.parentRef.setValue(this));
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
        // -- YAML IO
        public static Schema fromYaml(final String yaml) {
            return _Parser.parseSchema(yaml);
        }
        public static Schema fromYamlFolder(final File rootDirectory) {
            return fromYaml(_FileUtils.collectSchemaFromFolder(rootDirectory));
        }
        public String toYaml() {
            return _Writer.toYaml(this);
        }
        public void writeToFileAsYaml(final File file, final LicenseHeader licenseHeader) {
            _FileUtils.writeSchemaToFile(this, file, licenseHeader);
        }
        public void writeEntitiesToIndividualFiles(final File rootDirectory, final LicenseHeader licenseHeader) {
            _FileUtils.writeEntitiesToIndividualFiles(entities().values(), rootDirectory, licenseHeader);
        }
        // -- UTILITY
        public ObjectGraph asObjectGraph() {
            return new _ObjectGraphFactory(this).create();
        }
        public Schema concat(final Schema other) {
            return Schema.of(Stream.concat(
                    this.entities().values().stream(),
                    other.entities().values().stream()));
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
        val field = new Field(SneakyRef.of(entity), /*ordinal*/0, "name", "NAME", "nvarchar(100)",
                true, false, false,
                OptionalInt.of(2),
                "",
                List.of(), List.of(), List.of(), List.of("aa", "bb", "cc"));
        entity.fields().add(field);
        return Can.of(
                Schema.of(List.of(entity)));
    }

    // -- HELPER

    private boolean isMultilineStringBlank(final List<String> lines) {
        return _NullSafe.size(lines)==0
            ? true
            : _Strings.isNullOrEmpty(lines.stream().collect(Collectors.joining("")).trim());
    }

}
