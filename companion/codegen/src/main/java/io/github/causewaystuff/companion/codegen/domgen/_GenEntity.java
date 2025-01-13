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

import java.util.List;
import java.util.stream.Collectors;

import javax.lang.model.element.Modifier;

import org.apache.causeway.applib.annotation.Editing;
import org.apache.causeway.applib.annotation.Optionality;
import org.apache.causeway.applib.annotation.Where;
import org.apache.causeway.applib.services.repository.RepositoryService;
import org.apache.causeway.commons.collections.Can;
import org.apache.causeway.commons.internal.base._Strings;

import lombok.experimental.UtilityClass;

import io.github.causewaystuff.companion.applib.services.iconfa.IconFaService;
import io.github.causewaystuff.companion.applib.services.lookup.HasSecondaryKey;
import io.github.causewaystuff.companion.applib.services.search.SearchService;
import io.github.causewaystuff.companion.codegen.domgen.DomainGenerator.QualifiedType;
import io.github.causewaystuff.companion.codegen.domgen._Annotations.DomainObjectLayoutSpec;
import io.github.causewaystuff.companion.codegen.model.Schema;
import io.github.causewaystuff.companion.codegen.model.Schema.Entity;
import io.github.causewaystuff.companion.codegen.model.Schema.EntityField;
import io.github.causewaystuff.tooling.javapoet.ClassName;
import io.github.causewaystuff.tooling.javapoet.CodeBlock;
import io.github.causewaystuff.tooling.javapoet.FieldSpec;
import io.github.causewaystuff.tooling.javapoet.MethodSpec;
import io.github.causewaystuff.tooling.javapoet.ParameterizedTypeName;
import io.github.causewaystuff.tooling.javapoet.TypeSpec;

@UtilityClass
class _GenEntity {

    public QualifiedType qualifiedType(
            final DomainGenerator.Config config,
            final Schema.Entity entityModel) {

        var typeModelBuilder = TypeSpec.classBuilder(entityModel.name())
                .addJavadoc(entityModel.description().describedAs())
                .addAnnotation(_Annotations.generated(_GenEntity.class))
                .addAnnotation(_Annotations.named(config.fullLogicalName(entityModel.namespace()) + "." + entityModel.name()))
                .addAnnotation(_Annotations.domainObject())
                .addAnnotation(_Annotations.domainObjectLayout(
                        DomainObjectLayoutSpec.builder()
                            .named(entityModel.named())
                            .describedAs(entityModel.description().describedAs())
                            .cssClassFa(entityModel.icon())
                            .build()
                        ));

        switch (config.persistence()) {
            case JPA -> typeModelBuilder
                .addAnnotation(_Annotations.jpaEntity())
                .addAnnotation(_Annotations.jpaTable(attr->attr.tableName(entityModel.table())));
            case JDO -> typeModelBuilder
                .addAnnotation(_Annotations.jdoPersistenceCapable(entityModel.table()))
                .addAnnotation(_Annotations.jdoDatastoreIdentity());
            case NONE -> {}
        }

        typeModelBuilder
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ParameterizedTypeName.get(
                        ClassName.get(io.github.causewaystuff.companion.applib.services.lookup.Cloneable.class),
                        ClassName.get("", entityModel.name())))
                .addField(_Fields.inject(RepositoryService.class, "repositoryService"))
                .addField(_Fields.inject(SearchService.class, "searchService"))
                .addMethod(asTitleMethod(entityModel, Modifier.PUBLIC))
                .addMethod(asToStringMethod(entityModel))
                .addMethod(asCopyMethod(entityModel))
                .addMethod(_Methods.navigableParent(config.persistence(), entityModel.name()))
                .addFields(asFields(config.persistence(), entityModel.fields(), Modifier.PRIVATE))
                ;

        if(entityModel.iconService()) {
            typeModelBuilder
                .addField(_Fields.inject(IconFaService.class, "iconFaService"))
                .addMethod(_Methods.iconFaLayers(Modifier.PUBLIC));
        }

        // data federation support
        if(_Strings.isNotEmpty(config.datastore())) {
            typeModelBuilder.addAnnotation(_Annotations.datanucleusDatastore(config.datastore()));
        }

        // super type

        if(entityModel.hasSuperType()) {
            var packageName = config.fullPackageName(entityModel.superTypeNamespace());
            var superTypeName = ClassName.get(packageName, entityModel.superTypeSimpleName());
            typeModelBuilder.addSuperinterface(superTypeName);
        }

        // inner enums

        entityModel.fields().stream()
                .filter(Schema.EntityField::isEnum)
                .forEach(field->
                    typeModelBuilder.addType(
                            _Enums.enumForColumn(field.asJavaType(), field.enumConstants())));

        // inner manager view model

        var managerViewmodel = _GenEntity_Manager.generate(config, entityModel);
        typeModelBuilder.addType(managerViewmodel);

        if(entityModel.hasSecondaryKey()) {

            if(!entityModel.suppressUniqueConstraint()) {

                switch (config.persistence()) {
                    case JDO -> typeModelBuilder.addAnnotation(_Annotations.jdoUnique(
                            String.format("SEC_KEY_UNQ_%s", entityModel.name()),
                            Can.ofCollection(entityModel.secondaryKeyFields()).map(EntityField::name)));
                    case JPA, NONE -> {}
                }

            }

            typeModelBuilder.addSuperinterface(ParameterizedTypeName.get(
                    ClassName.get(HasSecondaryKey.class),
                    ClassName.get("", entityModel.name())));

            // inner params record
            var paramsRecord = _GenEntity_Params.generate(config, entityModel);
            typeModelBuilder.addType(paramsRecord);

            // inner secondary key record
            var secondaryKeyRecord = _GenEntity_SecondaryKey.generate(config, entityModel);
            typeModelBuilder.addType(secondaryKeyRecord);

            // secondary key factory method
            typeModelBuilder.addMethod(asSecondaryKeyMethod(secondaryKeyRecord, entityModel.secondaryKeyFields(), Modifier.PUBLIC));

            // inner unresolvable class
            var unresolvableClass = _GenEntity_Unresolvable.generate(config, entityModel);
            typeModelBuilder.addType(unresolvableClass);
        }
        return new QualifiedType(
                config.fullPackageName(entityModel.namespace()),
                typeModelBuilder.build());
    }

    // -- HELPER

    private MethodSpec asTitleMethod(final Entity entityModel, final Modifier ... modifiers) {
        return _Methods.objectSupport("title",
                CodeBlock.of("return $1L;", _Strings.nonEmpty(entityModel.title())
                        .orElse("this.toString()")),
                modifiers);
    }

    private MethodSpec asToStringMethod(final Entity entityModel) {
        var propertiesAsStrings = entityModel.fields().stream()
            .map(field->String.format("\"%s=\" + %s()", field.name(), field.getter()))
            .collect(Collectors.joining(" + \",\"\n +"));
        return _Methods.toString(
                CodeBlock.of("""
                        return "$1L(" + $2L + ")";""", entityModel.name(), propertiesAsStrings));
    }

    private MethodSpec asCopyMethod(final Entity entityModel) {
        var propertiesAsAssignments = entityModel.fields().stream()
                .map(field->String.format("copy.%s(%s());", field.setter(), field.getter()))
                .collect(Collectors.joining("\n"));
        return MethodSpec.methodBuilder("copy")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(_Annotations.programmatic())
                .addAnnotation(_Annotations.override())
                .returns(ClassName.get("", entityModel.name()))
                .addCode(CodeBlock.of("""
                        var copy = repositoryService.detachedEntity(new $1L());
                        $2L
                        return copy;""",
                        entityModel.name(),
                        propertiesAsAssignments))
                .build();
    }

    private Iterable<FieldSpec> asFields(
            final Persistence persistence,
            final List<Schema.EntityField> fields,
            final Modifier ... modifiers) {

        return fields.stream()
                .map(field->{
                    var fieldBuilder = FieldSpec.builder(
                            field.isEnum()
                                ? field.asJavaEnumType()
                                : field.asJavaType(),
                            field.name(),
                            modifiers)
                    .addJavadoc(field.propertyLayout().describedAs())
                    .addAnnotation(_Annotations.property(attr->{
                        attr.optionality(field.requiredInTheUi()
                                ? Optionality.MANDATORY
                                : Optionality.OPTIONAL);
                        var isEditingVetoed = field.hasForeignKeys()
                                || field.isMemberOfSecondaryKey();
                        if(!isEditingVetoed) attr.editing(Editing.ENABLED);
                        return attr;
                    }))
                    .addAnnotation(_Annotations.propertyLayout(attr->attr
                            .fieldSet(field.isMemberOfSecondaryKey()
                                    ? "identity"
                                    : field.hasForeignKeys()
                                        ? "foreign"
                                        : "details")
                            .sequence(field.sequence())
                            .hiddenWhere(field.hasForeignKeys()
                                            ? Where.ALL_TABLES
                                            : null),
                            field.propertyLayout())
                    );

                    switch (persistence) {
                        case JPA -> fieldBuilder.addAnnotation(_Annotations.jpaColumn(col->col
                                .columnName(field.column())
                                .columnDefinition(_TypeMapping.dbTypeToJdbcColumnExplicitType(field.columnType()))
                                .nullable(!field.required())
                                .length(field.maxLength())));
                        case JDO -> fieldBuilder.addAnnotation(_Annotations.jdoColumn(col->col
                                .columnName(field.column())
                                .jdbcType(_TypeMapping.dbTypeToJdbcColumnExplicitType(field.columnType()))
                                .allowsNull(!field.required())
                                .maxLength(field.maxLength())));
                        case NONE -> {}
                    }



                    fieldBuilder
                        .addAnnotation(_Annotations.getter())
                        .addAnnotation(_Annotations.setter());

                    if(field.isEnum()) {
                        fieldBuilder
                            .addAnnotation(_Annotations.datanucleusCheckEnumConstraint(true))
                            .addAnnotation(_Annotations.datanucleusEnumValueGetter("getMatchOn"));
                    }

                    return fieldBuilder.build();
                })
                .toList();
    }

    private MethodSpec asSecondaryKeyMethod(
            final TypeSpec secondaryKeyClass,
            final List<Schema.EntityField> fields,
            final Modifier ... modifiers) {
        return MethodSpec.methodBuilder("secondaryKey")
                .addModifiers(modifiers)
                .addAnnotation(_Annotations.programmatic())
                .returns(ClassName.get("", "SecondaryKey"))
                .addCode(String.format("return new SecondaryKey(%s);", asArgList(fields)))
                .build();
    }

    private String asArgList(final List<EntityField> fields) {
        return fields.stream()
                .map(field->field.isEnum()
                        ? String.format("%s()!=null ? %s().matchOn : null", field.getter(), field.getter())
                        : String.format("%s()", field.getter()))
                .collect(Collectors.joining(", \n"));
    }

}
