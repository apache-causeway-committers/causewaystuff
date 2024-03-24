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
package org.causewaystuff.companion.codegen.domgen;

import java.util.List;
import java.util.stream.Collectors;

import javax.lang.model.element.Modifier;

import org.causewaystuff.companion.applib.services.iconfa.IconFaService;
import org.causewaystuff.companion.applib.services.lookup.HasSecondaryKey;
import org.causewaystuff.companion.applib.services.lookup.ISecondaryKey;
import org.causewaystuff.companion.applib.services.search.SearchService;
import org.causewaystuff.companion.codegen.domgen.DomainGenerator.QualifiedType;
import org.causewaystuff.companion.codegen.model.OrmModel;
import org.causewaystuff.companion.codegen.model.OrmModel.Entity;
import org.causewaystuff.companion.codegen.model.OrmModel.Field;
import org.causewaystuff.tooling.javapoet.ClassName;
import org.causewaystuff.tooling.javapoet.CodeBlock;
import org.causewaystuff.tooling.javapoet.FieldSpec;
import org.causewaystuff.tooling.javapoet.MethodSpec;
import org.causewaystuff.tooling.javapoet.ParameterSpec;
import org.causewaystuff.tooling.javapoet.ParameterizedTypeName;
import org.causewaystuff.tooling.javapoet.TypeSpec;

import org.apache.causeway.applib.ViewModel;
import org.apache.causeway.applib.annotation.Editing;
import org.apache.causeway.applib.annotation.Optionality;
import org.apache.causeway.applib.annotation.PrecedingParamsPolicy;
import org.apache.causeway.applib.annotation.Where;
import org.apache.causeway.applib.services.repository.RepositoryService;
import org.apache.causeway.commons.collections.Can;
import org.apache.causeway.commons.internal.base._Strings;

import lombok.val;
import lombok.experimental.UtilityClass;

@UtilityClass
class _GenEntity {

    public QualifiedType qualifiedType(
            final DomainGenerator.Config config,
            final OrmModel.Entity entityModel) {

        var typeModelBuilder = TypeSpec.classBuilder(entityModel.name())
                .addJavadoc(entityModel.formatDescription("\n"))
                .addAnnotation(_Annotations.generated(_GenEntity.class))
                .addAnnotation(_Annotations.named(config.fullLogicalName(entityModel.namespace()) + "." + entityModel.name()))
                .addAnnotation(_Annotations.domainObject())
                .addAnnotation(_Annotations.domainObjectLayout(
                        entityModel.formatDescription("\n"),
                        entityModel.icon()))
                .addAnnotation(_Annotations.persistenceCapable(entityModel.table()))
                .addAnnotation(_Annotations.datastoreIdentity())
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ParameterizedTypeName.get(
                        ClassName.get(org.causewaystuff.companion.applib.services.lookup.Cloneable.class),
                        ClassName.get("", entityModel.name())))
                .addField(_Fields.inject(RepositoryService.class, "repositoryService"))
                .addField(_Fields.inject(SearchService.class, "searchService"))
                .addMethod(asTitleMethod(entityModel, Modifier.PUBLIC))
                .addMethod(asToStringMethod(entityModel))
                .addMethod(asCopyMethod(entityModel))
                .addMethod(_Methods.navigableParent(entityModel.name()))
                .addFields(asFields(entityModel.fields(), Modifier.PRIVATE))
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
            val packageName = config.fullPackageName(entityModel.superTypeNamespace());
            val superTypeName = ClassName.get(packageName, entityModel.superTypeSimpleName());
            typeModelBuilder.addSuperinterface(superTypeName);
        }

        // inner enums

        entityModel.fields().stream()
                .filter(OrmModel.Field::isEnum)
                .forEach(field->
                    typeModelBuilder.addType(
                            _Enums.enumForColumn(field.asJavaType(), field.enumConstants())));

        // inner manager view model

        val managerViewmodel = TypeSpec.classBuilder("Manager")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .addJavadoc("Manager Viewmodel for @{link $1L}", entityModel.name())
                .addSuperinterface(ClassName.get(ViewModel.class))
                .addAnnotation(_Annotations.named(config.fullLogicalName(entityModel.namespace())
                        + "." + entityModel.name() + ".Manager"))
                .addAnnotation(_Annotations.domainObjectLayout(
                        entityModel.formatDescription("\n"),
                        entityModel.icon()))
                .addAnnotation(_Annotations.allArgsConstructor())
                .addField(FieldSpec.builder(SearchService.class, "searchService", Modifier.PUBLIC, Modifier.FINAL)
                        .build())
                .addField(FieldSpec.builder(String.class, "search", Modifier.PRIVATE)
                        .addAnnotation(_Annotations.property(attr->attr
                                .optionality(Optionality.OPTIONAL)
                                .editing(Editing.ENABLED)))
                        .addAnnotation(_Annotations.propertyLayout(attr->attr
                                .fieldSetId("searchBar")))
                        .addAnnotation(_Annotations.getter())
                        .addAnnotation(_Annotations.setter())
                        .build())
                .addMethod(_Methods.objectSupport("title",
                        CodeBlock.of("""
                                return "Manage $1L";""", _Strings.asNaturalName.apply(entityModel.name())),
                        Modifier.PUBLIC))
                .addMethod(_Methods.managerSearch(entityModel.name()))
                .addMethod(_Methods.viewModelMemento(CodeBlock.of("return getSearch();")))
                .build();

        typeModelBuilder.addType(managerViewmodel);

        if(entityModel.hasSecondaryKey()) {

            if(!entityModel.suppressUniqueConstraint()) {
                typeModelBuilder.addAnnotation(_Annotations.unique(
                        String.format("SEC_KEY_UNQ_%s", entityModel.name()),
                        Can.ofCollection(entityModel.secondaryKeyFields()).map(Field::name)));
            }

            typeModelBuilder.addSuperinterface(ParameterizedTypeName.get(
                    ClassName.get(HasSecondaryKey.class),
                    ClassName.get("", entityModel.name())));

            // inner params record

            val paramsRecord = TypeSpec.recordBuilder("Params")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .addJavadoc("Parameter model for @{link $1L}", entityModel.name())
                    .addRecordComponents(asParameterModelParams(config, entityModel.fields()))
                    .build();

            typeModelBuilder.addType(paramsRecord);

            // inner secondary key record

            val secondaryKeyRecord = TypeSpec.recordBuilder("SecondaryKey")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .addSuperinterface(ParameterizedTypeName.get(
                            ClassName.get(ISecondaryKey.class),
                            ClassName.get("", entityModel.name())))
                    .addJavadoc("SecondaryKey for @{link $1L}", entityModel.name())
                    .addRecordComponents(asSecondaryKeyParams(entityModel.secondaryKeyFields()))
                    .addMethod(MethodSpec.methodBuilder("correspondingClass")
                            .addModifiers(Modifier.PUBLIC)
                            .addAnnotation(_Annotations.override())
                            .returns(ParameterizedTypeName.get(
                                    ClassName.get(Class.class),
                                    ClassName.get("", entityModel.name())))
                            .addCode("return $1L.class;", entityModel.name())
                            .build())
                    .addMethod(MethodSpec.methodBuilder("unresolvable")
                            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                            .addAnnotation(_Annotations.override())
                            .returns(ClassName.get("", "Unresolvable"))
                            .addCode("""
                                    return new Unresolvable(String.format("UNRESOLVABLE %s%s",
                                        correspondingClass().getSimpleName(),
                                        this.toString().substring(12)));""")
                            .build())
                    .build();

            typeModelBuilder.addType(secondaryKeyRecord);
            typeModelBuilder.addMethod(asSecondaryKeyMethod(secondaryKeyRecord, entityModel.secondaryKeyFields(), Modifier.PUBLIC));

            // inner unresolvable class

            val unresolvableClass = TypeSpec.classBuilder("Unresolvable")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .superclass(ClassName.get("", entityModel.name()))
                    .addJavadoc("Placeholder @{link ViewModel} for @{link $1L} "
                            + "in case of an unresolvable secondary key.", entityModel.name())
                    .addSuperinterface(ClassName.get(org.apache.causeway.applib.ViewModel.class))
                    .addAnnotation(_Annotations.domainObjectLayout(
                            String.format("Unresolvable %s", entityModel.name()),
                            "skull .unresolvable-color"))
                    .addAnnotation(_Annotations.named(config.fullLogicalName(entityModel.namespace())
                            + "." + entityModel.name() + ".Unresolvable"))
                    .addAnnotation(_Annotations.requiredArgsConstructor())
                    .addField(FieldSpec.builder(ClassName.get(String.class), "viewModelMemento", Modifier.PRIVATE, Modifier.FINAL)
                            .addAnnotation(_Annotations.getterWithOverride())
                            .addAnnotation(_Annotations.accessorsFluent())
                            .build())
                    .addMethod(MethodSpec.methodBuilder("title")
                            .addModifiers(Modifier.PUBLIC)
                            .addAnnotation(_Annotations.override())
                            .returns(ClassName.get(String.class))
                            .addCode("return viewModelMemento;")
                            .build())
                    .build();

            typeModelBuilder.addType(unresolvableClass);
//            typeModelBuilder.addMethod(asUnresolvableMethod(unresolvableClass, entityModel.secondaryKeyFields(), Modifier.PUBLIC));

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
        val propertiesAsStrings = entityModel.fields().stream()
            .map(field->String.format("\"%s=\" + %s()", field.name(), field.getter()))
            .collect(Collectors.joining(" + \",\"\n +"));
        return _Methods.toString(
                CodeBlock.of("""
                        return "$1L(" + $2L + ")";""", entityModel.name(), propertiesAsStrings));
    }

    private MethodSpec asCopyMethod(final Entity entityModel) {
        val propertiesAsAssignments = entityModel.fields().stream()
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
            final List<OrmModel.Field> fields,
            final Modifier ... modifiers) {
        return fields.stream()
                .map(field->{
                    var fieldBuilder = FieldSpec.builder(
                            field.isEnum()
                                ? field.asJavaEnumType()
                                : field.asJavaType(),
                            field.name(),
                            modifiers)
                    .addJavadoc(field.formatDescription("\n"))
                    .addAnnotation(_Annotations.property(attr->{
                        attr.optionality(field.requiredInTheUi()
                                ? Optionality.MANDATORY
                                : Optionality.OPTIONAL);
                        val isEditingVetoed = field.hasForeignKeys()
                                || field.isMemberOfSecondaryKey();
                        if(!isEditingVetoed) attr.editing(Editing.ENABLED);
                        return attr;
                    }))
                    .addAnnotation(_Annotations.propertyLayout(attr->attr
                            .fieldSetId(field.isMemberOfSecondaryKey()
                                    ? "identity"
                                    : field.hasForeignKeys()
                                        ? "foreign"
                                        : "details")
                            .sequence(field.sequence())
                            .describedAs(
                                field.formatDescription("\n"))
                            .hiddenWhere(field.hasForeignKeys()
                                ? Where.ALL_TABLES
                                : Where.NOWHERE)))
                    .addAnnotation(_Annotations.column(field.column(), !field.required(), field.maxLength()))
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

    private Iterable<ParameterSpec> asParameterModelParams(
            final DomainGenerator.Config config,
            final List<OrmModel.Field> fields,
            final Modifier ... modifiers) {
        return fields.stream()
                .map(field->
                    ParameterSpec.builder(
                            field.isEnum()
                                ? field.asJavaEnumType()
                                : field.hasForeignKeys()
                                    ? _Foreign.foreignClassName(field, field.foreignFields().getFirstElseFail(), config)
                                    : field.asJavaType(),
                            field.hasForeignKeys()
                                    ? _Foreign.resolvedFieldName(field)
                                    : field.name(),
                            modifiers)
                    .addJavadoc(field.formatDescription("\n"))
                    .addAnnotation(_Annotations.parameter(attr->attr
                            .precedingParamsPolicy(
                                field.hasDiscriminator()
                                    ? PrecedingParamsPolicy.RESET
                                    : PrecedingParamsPolicy.PRESERVE_CHANGES)
                            .optionality(
                                field.requiredInTheUi()
                                    ? Optionality.MANDATORY
                                    : Optionality.OPTIONAL)))
                    .addAnnotation(_Annotations.parameterLayout(attr->attr
                            .describedAs(field.formatDescription("\n"))))
                    .build())
                .toList();
    }

    private Iterable<ParameterSpec> asSecondaryKeyParams(
            final List<OrmModel.Field> fields,
            final Modifier ... modifiers) {
        return fields.stream()
                .map(field->
                    ParameterSpec.builder(field.asJavaType(), field.name(), modifiers)
                    .addJavadoc(field.formatDescription("\n"))
                    .build())
                .toList();
    }

    private MethodSpec asSecondaryKeyMethod(
            final TypeSpec secondaryKeyClass,
            final List<OrmModel.Field> fields,
            final Modifier ... modifiers) {
        return MethodSpec.methodBuilder("secondaryKey")
                .addModifiers(modifiers)
                .addAnnotation(_Annotations.programmatic())
                .returns(ClassName.get("", "SecondaryKey"))
                .addCode(String.format("return new SecondaryKey(%s);", asArgList(fields)))
                .build();
    }

    private String asArgList(final List<Field> fields) {
        return fields.stream()
                .map(field->field.isEnum()
                        ? String.format("%s()!=null ? %s().matchOn : null", field.getter(), field.getter())
                        : String.format("%s()", field.getter()))
                .collect(Collectors.joining(", \n"));
    }

}
