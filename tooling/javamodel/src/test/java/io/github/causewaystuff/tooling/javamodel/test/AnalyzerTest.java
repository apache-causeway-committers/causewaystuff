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
package io.github.causewaystuff.tooling.javamodel.test;

import static io.github.causewaystuff.tooling.codeassert.config.Language.JAVA;

import java.io.File;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import org.apache.causeway.commons.collections.Can;
import org.apache.causeway.commons.io.FileUtils;



import io.github.causewaystuff.tooling.codeassert.config.Language;
import io.github.causewaystuff.tooling.codeassert.model.CodeClass;
import io.github.causewaystuff.tooling.codeassert.model.Model;
import io.github.causewaystuff.tooling.javamodel.AnalyzerConfigFactory;
import io.github.causewaystuff.tooling.javamodel.ast.AnyTypeDeclaration;
import io.github.causewaystuff.tooling.javamodel.ast.CompilationUnits;

class AnalyzerTest {

    @Test
    void testSourceFileListing() {

        var projDir = ProjectSamples.causewaystuffCommonsBase();
        var analyzerConfig = AnalyzerConfigFactory.maven(projDir, Language.JAVA).main();
        var commonPath = projDir.getAbsolutePath();

        final Stream<String> sources = analyzerConfig.getSources(JAVA)
                .stream()
                .map(File::getAbsolutePath)
                .map(sourceFile->FileUtils.toRelativePath(commonPath, sourceFile));

        ProjectSamples.assertHasCommonsBaseSourceFiles(sources);
    }

    @Test //work in progress, as of yet a proof of concept
    void testJavaDocMining() {

        var projDir = ProjectSamples.self();
        var analyzerConfig = AnalyzerConfigFactory.mavenTest(projDir, Language.JAVA).main();

        analyzerConfig.getSources(JAVA)
        .stream()
        .filter(source->source.toString().contains("UserService"))
        .peek(source->System.out.println("parsing source: " + source))
        .map(CompilationUnits::parse)
        .flatMap(CompilationUnits::streamTypeDeclarations)
        .peek(td->{

            td.getJavadoc().ifPresent(javadoc->{

                javadoc.getBlockTags().stream()
                .filter(tag->tag.getTagName().equals("since"))
                .forEach(tag->System.out.println("--- SINCE " + tag.getContent().toText()));

            });

        })
        .map(AnyTypeDeclaration::getPublicMethodDeclarations)
        .flatMap(Can::stream)
        .forEach(md->{

            System.out.println("javadoc: " + md.getJavadocComment());
            System.out.println("non private method: " + md.getDeclarationAsString());

        });
    }

    @Test //fails when run with the CI pipeline
    //@DisabledIfRunningWithSurefire
    void testAnnotationGathering() {

        var projDir = ProjectSamples.causewaystuffBlobstoreApplib();
        var analyzerConfig = AnalyzerConfigFactory.maven(projDir, Language.JAVA).main();

        var model = Model.from(analyzerConfig.getClasses()).read();

        final Stream<String> components = model.getClasses()
                .stream()
                .filter(codeClass->codeClass
                        .getAnnotations()
                        .stream()
                        .map(CodeClass::getName)
                        .anyMatch(name->name.startsWith("org.springframework.stereotype.")))
                .map(CodeClass::getName);

        ProjectSamples.assertHasBlobstoreApplibServices(components);
    }

}
