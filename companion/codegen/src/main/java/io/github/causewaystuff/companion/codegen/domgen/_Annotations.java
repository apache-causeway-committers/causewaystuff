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
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import javax.annotation.processing.Generated;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;

import org.jspecify.annotations.Nullable;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import org.apache.causeway.applib.annotation.Action;
import org.apache.causeway.applib.annotation.ActionLayout;
import org.apache.causeway.applib.annotation.ActionLayout.Position;
import org.apache.causeway.applib.annotation.Collection;
import org.apache.causeway.applib.annotation.CollectionLayout;
import org.apache.causeway.applib.annotation.DomainObject;
import org.apache.causeway.applib.annotation.DomainObjectLayout;
import org.apache.causeway.applib.annotation.DomainService;
import org.apache.causeway.applib.annotation.Editing;
import org.apache.causeway.applib.annotation.LabelPosition;
import org.apache.causeway.applib.annotation.MemberSupport;
import org.apache.causeway.applib.annotation.Nature;
import org.apache.causeway.applib.annotation.Navigable;
import org.apache.causeway.applib.annotation.ObjectSupport;
import org.apache.causeway.applib.annotation.Optionality;
import org.apache.causeway.applib.annotation.Parameter;
import org.apache.causeway.applib.annotation.ParameterLayout;
import org.apache.causeway.applib.annotation.PrecedingParamsPolicy;
import org.apache.causeway.applib.annotation.Programmatic;
import org.apache.causeway.applib.annotation.Property;
import org.apache.causeway.applib.annotation.PropertyLayout;
import org.apache.causeway.applib.annotation.SemanticsOf;
import org.apache.causeway.applib.annotation.Snapshot;
import org.apache.causeway.applib.annotation.TableDecorator;
import org.apache.causeway.applib.annotation.Where;
import org.apache.causeway.commons.collections.Can;
import org.apache.causeway.commons.internal.base._Strings;
import org.apache.causeway.commons.internal.collections._Multimaps.ListMultimap;

import lombok.Builder;
import lombok.experimental.UtilityClass;

import io.github.causewaystuff.companion.codegen.model.PropertyLayoutSpec;
import io.micronaut.sourcegen.javapoet.AnnotationSpec;
import io.micronaut.sourcegen.javapoet.ClassName;
import io.micronaut.sourcegen.javapoet.CodeBlock;

@UtilityClass
class _Annotations {

    // -- JAVA

    AnnotationSpec override() {
        return AnnotationSpec.builder(ClassName.get(Override.class))
                .build();
    }

    // -- LOMBOK

    AnnotationSpec builder() {
        return AnnotationSpec.builder(ClassName.get("lombok", "Builder"))
                .build();
    }
    AnnotationSpec allArgsConstructor() {
        return AnnotationSpec.builder(ClassName.get("lombok", "AllArgsConstructor"))
                .build();
    }
    AnnotationSpec requiredArgsConstructor() {
        return AnnotationSpec.builder(ClassName.get("lombok", "RequiredArgsConstructor"))
                .build();
    }
    AnnotationSpec lombokValue() {
        return AnnotationSpec.builder(ClassName.get("lombok", "Value"))
                .build();
    }
    AnnotationSpec getter() {
        return AnnotationSpec.builder(ClassName.get("lombok", "Getter"))
                .build();
    }
    AnnotationSpec getterWithOverride() {
        return AnnotationSpec.builder(ClassName.get("lombok", "Getter"))
                .addMember("onMethod_", "{@Override}")
                .build();
    }
    AnnotationSpec setter() {
        return AnnotationSpec.builder(ClassName.get("lombok", "Setter"))
                .build();
    }
    AnnotationSpec accessorsFluent() {
        return AnnotationSpec.builder(ClassName.get("lombok.experimental", "Accessors"))
                .addMember("fluent", "true")
                .build();
    }

    // -- JAKARTA

    /**
     * @param logicalTypeName - logical type name (Apache Causeway semantics)
     */
    AnnotationSpec named(final String logicalTypeName) {
        return AnnotationSpec.builder(Named.class)
                .addMember("value", "$1S", logicalTypeName)
                .build();
    }
    AnnotationSpec inject() {
        return AnnotationSpec.builder(Inject.class)
                .build();
    }

    // -- JDK

    /**
     * Example:
     * <pre>
     * &#64;Generated(value="com.example.Generator")
     * </pre>
     */
    AnnotationSpec generated(final String byClassName) {
        return AnnotationSpec.builder(Generated.class)
                .addMember("value", "$1S", byClassName)
                .build();
    }
    /**
     * Example:
     * <pre>
     * &#64;Generated(value="com.example.Generator")
     * </pre>
     */
    AnnotationSpec generated(final Class<?> byClass) {
        return generated(byClass.getName());
    }

    // -- SPRING

    AnnotationSpec configuration() {
        return AnnotationSpec.builder(Configuration.class)
                .build();
    }
    AnnotationSpec imports(final ListMultimap<String, ClassName> importsByCategory) {

        var importEntries = new ArrayList<CodeBlock>();

        importsByCategory.entrySet().stream()
        .forEach(entry->{

            // category comment
            importEntries.add(CodeBlock.of("\n// $1L", entry.getKey()));

            entry.getValue().stream()
                .sorted((a, b)->a.simpleName().compareTo(b.simpleName()))
                .map(classToImport->CodeBlock.of("$1T.class,", classToImport))
                .forEach(importEntries::add);
        });

        return AnnotationSpec.builder(Import.class)
                .addMember("value", CodeBlock.join(List.of(
                        CodeBlock.of("{"),
                        CodeBlock.join(importEntries, "\n"),
                        CodeBlock.of("}")),
                        "\n"))
                .build();
    }

    // -- CAUSEWAY - DOMAIN OBJECT

    AnnotationSpec domainObject() {
        return AnnotationSpec.builder(DomainObject.class)
                .build();
    }
    AnnotationSpec domainObject(final @Nullable Nature nature) {
        var builder = AnnotationSpec.builder(DomainObject.class);
        Optional.ofNullable(nature)
            .ifPresent(__->builder.addMember("nature", "$1T.$2L", Nature.class, nature.name()));
        return builder
            .build();
    }
    AnnotationSpec domainService() {
        return AnnotationSpec.builder(DomainService.class)
                .build();
    }

    // -- CAUSEWAY - DOMAIN OBJECT LAYOUT

    @Builder
    static record DomainObjectLayoutSpec(
        String named,
        String describedAs,
        String cssClassFa) {
    }
    /**
     * @param describedAs - entity description
     * @param cssClassFa - entity icon (Font Awesome)
     */
    AnnotationSpec domainObjectLayout(
            final DomainObjectLayoutSpec domainObjectLayoutSpec) {
        var builder = AnnotationSpec.builder(DomainObjectLayout.class);
        _Strings.nonEmpty(domainObjectLayoutSpec.named)
            .ifPresent(__->builder.addMember("named", "$1S", domainObjectLayoutSpec.named));
        _Strings.nonEmpty(domainObjectLayoutSpec.describedAs)
            .ifPresent(__->builder.addMember("describedAs", "$1S", domainObjectLayoutSpec.describedAs));
        _Strings.nonEmpty(domainObjectLayoutSpec.cssClassFa)
            .ifPresent(__->builder.addMember("cssClassFa", "$1S", domainObjectLayoutSpec.cssClassFa));
        return builder.build();
    }

    // -- CAUSEWAY - ACTION

    @Builder
    static record ActionSpec(
        SemanticsOf semantics) {
    }
    AnnotationSpec action(final UnaryOperator<ActionSpec.ActionSpecBuilder> attrProvider) {
        var builder = AnnotationSpec.builder(Action.class);
        var attr = attrProvider.apply(ActionSpec.builder()).build();
        Optional.ofNullable(attr.semantics())
            .ifPresent(semantics->builder.addMember("semantics", "$1T.$2L", SemanticsOf.class, semantics.name()));
        return builder.build();
    }
    @Builder
    static record ActionLayoutSpec(
        String cssClassFa,
        String fieldSetId,
        String sequence,
        String describedAs,
        Position position) {
    }
    AnnotationSpec actionLayout(final UnaryOperator<ActionLayoutSpec.ActionLayoutSpecBuilder> attrProvider) {
        var builder = AnnotationSpec.builder(ActionLayout.class);
        var attr = attrProvider.apply(ActionLayoutSpec.builder()).build();
        _Strings.nonEmpty(attr.cssClassFa())
            .ifPresent(cssClassFa->builder.addMember("cssClassFa", "$1S", cssClassFa));
        _Strings.nonEmpty(attr.fieldSetId())
            .ifPresent(fieldSetId->builder.addMember("fieldSetId", "$1S", fieldSetId));
        _Strings.nonEmpty(attr.sequence())
            .ifPresent(sequence->builder.addMember("sequence", "$1S", sequence));
        _Strings.nonEmpty(attr.describedAs())
            .ifPresent(describedAs->builder.addMember("describedAs", "$1S", describedAs));
        Optional.ofNullable(attr.position())
            .ifPresent(position->builder.addMember("position", "$1T.$2L", Position.class, position.name()));
        return builder.build();
    }

    // -- CAUSEWAY - PROPERTY

    @Builder
    static record PropertySpec(
        Optionality optionality,
        Editing editing,
        Snapshot snapshot) {
    }
    AnnotationSpec property(final UnaryOperator<PropertySpec.PropertySpecBuilder> attrProvider) {
        var builder = AnnotationSpec.builder(Property.class);
        var attr = attrProvider.apply(PropertySpec.builder()).build();
        Optional.ofNullable(attr.optionality())
            .ifPresent(optionality->builder.addMember("optionality", "$1T.$2L", Optionality.class, optionality.name()));
        Optional.ofNullable(attr.editing())
            .ifPresent(editing->builder.addMember("editing", "$1T.$2L", Editing.class, editing.name()));
        Optional.ofNullable(attr.snapshot())
            .ifPresent(snapshot->builder.addMember("snapshot", "$1T.$2L", Snapshot.class, snapshot.name()));
        return builder.build();
    }

    AnnotationSpec propertyLayout(final PropertyLayoutSpec attr) {
        var builder = AnnotationSpec.builder(PropertyLayout.class);
        _Strings.nonEmpty(attr.named())
            .ifPresent(named->builder.addMember("named", "$1S", named));
        _Strings.nonEmpty(attr.cssClass())
            .ifPresent(cssClass->builder.addMember("cssClass", "$1S", cssClass));
        _Strings.nonEmpty(attr.fieldSet())
            .ifPresent(fieldSetId->builder.addMember("fieldSetId", "$1S", fieldSetId));
        _Strings.nonEmpty(attr.sequence())
            .ifPresent(sequence->builder.addMember("sequence", "$1S", sequence));
        _Strings.nonEmpty(attr.describedAs())
            .ifPresent(describedAs->builder.addMember("describedAs", "$1S", describedAs));
        Optional.ofNullable(attr.hiddenWhere())
            .ifPresent(hiddenWhere->builder.addMember("hidden", "$1T.$2L", Where.class, hiddenWhere.name()));
        if(attr.multiLine()!=null
                && attr.multiLine()>1) {
            builder.addMember("multiLine", "$1L", attr.multiLine());
        }
        Optional.ofNullable(attr.navigable())
            .ifPresent(navigable->builder.addMember("navigable", "$1T.$2L", Navigable.class, navigable.name()));
        Optional.ofNullable(attr.labelPosition())
            .ifPresent(labelPosition->builder.addMember("labelPosition", "$1T.$2L", LabelPosition.class, labelPosition.name()));
        return builder.build();
    }

    AnnotationSpec propertyLayout(
            final UnaryOperator<PropertyLayoutSpec.PropertyLayoutSpecBuilder> primer,
            final PropertyLayoutSpec override) {
        var attr = primer.apply(PropertyLayoutSpec.builder()).build();
        return propertyLayout(attr.overrideWith(override));
    }

    // -- CAUSEWAY - COLLECTION

    @Builder
    static record CollectionSpec(
            Class<?> typeOf) {
    }
    AnnotationSpec collection(final UnaryOperator<CollectionSpec.CollectionSpecBuilder> attrProvider) {
        var builder = AnnotationSpec.builder(Collection.class);
        var attr = attrProvider.apply(CollectionSpec.builder()).build();
        Optional.ofNullable(attr.typeOf())
            .ifPresent(typeOf->builder.addMember("typeOf", "$1T.class", ClassName.get(typeOf)));
        return builder.build();
    }
    @Builder
    static record CollectionLayoutSpec(
            String describedAs,
            Where hiddenWhere,
            Class<? extends TableDecorator> tableDecorator) {
    }
    AnnotationSpec collectionLayout(final UnaryOperator<CollectionLayoutSpec.CollectionLayoutSpecBuilder> attrProvider) {
        var builder = AnnotationSpec.builder(CollectionLayout.class);
        var attr = attrProvider.apply(CollectionLayoutSpec.builder()).build();
        _Strings.nonEmpty(attr.describedAs())
            .ifPresent(describedAs->builder.addMember("describedAs", "$1S", describedAs));
        Optional.ofNullable(attr.hiddenWhere())
            .ifPresent(hiddenWhere->builder.addMember("hidden", "$1T.$2L", Where.class, hiddenWhere.name()));
        Optional.ofNullable(attr.tableDecorator())
            .ifPresent(tableDecorator->builder.addMember("tableDecorator", "$1T.class", tableDecorator));
        return builder.build();
    }

    // -- CAUSEWAY - PARAM

    @Builder
    static record ParameterSpec(
            PrecedingParamsPolicy precedingParamsPolicy,
            Optionality optionality) {
    }
    AnnotationSpec parameter(final UnaryOperator<ParameterSpec.ParameterSpecBuilder> attrProvider) {
        var builder = AnnotationSpec.builder(Parameter.class);
        var attr = attrProvider.apply(ParameterSpec.builder()).build();
        Optional.ofNullable(attr.precedingParamsPolicy())
            .ifPresent(precedingParamsPolicy->builder.addMember(
                    "precedingParamsPolicy", "$1T.$2L",
                    PrecedingParamsPolicy.class, precedingParamsPolicy.name()));
        Optional.ofNullable(attr.optionality())
            .ifPresent(optionality->builder.addMember(
                    "optionality", "$1T.$2L",
                    Optionality.class, optionality.name()));
        return builder.build();
    }
    @Builder
    static record ParameterLayoutSpec(
        String describedAs,
        LabelPosition labelPosition,
        int multiLine) {
    }
    AnnotationSpec parameterLayout(final UnaryOperator<ParameterLayoutSpec.ParameterLayoutSpecBuilder> attrProvider) {
        var builder = AnnotationSpec.builder(ParameterLayout.class);
        var attr = attrProvider.apply(ParameterLayoutSpec.builder()).build();
        _Strings.nonEmpty(attr.describedAs())
            .ifPresent(describedAs->builder.addMember("describedAs", "$1S", describedAs));
        Optional.ofNullable(attr.labelPosition())
            .ifPresent(labelPosition->builder.addMember("labelPosition", "$1T.$2L", LabelPosition.class, labelPosition.name()));
        if(attr.multiLine()>1) {
            builder.addMember("multiLine", "$1L", attr.multiLine());
        }
        return builder.build();
    }

    // -- CAUSEWAY - SUPPORT

    AnnotationSpec memberSupport() {
        return AnnotationSpec.builder(MemberSupport.class)
                .build();
    }

    AnnotationSpec objectSupport() {
        return AnnotationSpec.builder(ObjectSupport.class)
                .build();
    }

    AnnotationSpec programmatic() {
        return AnnotationSpec.builder(Programmatic.class)
                .build();
    }

    // -- JPA

    AnnotationSpec jpaEntity() {
        return AnnotationSpec.builder(Entity.class)
                .build();
    }

    AnnotationSpec jpaTransient() {
        return AnnotationSpec.builder(Transient.class)
                .build();
    }

    @Builder
    static record JpaTableSpec(
        /**
         * (Optional) The name of the table.
         * <p> Defaults to the entity name.
         */
        String tableName, // default "";

        /** (Optional) The catalog of the table.
         * <p> Defaults to the default catalog.
         */
        String catalog, // default "";

        /** (Optional) The schema of the table.
         * <p> Defaults to the default schema for user.
         */
        String schema, // default "";

        /**
         * (Optional) Unique constraints that are to be placed on
         * the table. These are only used if table generation is in
         * effect. These constraints apply in addition to any constraints
         * specified by the <code>Column</code> and <code>JoinColumn</code>
         * annotations and constraints entailed by primary key mappings.
         * <p> Defaults to no additional constraints.
         */
        UniqueConstraint[] uniqueConstraints, // default {};

        /**
         * (Optional) Indexes for the table.  These are only used if
         * table generation is in effect.  Note that it is not necessary
         * to specify an index for a primary key, as the primary key
         * index will be created automatically.
         */
        Index[] indexes // default {};
        ) {
    }
    AnnotationSpec jpaTable(final UnaryOperator<JpaTableSpec.JpaTableSpecBuilder> attrProvider) {
        var annotBuilder = AnnotationSpec.builder(Table.class);
        var attr = attrProvider.apply(JpaTableSpec.builder()).build();
        _Strings.nonEmpty(_Strings.trim(attr.tableName))
            .ifPresent(name->annotBuilder.addMember("name", "$1S", name));
        _Strings.nonEmpty(_Strings.trim(attr.tableName))
            .ifPresent(catalog->annotBuilder.addMember("catalog", "$1S", catalog));
        _Strings.nonEmpty(_Strings.trim(attr.tableName))
            .ifPresent(schema->annotBuilder.addMember("schema", "$1S", schema));
        return annotBuilder.build();
    }

    @Builder
    static record JpaColumnSpec(
            /**
             * (Optional) The name of the column. Defaults to
             * the property or field name.
             */
            String columnName,

            /**
             * (Optional) Whether the column is a unique key.  This is a
             * shortcut for the <code>UniqueConstraint</code> annotation at the table
             * level and is useful for when the unique key constraint
             * corresponds to only a single column. This constraint applies
             * in addition to any constraint entailed by primary key mapping and
             * to constraints specified at the table level.
             */
            boolean unique, // default false;

            /**
             * (Optional) Whether the database column is nullable.
             */
            boolean nullable, // default true;

            /**
             * (Optional) Whether the column is included in SQL INSERT
             * statements generated by the persistence provider.
             */
            boolean insertable, // default true;

            /**
             * (Optional) Whether the column is included in SQL UPDATE
             * statements generated by the persistence provider.
             */
            boolean updatable, // default true;

            /**
             * (Optional) The SQL fragment that is used when
             * generating the DDL for the column.
             * <p> Defaults to the generated SQL to create a
             * column of the inferred type.
             */
            String columnDefinition, // default "";

            /**
             * (Optional) The name of the table that contains the column.
             * If absent the column is assumed to be in the primary table.
             */
            String table, // default "";

            /**
             * (Optional) The column length. (Applies only if a
             * string-valued column is used.)
             */
            int length, // default 255;

            /**
             * (Optional) The precision for a decimal (exact numeric)
             * column. (Applies only if a decimal column is used.)
             * Value must be set by developer if used when generating
             * the DDL for the column.
             */
            int precision, // default 0;

            /**
             * (Optional) The scale for a decimal (exact numeric) column.
             * (Applies only if a decimal column is used.)
             */
            int scale // default 0;
            ) {
    }
    AnnotationSpec jpaColumn(final UnaryOperator<JpaColumnSpec.JpaColumnSpecBuilder> attrProvider) {
        var annotBuilder = AnnotationSpec.builder(Column.class);
        var attr = attrProvider.apply(JpaColumnSpec.builder()).build();
        _Strings.nonEmpty(_Strings.trim(attr.columnName))
            .ifPresent(name->annotBuilder.addMember("name", "$1S", name));
        annotBuilder.addMember("nullable", "$1L", "" + attr.nullable);
        if(attr.length>0) {
            annotBuilder.addMember("length", "$1L", Math.min(attr.length, 1024*4)); // upper bound = 4k
        }
        if(attr.columnDefinition!=null) {
            annotBuilder.addMember("columnDefinition", "$1S", attr.columnDefinition);
        }
        return annotBuilder.build();
    }

    // -- JDO

    AnnotationSpec jdoNotPersistent() {
        return AnnotationSpec.builder(ClassName.get("javax.jdo.annotations", "NotPersistent"))
                .build();
    }

    AnnotationSpec jdoPersistenceCapable() {
        return AnnotationSpec.builder(ClassName.get("javax.jdo.annotations", "PersistenceCapable"))
                .build();
    }
    AnnotationSpec jdoPersistenceCapable(final String tableName) {
        return AnnotationSpec.builder(ClassName.get("javax.jdo.annotations", "PersistenceCapable"))
                .addMember("table", "$1S", tableName)
                .build();
    }
    AnnotationSpec jdoDatastoreIdentity() {
        return AnnotationSpec.builder(ClassName.get("javax.jdo.annotations", "DatastoreIdentity"))
                .addMember("strategy", "$1L", "javax.jdo.annotations.IdGeneratorStrategy.IDENTITY")
                .addMember("column", "$1S", "id")
                .build();
    }

    @Builder
    static record JdoColumnSpec(
            /** name of the db column, if null or empty uses default name */
            String columnName,
            /** whether null is allowed as database value for this column */
            boolean allowsNull,
            /** ignored if less than one */
            int maxLength,
            String jdbcType) {
    }
    AnnotationSpec jdoColumn(final UnaryOperator<JdoColumnSpec.JdoColumnSpecBuilder> attrProvider) {
        var annotBuilder = AnnotationSpec.builder(ClassName.get("javax.jdo.annotations", "Column"));
        var attr = attrProvider.apply(JdoColumnSpec.builder()).build();
        _Strings.nonEmpty(_Strings.trim(attr.columnName))
            .ifPresent(name->annotBuilder.addMember("name", "$1S", name));
        annotBuilder.addMember("allowsNull", "$1S", "" + attr.allowsNull);
        if(attr.maxLength>0) {
            annotBuilder.addMember("length", "$1L", Math.min(attr.maxLength, 1024*4)); // upper bound = 4k
        }
        if(attr.jdbcType!=null) {
            annotBuilder.addMember("jdbcType", "$1S", attr.jdbcType);
        }
        return annotBuilder.build();
    }

    /**
     * <pre>{@code @Unique(name="MY_COMPOSITE_IDX", members={"field1", "field2"})}</pre>
     */
    AnnotationSpec jdoUnique(final String name, final Can<String> members) {
        return AnnotationSpec.builder(ClassName.get("javax.jdo.annotations", "Unique"))
            .addMember("name", "$1S", name)
            .addMember("members", "{$1L}", members
                    .stream()
                    .map(fieldName->String.format("\"%s\"", fieldName)) // double quote
                    .collect(Collectors.joining(", ")))
            .build();
    }
    /**
     * <pre>{@code @Unique(name="MY_COMPOSITE_IDX", members={"field1", "field2"})}</pre>
     */
    AnnotationSpec jdoUunique(final String name, final String ...members) {
        return jdoUnique(name, Can.ofArray(members));
    }

    /**
     * <pre>{@code @Extension(vendorName="datanucleus", key="datastore", value="store2")}</pre>
     */
    AnnotationSpec datanucleusDatastore(final String datastore) {
        return AnnotationSpec.builder(ClassName.get("javax.jdo.annotations", "Extension"))
            .addMember("vendorName", "$1S", "datanucleus")
            .addMember("key", "$1S", "datastore")
            .addMember("value", "$1S", datastore)
            .build();
    }

    /**
     * <pre>{@code @Extension(vendorName="datanucleus", key="enum-value-getter", value="getValue")}</pre>
     */
    AnnotationSpec datanucleusEnumValueGetter(final String enumValueGetter) {
        return AnnotationSpec.builder(ClassName.get("javax.jdo.annotations", "Extension"))
            .addMember("vendorName", "$1S", "datanucleus")
            .addMember("key", "$1S", "enum-value-getter")
            .addMember("value", "$1S", enumValueGetter)
            .build();
    }

    /**
     * <pre>{@code @Extension(vendorName="datanucleus", key="enum-check-constraint", value="true")}</pre>
     */
    AnnotationSpec datanucleusCheckEnumConstraint(final boolean checkEnumConstraint) {
        return AnnotationSpec.builder(ClassName.get("javax.jdo.annotations", "Extension"))
            .addMember("vendorName", "$1S", "datanucleus")
            .addMember("key", "$1S", "enum-check-constraint")
            .addMember("value", "$1S", checkEnumConstraint)
            .build();
    }

}
