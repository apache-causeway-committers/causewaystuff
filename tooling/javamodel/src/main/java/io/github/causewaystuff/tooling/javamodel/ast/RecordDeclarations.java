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
package io.github.causewaystuff.tooling.javamodel.ast;

import org.jspecify.annotations.NonNull;

import java.util.stream.Stream;

import com.github.javaparser.ast.body.*;

public final class RecordDeclarations {

    // -- FIELDS

    public static <T> Stream<FieldDeclaration> streamFieldDeclarations(
            final @NonNull RecordDeclaration typeDeclaration) {
        return typeDeclaration.getFields().stream();
    }

    public static <T> Stream<FieldDeclaration> streamPublicFieldDeclarations(
            final @NonNull RecordDeclaration typeDeclaration) {
        return streamFieldDeclarations(typeDeclaration)
                .filter(fd->FieldDeclarations.isEffectivePublic(fd, typeDeclaration));
    }

    // -- CONSTRUCTORS

    public static <T> Stream<ConstructorDeclaration> streamConstructorDeclarations(
            final @NonNull RecordDeclaration typeDeclaration) {
        return typeDeclaration.getConstructors().stream();
    }

    public static <T> Stream<ConstructorDeclaration> streamPublicConstructorDeclarations(
            final @NonNull RecordDeclaration typeDeclaration) {
        return streamConstructorDeclarations(typeDeclaration)
                .filter(cd->ConstructorDeclarations.isEffectivePublic(cd, typeDeclaration));
    }

    // -- METHODS

    public static <T> Stream<MethodDeclaration> streamMethodDeclarations(
            final @NonNull RecordDeclaration typeDeclaration) {
        return typeDeclaration.getMethods().stream();
    }

    public static <T> Stream<MethodDeclaration> streamPublicMethodDeclarations(
            final @NonNull RecordDeclaration typeDeclaration) {
        return streamMethodDeclarations(typeDeclaration)
                .filter(md->MethodDeclarations.isEffectivePublic(md, typeDeclaration));
    }

    // -- CONTEXT

    public static boolean isEffectivePublic(final @NonNull RecordDeclaration td) {
        return !td.isPrivate()
                && !td.isProtected()
                ;
    }

}
