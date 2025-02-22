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

import java.util.Optional;

import com.github.javaparser.ast.PackageDeclaration;

import org.apache.causeway.commons.collections.Can;
import org.apache.causeway.commons.internal.base._Strings;

import org.jspecify.annotations.NonNull;

public final class PackageDeclarations {

    public static Can<String> namespace(final @NonNull Optional<PackageDeclaration> pdOptional) {
        return pdOptional
        .map(PackageDeclaration::getNameAsString)
        .map(_Strings::nullToEmpty)
        .map(String::trim)
        .map(namespace->_Strings.splitThenStream(namespace, ".").collect(Can.toCan()))
        .orElseGet(()->Can.of("default"));
    }

}
