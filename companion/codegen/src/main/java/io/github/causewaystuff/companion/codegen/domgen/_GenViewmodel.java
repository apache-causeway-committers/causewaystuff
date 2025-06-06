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

import org.apache.causeway.applib.annotation.Editing;
import org.apache.causeway.applib.annotation.Optionality;
import org.apache.causeway.commons.internal.base._Strings;

import lombok.experimental.UtilityClass;

import io.github.causewaystuff.companion.codegen.domgen.DomainGenerator.QualifiedType;
import io.github.causewaystuff.companion.codegen.domgen._Annotations.DomainObjectLayoutSpec;
import io.github.causewaystuff.companion.codegen.model.Schema;
import io.micronaut.sourcegen.javapoet.CodeBlock;
import io.micronaut.sourcegen.javapoet.FieldSpec;
import io.micronaut.sourcegen.javapoet.MethodSpec;
import io.micronaut.sourcegen.javapoet.ParameterSpec;
import io.micronaut.sourcegen.javapoet.TypeSpec;

@UtilityClass
class _GenViewmodel {

    public QualifiedType qualifiedType(
            final DomainGenerator.Config config,
            final Schema.Viewmodel vm) {

        return switch (vm.generator()) {
            case "class" -> {
                var typeModelBuilder = TypeSpec.classBuilder(vm.name())
                        .addJavadoc(vm.description().describedAs())
                        .addAnnotation(_Annotations.generated(_GenViewmodel.class))
                        .addAnnotation(_Annotations.named(config.fullLogicalName(vm.namespace()) + "." + vm.name()))
                        .addAnnotation(_Annotations.domainObject())
                        .addAnnotation(_Annotations.domainObjectLayout(
                                DomainObjectLayoutSpec.builder()
                                    .named(vm.named())
                                    .describedAs(vm.description().describedAs())
                                    .cssClassFa(vm.icon())
                                    .build()
                                ))
                        .addModifiers(Modifier.PUBLIC)
                        .addMethod(asTitleMethod(vm, Modifier.PUBLIC))
                        .addFields(asFields(vm.fields(), Modifier.PRIVATE))
                        ;

                yield new QualifiedType(
                        config.fullPackageName(vm.namespace()),
                        typeModelBuilder.build());
            }
            case "record" -> {
                var typeModelBuilder = TypeSpec.recordBuilder(vm.name())
                        .addJavadoc(vm.description().describedAs())
                        .addAnnotation(_Annotations.generated(_GenViewmodel.class))
                        .addAnnotation(_Annotations.named(config.fullLogicalName(vm.namespace()) + "." + vm.name()))
                        .addAnnotation(_Annotations.domainObject())
                        .addAnnotation(_Annotations.domainObjectLayout(
                                DomainObjectLayoutSpec.builder()
                                    .named(vm.named())
                                    .describedAs(vm.description().describedAs())
                                    .cssClassFa(vm.icon())
                                    .build()
                                ))
                        .addAnnotation(_Annotations.builder())
                        .addModifiers(Modifier.PUBLIC)
                        .addMethod(asTitleMethod(vm, Modifier.PUBLIC))
                        .addRecordComponents(asRecordComponents(vm.fields()))
                        ;

                yield new QualifiedType(
                        config.fullPackageName(vm.namespace()),
                        typeModelBuilder.build());
            }
            default ->
                throw new IllegalArgumentException("Unexpected value: " + vm.generator());
            };
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
                    .addJavadoc(field.propertyLayout().describedAs())
                    .addAnnotation(_Annotations.property(attr->{
                        attr.optionality(field.requiredInTheUi()
                                ? Optionality.MANDATORY
                                : Optionality.OPTIONAL);
                        attr.editing(Editing.ENABLED);
                        return attr;
                    }))
                    .addAnnotation(_Annotations.propertyLayout(attr->attr
                            .fieldSet("details")
                            .sequence(field.sequence()),
                        field.propertyLayout())
                    )
                    .addAnnotation(_Annotations.getter())
                    .addAnnotation(_Annotations.setter())
                    ;

                    return fieldBuilder.build();
                })
                .toList();
    }

    private Iterable<ParameterSpec> asRecordComponents(
            final List<Schema.VmField> fields) {
        return fields.stream()
                .map(field->
                    ParameterSpec.builder(
                            field.isEnum()
                                ? field.asJavaEnumType()
                                : field.asJavaType(),
                            field.name())
                    .addJavadoc(field.propertyLayout().describedAs())
                    .addAnnotation(_Annotations.propertyLayout(attr->attr
                            .fieldSet("details")
                            .sequence(field.sequence()),
                        field.propertyLayout())
                    )
                    .build())
                .toList();
    }

}
