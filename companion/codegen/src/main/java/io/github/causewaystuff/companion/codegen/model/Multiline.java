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

import java.util.List;

import org.apache.causeway.commons.internal.base._Strings;

public record Multiline(List<String> lines) {

    public static Multiline empty() {
        return new Multiline(List.of());
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }

    public String describedAs() {
        return formatDescription("\n");
    }

    // -- INTERNAL

    static Multiline parseMultilineString(final String input) {
        return new Multiline(_Strings.splitThenStream(input, "\n")
            .filter(_Strings::isNotEmpty)
            .toList());
    }

    static Multiline parseMultilineStringTrimmed(final String input) {
        return new Multiline(_Strings.splitThenStream(input, "\n")
            .filter(_Strings::isNotEmpty)
            .map(String::trim)
            .toList());
    }

    private String formatDescription(final String continuation, final String ... moreLines) {
        return _Format.parseYamlMultiline(lines(), "has no description", continuation, moreLines);
    }

}
