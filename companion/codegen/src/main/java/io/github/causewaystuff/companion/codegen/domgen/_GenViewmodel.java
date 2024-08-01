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

import javax.lang.model.element.Modifier;

import org.apache.causeway.commons.internal.base._Strings;

import lombok.experimental.UtilityClass;

import io.github.causewaystuff.companion.codegen.domgen.DomainGenerator.QualifiedType;
import io.github.causewaystuff.companion.codegen.model.Schema;
import io.github.causewaystuff.tooling.javapoet.CodeBlock;
import io.github.causewaystuff.tooling.javapoet.FieldSpec;
import io.github.causewaystuff.tooling.javapoet.MethodSpec;
import io.github.causewaystuff.tooling.javapoet.TypeSpec;

@UtilityClass
class _GenViewmodel {

    public QualifiedType qualifiedType(
            final DomainGenerator.Config config,
            final Schema.Viewmodel vm) {

        var typeModelBuilder = TypeSpec.classBuilder(vm.name())
                .addJavadoc(vm.formatDescription("\n"))
                .addAnnotation(_Annotations.generated(_GenViewmodel.class))
                .addAnnotation(_Annotations.named(config.fullLogicalName(vm.namespace()) + "." + vm.name()))
                .addAnnotation(_Annotations.domainObject())
                .addAnnotation(_Annotations.domainObjectLayout(
                        vm.formatDescription("\n"),
                        vm.icon()))
                .addModifiers(Modifier.PUBLIC)
                .addMethod(asTitleMethod(vm, Modifier.PUBLIC))
                .addFields(asFields(vm.fields(), Modifier.PRIVATE))
                ;

        return new QualifiedType(
                config.fullPackageName(vm.namespace()),
                typeModelBuilder.build());
    }

    // -- HELPER

    private MethodSpec asTitleMethod(final Schema.Viewmodel vm, final Modifier ... modifiers) {
        return _Methods.objectSupport("title",
                CodeBlock.of("return $1L;", _Strings.nonEmpty(vm.title())
                        .orElse("this.toString()")),
                modifiers);
    }

    private Iterable<FieldSpec> asFields(
            final List<Schema.VmField> fields,
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
//                    .addAnnotation(_Annotations.property(attr->{
//                        attr.optionality(field.requiredInTheUi()
//                                ? Optionality.MANDATORY
//                                : Optionality.OPTIONAL);
//                        attr.editing(Editing.ENABLED);
//                        return attr;
//                    }))
//                    .addAnnotation(_Annotations.propertyLayout(attr->attr
//                            .fieldSetId(field.isMemberOfSecondaryKey()
//                                    ? "identity"
//                                    : field.hasForeignKeys()
//                                        ? "foreign"
//                                        : "details")
//                            .sequence(field.sequence())
//                            .describedAs(
//                                field.formatDescription("\n"))
//                            .multiLine(field.multiLine().orElse(0))
//                            .hiddenWhere(Optional.ofNullable(field.hiddenWhere())
//                                    .orElseGet(()->field.hasForeignKeys()
//                                            ? Where.ALL_TABLES
//                                            : Where.NOWHERE)
//                                    )))
//                    .addAnnotation(_Annotations.column(field.column(), !field.required(), field.maxLength()))
//                    .addAnnotation(_Annotations.getter())
//                    .addAnnotation(_Annotations.setter())
                    ;

                    return fieldBuilder.build();
                })
                .toList();
    }

}
