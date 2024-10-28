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
package io.github.causewaystuff.tooling.j2adoc.util;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import org.apache.causeway.commons.io.TextUtils;

public final class AsciiDocIncludeTagFilter {

    public static String read(final File source) {
        return TextUtils.readLinesFromFile(source, StandardCharsets.UTF_8).stream()
        //.filter(line->!containsIncludeTag(line))
        .filter(line->!isAllLineComment(line))
        .map(AsciiDocIncludeTagFilter::removeFootNoteReference)
        .collect(Collectors.joining("\n"));
    }

    public static void removeAdocExampleTags(final File source) {
        var fixedLines = TextUtils.readLinesFromFile(source, StandardCharsets.UTF_8)
        .filter(line->!isIncludeTagComment(line))
        .map(AsciiDocIncludeTagFilter::removeFootNoteReference);

        TextUtils.writeLinesToFile(fixedLines, source, StandardCharsets.UTF_8);
    }

    // -- HELPER

    private static boolean isIncludeTagComment(String line) {
        line = line.trim();
        if(!line.startsWith("//")) {
            return false;
        }
        return line.contains(" tag::")
                || line.contains(" end::");
    }

    private static boolean isAllLineComment(final String line) {
        return line.trim().startsWith("//");
    }

    private static String removeFootNoteReference(final String line) {
        if(!line.contains("// <")) {
            return line;
        }
        return line.replace("// <.>", "")
                .replace("// <1>", "")
                .replace("// <2>", "")
                .replace("// <3>", "")
                .replace("// <4>", "")
                .replace("// <5>", "")
                .replace("// <6>", "")
                .replace("// <7>", "")
                .replace("// <8>", "")
                .replace("// <9>", "")
                .replace("// <10>", "")
                .replace("// <11>", "")
                .replace("// <12>", "")
                .replace("// <13>", "")
                .replace("// <14>", "")
                .replace("// <15>", "")
                .replace("// <16>", "")
                .stripTrailing();
    }

}
