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
import java.util.stream.Collectors;

import org.apache.causeway.commons.collections.Can;
import org.apache.causeway.commons.internal.base._NullSafe;
import org.apache.causeway.commons.internal.base._Strings;

import lombok.experimental.UtilityClass;

@UtilityClass
class _Format {

    String parseYamlMultiline(final List<String> multiline, final String textWhenEmpty, final String continuation) {
        if(isMultilineStringBlank(multiline)) return textWhenEmpty;
        return multiline
                .stream()
                .map(String::trim)
                .collect(Collectors.joining(continuation));
    }

    String parseYamlMultiline(final List<String> multiline, final String textWhenEmpty, final String continuation, final String ... moreLines) {
        var descriptionLines = (isMultilineStringBlank(multiline)
                ? Can.of(textWhenEmpty)
                : multiline
                    .stream()
                    .map(String::trim)
                    .collect(Can.toCan()));
        var more = _NullSafe.stream(moreLines)
            .map(String::trim)
            .collect(Can.toCan());
        descriptionLines = descriptionLines.addAll(more);
        return descriptionLines.stream()
                .collect(Collectors.joining(continuation));
    }

    // -- HELPER

    private boolean isMultilineStringBlank(final List<String> lines) {
        return _NullSafe.size(lines)==0
            ? true
            : _Strings.isNullOrEmpty(lines.stream().collect(Collectors.joining("")).trim());
    }

}
