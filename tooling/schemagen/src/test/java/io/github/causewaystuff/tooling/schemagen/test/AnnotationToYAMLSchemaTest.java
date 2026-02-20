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
package io.github.causewaystuff.tooling.schemagen.test;

import static io.github.causewaystuff.tooling.codeassert.config.Language.JAVA;

import java.io.File;

import org.junit.jupiter.api.Test;

import org.apache.causeway.applib.annotation.PropertyLayout;
import org.apache.causeway.commons.collections.Can;
import org.apache.causeway.commons.io.YamlUtils;
import org.apache.causeway.testing.unittestsupport.applib.annotations.DisabledIfRunningWithSurefire;

import io.github.causewaystuff.tooling.codeassert.config.Language;
import io.github.causewaystuff.tooling.javamodel.AnalyzerConfigFactory;
import io.github.causewaystuff.tooling.javamodel.ast.AnnotationMemberDeclarations;
import io.github.causewaystuff.tooling.javamodel.ast.AnyTypeDeclaration;
import io.github.causewaystuff.tooling.javamodel.ast.CompilationUnits;
import io.github.causewaystuff.tooling.schemagen.SchemaGeneratorUtils;
import tools.jackson.databind.JsonNode;

class AnnotationToYAMLSchemaTest {

    @DisabledIfRunningWithSurefire // WIP
    @Test
    void propertyLayout()  {
        var generator = SchemaGeneratorUtils.schemaGeneratorWithAnnotationTypeSupport();
        //JsonNode jsonSchema = generator.generateSchema(Customer.class);
        JsonNode jsonSchema = generator.generateSchema(PropertyLayout.class);

        //TODO we'd like to post process and add description nodes from annotation member's java-doc
        //TODO we'd like to post process and remove any '()'
        //System.err.printf("%s%n", jsonSchema);
        System.err.printf("%s%n", YamlUtils.toStringUtf8(jsonSchema));
    }

    @DisabledIfRunningWithSurefire // WIP - gather annotation member's java-doc
    @Test
    void testAnnotationGathering() {

        var projDir = new File("D:\\development\\git\\cw-main\\causeway\\api\\applib");
        if(!projDir.exists()) return;
        var analyzerConfig = AnalyzerConfigFactory.maven(projDir, Language.JAVA).main();

        analyzerConfig
            .getSources(JAVA)
            .stream()
            .filter(source->source.toString().contains("annotation"))
            //.peek(source->System.out.println("parsing source: " + source))
            .map(CompilationUnits::parse)
            .flatMap(CompilationUnits::streamTypeDeclarations)
            .filter(anyT->anyT.getKind().isAnnotation())
            .peek(td->{
                System.out.println("@" + td.getSimpleName());
                System.out.println("properties:");
            })
            .map(AnyTypeDeclaration::getAnnotationMemberDeclarations)
            .flatMap(Can::stream)
            .forEach(amd->{
                System.out.println("  " + AnnotationMemberDeclarations.asNormalizedName(amd) + ":");
                System.out.println("    type: " + amd.getTypeAsString());
                System.out.println("    description: " + amd.getJavadocComment());
            });

    }

}
