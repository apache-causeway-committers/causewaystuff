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
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

import lombok.NonNull;

import lombok.experimental.UtilityClass;

import io.github.causewaystuff.commons.base.types.internal.ObjectRef;
import io.github.causewaystuff.commons.base.types.internal.SneakyRef;
import io.github.causewaystuff.companion.codegen.model._Parser.ParserHint;
import io.github.causewaystuff.companion.schema.LicenseHeader;
import io.github.causewaystuff.tooling.javapoet.ClassName;
import io.github.causewaystuff.tooling.javapoet.TypeName;

/**
 * Read and write schema model from and to YAML format.
 */
@UtilityClass
public class Schema {

    public sealed interface DomainObject permits Viewmodel, Entity {
        String name();
        String namespace();
        Multiline description();
        default String fqn() {
            return String.format("%s.%s", namespace(), name());
        }
    }

    public sealed interface Field permits VmField, EntityField {
        int ordinal();
        String name();
        boolean required();
        List<String> enumeration();
        PropertyLayoutSpec propertyLayout();

        TypeName asJavaType();

        default boolean isEnum() {
            return enumeration().size()>0;
        }
        default TypeName asJavaEnumType() {
            return ClassName.get("", _Strings.capitalize(name()));
        }
        default String sequence() {
            return "" + (ordinal() + 1);
        }
        default List<EnumConstant> enumConstants() {
            return _NullSafe.stream(enumeration())
                    .map(IndexedFunction.zeroBased((index, ev)->EnumConstant.parse(this, index, ev)))
                    .collect(Collectors.toList());
        }
        default boolean requiredInTheUi() {
            // when enum and the enum also represents null,
            // then Optionality.MANDATORY is enforced (regardless of any required=false)
            return required()
                    || (isEnum()
                    && enumConstants().stream().anyMatch(EnumConstant::isRepresentingNull));
        }
    }

    public record Viewmodel(
            ObjectRef<Domain> parentRef,
            String generator,
            String name,
            String namespace,
            String title,
            String icon,
            boolean iconService,
            /**
             * as named in the UI, not in code
             */
            String named,
            Multiline description,
            List<VmField> fields) implements DomainObject {
        public Domain parentSchema() {
            return parentRef.getValue();
        }
    }

    public record VmField(
            SneakyRef<Viewmodel> parentRef,
            int ordinal,
            String name,
            @NonNull String type,
            boolean required,
            boolean plural,
            String elementType,
            PropertyLayoutSpec propertyLayout,
            List<String> enumeration) implements Field {
        @Override
        public TypeName asJavaType() {
            return _TypeMapping.simpleNameToJava(type);
        }
    }

    public record Entity(
            ObjectRef<Domain> parentRef,
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
            /**
             * as named in the UI, not in code
             */
            String named,
            Multiline description,
            List<EntityField> fields)  implements DomainObject {
        public Domain parentSchema() {
            return parentRef.getValue();
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
        public List<EntityField> secondaryKeyFields() {
            return _NullSafe.stream(secondaryKey)
                    .map(fieldId->fields()
                            .stream()
                            .filter(field->field.column().equalsIgnoreCase(fieldId))
                            .findAny()
                            .orElseThrow(()->_Exceptions
                                    .noSuchElement("secondary-key field not found by column name '%s' in %s",
                                            fieldId, fqn())))
                    .collect(Collectors.toList());
        }
        public Optional<Schema.EntityField> lookupFieldByColumnName(final String columnName) {
            return fields().stream()
                    .filter(f->f.column().equalsIgnoreCase(columnName))
                    .findFirst();
        }
        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (!(o instanceof Entity)) return false;
            return this.fqn().equals(((Entity) o).fqn());
        }
        @Override
        public int hashCode() {
            return fqn().hashCode();
        }
        // -- YAML IO
        static Entity parse(@SuppressWarnings("rawtypes") final Map.Entry<String, Map> entry) {
            return _Parser.parseEntity(entry, ParserHint.empty());
        }
        String toYaml() {
            return _Writer.toYaml(this);
        }
    }

    public record EntityField(
            SneakyRef<Entity> parentRef,
            int ordinal,
            String name,
            String column,
            String columnType,
            boolean required,
            boolean unique,
            boolean plural,
            String elementType,
            PropertyLayoutSpec propertyLayout,
            List<String> enumeration,
            List<String> discriminator,
            List<String> foreignKeys) implements Field {
        public Entity parentEntity() {
            return parentRef.value();
        }
        public String fqColumnName() {
            return parentEntity().table().toUpperCase() + "." + column().toUpperCase();
        }
        @Override
        public TypeName asJavaType() {
            return _TypeMapping.dbToJava(columnType(), !required);
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
        public boolean hasDiscriminator() {
            return discriminator.size()>0;
        }
        public List<EntityField> discriminatorFields() {
            return _NullSafe.stream(discriminator)
                .map(fieldId->parentEntity().fields()
                        .stream()
                        .filter(field->field.column().equalsIgnoreCase(fieldId))
                        .findAny()
                        .orElseThrow(()->_Exceptions
                                .noSuchElement("secondary-key field not found by column name '%s' in %s",
                                        fieldId, parentEntity().fqn())))
                .collect(Collectors.toList());
        }
        public boolean hasForeignKeys() {
            return foreignKeys.size()>0;
        }
        public Can<EntityField> foreignFields() {
            return _Foreign.foreignFields(this);
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
            var lengthLiteralOrColumnType = TextUtils.cutter(columnType())
                .keepAfter("(")
                .keepBeforeLast(")")
                .getValue();
            final int parsedMaxLength = _Ints.parseInt(lengthLiteralOrColumnType, 10).orElse(-1);
            //H2 max
            if(parsedMaxLength>1000000000) return 1000000000;
            return parsedMaxLength;
        }
        public void withRequired(final boolean required) {
            var copy = new EntityField(parentRef,
                    ordinal,
                    name,
                    column,
                    columnType,
                    required,
                    unique,
                    plural,
                    elementType,
                    propertyLayout,
                    enumeration,
                    discriminator,
                    foreignKeys);
            parentEntity().fields().replaceAll(f->f.ordinal() == this.ordinal()
                    ? copy
                    : f);
        }
        public void withUnique(final boolean unique) {
            var copy = new EntityField(parentRef,
                    ordinal,
                    name,
                    column,
                    columnType,
                    required,
                    unique,
                    plural,
                    elementType,
                    propertyLayout,
                    enumeration,
                    discriminator,
                    foreignKeys);
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
            return _Parser.parseEnum(field, ordinal, enumDeclarationLine);
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

    public record Domain(
            /**
             * Viewmodel metadata by {@code <namespace>.<name>}.
             */
            Map<String, Viewmodel> viewmodels,
            /**
             * Entity metadata by {@code <namespace>.<name>}.
             */
            Map<String, Entity> entities) {
        public static Domain of(
                final Iterable<Entity> viewmodels,
                final Iterable<Entity> entities) {
            var schema = new Domain(
                    new TreeMap<String, Schema.Viewmodel>(),
                    new TreeMap<String, Schema.Entity>());
            for(var vm: viewmodels) {
                schema.entities().put(vm.fqn(), vm);
            }
            for(var entity: entities) {
                schema.entities().put(entity.fqn(), entity);
            }
            return schema;
        }
        public static Domain of(
                final @Nullable Stream<Viewmodel> viewmodels,
                final @Nullable Stream<Entity> entities) {
            var schema = new Domain(
                    new TreeMap<String, Schema.Viewmodel>(),
                    new TreeMap<String, Schema.Entity>());
            if(entities!=null) {
                entities.forEach(entity->schema.entities().put(entity.fqn(), entity));
            }
            if(entities!=null) {
                viewmodels.forEach(vm->schema.viewmodels().put(vm.fqn(), vm));
            }
            return schema;
        }
        public Domain(
                final Map<String, Viewmodel> viewmodels,
                final Map<String, Entity> entities){
            this.viewmodels = viewmodels;
            this.entities = entities;
            entities.values().forEach(e->e.parentRef.setValue(this));
        }
        public Optional<Schema.Entity> lookupEntityByTableName(final String tableName) {
            return entities().values()
                    .stream()
                    .filter(e->e.table().equalsIgnoreCase(tableName))
                    .findFirst();
        }
        public Domain concat(final Domain other) {
            return Domain.of(
                    Stream.concat(
                        this.viewmodels().values().stream(),
                        other.viewmodels().values().stream()),
                    Stream.concat(
                        this.entities().values().stream(),
                        other.entities().values().stream()));
        }
        public ObjectGraph asObjectGraph() {
            return new _ObjectGraphFactory(this).create();
        }
        // -- YAML IO
        public static Domain fromYaml(final String yaml) {
            return _Parser.parseSchema(yaml);
        }
        public static Domain fromYamlFolder(final File rootDirectory) {
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
        // -- UTIL
        public Can<Entity> findEntitiesWithoutRelations() {
            return _Foreign.findEntitiesWithoutRelations(this);
        }
    }

}
