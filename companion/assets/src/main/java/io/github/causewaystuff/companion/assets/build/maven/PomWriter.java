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
package io.github.causewaystuff.companion.assets.build.maven;

import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.apache.maven.model.v4.MavenStaxWriter;

import org.apache.causeway.commons.collections.Can;
import org.apache.causeway.commons.io.TextUtils;

import lombok.SneakyThrows;

import io.github.causewaystuff.companion.schema.LicenseHeader;

record PomWriter(
    org.apache.maven.api.model.Model model,
    LicenseHeader license,
    Can<String> generatedNoticeLines) {
   
    @SneakyThrows
    void write(Path pomFile) {

        var sw = new StringWriter();
        new MavenStaxWriter().write((Writer)sw, model);
        var lines = TextUtils.readLines(sw.toString());
        
        var headComment = switch (license) {
            case ASF_V2 -> license.text() + "\n\n" + generatedNoticeLines.join("\n");
            case NONE -> generatedNoticeLines.join("\n");
        };
        
        lines = lines.add(1, "<!--\n%s\n-->".formatted(headComment));

        TextUtils.writeLinesToFile(lines, pomFile.toFile(), StandardCharsets.UTF_8);
    }
    
}
