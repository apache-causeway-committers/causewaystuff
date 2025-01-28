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
package io.github.causewaystuff.tooling.j2adoc.test;

import static io.github.causewaystuff.tooling.codeassert.config.Language.JAVA;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import com.github.javaparser.StaticJavaParser;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.apache.causeway.commons.collections.Can;
import org.apache.causeway.commons.internal.collections._Sets;
import org.apache.causeway.commons.io.DataSource;
import org.apache.causeway.commons.io.TextUtils;
import org.apache.causeway.valuetypes.asciidoc.builder.AsciiDocWriter;

import org.jspecify.annotations.NonNull;

import io.github.causewaystuff.tooling.codeassert.config.Language;
import io.github.causewaystuff.tooling.j2adoc.J2AdocContext;
import io.github.causewaystuff.tooling.j2adoc.util.AsciiDocIncludeTagFilter;
import io.github.causewaystuff.tooling.javamodel.AnalyzerConfigFactory;

class J2AdocTest {

    @Test
    void regression() {

        // javaparser.version=3.26.0 fails to parse snippet ..
        // hence we reinstated 3.25.10

        var source=
        DataSource.ofResource(getClass(), "SampleSwitch.java.txt")
        .tryReadAsStringUtf8()
        .valueAsNonNullElseFail();

        StaticJavaParser.parse(source);
    }

    @Test @Disabled
    void testJavaDoc2AsciiDoc() throws IOException {

        var analyzerConfig = AnalyzerConfigFactory
                .maven(ProjectSampler.apacheCausewayApplib(), Language.JAVA)
                .main();

        var j2aContext = J2AdocContext
                .builder()
//                .javaSourceWithFootnotesFormat()
                //.compactFormat()
                .build();

        analyzerConfig.getSources(JAVA)
        .stream()
        //.filter(source->source.toString().contains("ExecutionMode"))
        //.filter(source->source.toString().contains("FactoryService"))
        //.filter(source->source.toString().contains("Action"))
        //.filter(source->source.toString().contains("SudoService"))
        .filter(source->source.toString().contains("NonRecoverableException"))
        //.peek(source->System.out.println("parsing source: " + source))
        .forEach(j2aContext::add);

        final File tempFile = File.createTempFile("tmp", "adoc");
        j2aContext.streamUnits()
        //.peek(unit->System.err.println("namespace "+unit.getNamespace()))
        .map(unit->unit.toAsciiDoc(j2aContext, tempFile))
        .forEach(adoc->{

            //System.out.println(adoc);

            AsciiDocWriter.print(adoc);
            System.out.println();

        });
    }

    @Test// @Disabled
    void adocDocMining() throws IOException {

        var adocFiles = ProjectSampler.adocFiles(ProjectSampler.apacheCausewayRoot());

        var names = _Sets.<String>newTreeSet();

        Can.ofCollection(adocFiles)
        .stream()

        .filter(source->!source.toString().contains("\\generated\\"))

        //.filter(source->source.toString().contains("XmlSnapshotService"))
        .forEach(file->parseAdoc(file, names::add));

        names.forEach(System.out::println);
    }

    private void parseAdoc(final @NonNull File file, final Consumer<String> onName) {
        var lines = TextUtils.readLinesFromFile(file, StandardCharsets.UTF_8);

        ExampleReferenceFinder.find(
                lines,
                line->line.contains("system:generated:page$")
//               line->line.startsWith("include::")
//                    && line.contains("[tags=")
                    )
        .forEach(exRef->{
            onName.accept(String.format("%s in %s", exRef.name, exRef.matchingLine));
        });
    }

    @Test @Disabled("DANGER!")
    void removeAdocExampleTags() throws IOException {

        var analyzerConfig = AnalyzerConfigFactory
                .maven(ProjectSampler.apacheCausewayApplib(), Language.JAVA)
                .main();

        analyzerConfig.getSources(JAVA)
        .stream()
        .peek(source->System.out.println("parsing source: " + source))
        .filter(source->source.toString().contains("\\causeway\\applib\\"))
        .forEach(AsciiDocIncludeTagFilter::removeAdocExampleTags);

    }

    @Test @Disabled("DANGER!")
    void adocExampleProcessing() throws IOException {

        var adocFiles = ProjectSampler.adocFiles(ProjectSampler.apacheCausewayRoot());

        Can.ofCollection(adocFiles)
        .stream()
        //.filter(source->source.toString().contains("FactoryService"))
        .forEach(ExampleReferenceRewriter::processAdocExampleReferences);
    }

}
