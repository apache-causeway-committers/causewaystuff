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

import javax.lang.model.element.Modifier;

import io.micronaut.sourcegen.javapoet.ClassName;
import io.micronaut.sourcegen.javapoet.FieldSpec;
import io.micronaut.sourcegen.javapoet.TypeName;

import lombok.experimental.UtilityClass;

@UtilityClass
class _Fields {

    FieldSpec inject(final TypeName fieldType, final String fieldName, final Modifier... modifiers) {
        return FieldSpec.builder(fieldType, fieldName, modifiers)
            .addAnnotation(_Annotations.inject())
            .build();
    }

    FieldSpec inject(final Class<?> injectedType, final String fieldName, final Modifier... modifiers) {
        return inject(TypeName.get(injectedType), fieldName, modifiers);
    }

    FieldSpec mixee(final ClassName mixeeType, final Modifier ... modifiers) {
        return FieldSpec.builder(mixeeType, "mixee", modifiers)
            .build();
    }

    //private static final long serialVersionUID = 1L;
    FieldSpec serialVersionUID(final long value) {
        return FieldSpec.builder(ClassName.LONG, "serialVersionUID", 
                    Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("$1L", value)
                .build();
    }

    /**
     * Example:
     * <pre>
     * public final static String NAMESPACE = "my.module";
     * </pre> 
     */
    FieldSpec namespaceConstant(String namespace) {
        return FieldSpec.builder(ClassName.get(String.class), "NAMESPACE", 
                    Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("$1S", namespace)
                .build();
    }

}
