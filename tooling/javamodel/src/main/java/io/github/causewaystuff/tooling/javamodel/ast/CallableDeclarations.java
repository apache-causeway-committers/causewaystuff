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

import com.github.javaparser.ast.body.CallableDeclaration;

import org.jspecify.annotations.NonNull;

//TODO effective public might require more context
public final class CallableDeclarations {

    public static String asAnchor(final @NonNull CallableDeclaration<?> md) {
        return nameAndParams(md, "_", "_", "");
    }

    public static String asMethodSignature(final @NonNull CallableDeclaration<?> md) {
        return nameAndParams(md, "(", ", ", ")");
    }

    private static String nameAndParams(@NonNull CallableDeclaration<?> md, String openParam, String comma, String closeParam) {
        var sb = new StringBuilder();
        sb.append(md.getName());
        sb.append(openParam);
        var firstParam = true;
        for (var param : md.getParameters()) {
            if (firstParam) {
                firstParam = false;
            } else {
                sb.append(comma);
            }
            sb.append(sanitize(param.getType().asString()));
        }
        sb.append(closeParam);
        return sb.toString();
    }

    private static String sanitize(String paramType) {
        var paramTypeNoWildcard = paramType.split("<")[0];
        var paramTypeNoArray = paramTypeNoWildcard.split("\\[")[0];
        var paramTypeNoDots = paramTypeNoArray.replace('.', '_');
        return paramTypeNoDots;
    }

}
