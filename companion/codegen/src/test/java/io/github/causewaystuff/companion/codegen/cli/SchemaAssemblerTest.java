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
package io.github.causewaystuff.companion.codegen.cli;

import java.util.stream.Stream;

import org.approvaltests.Approvals;
import org.approvaltests.reporters.DiffReporter;
import org.approvaltests.reporters.UseReporter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.causeway.testing.unittestsupport.applib.util.ApprovalUtils;

import io.github.causewaystuff.commons.base.types.ResourceFolder;
import io.github.causewaystuff.companion.codegen.domgen.DomainGenerator;
import io.github.causewaystuff.companion.codegen.model.Schema;
import io.github.causewaystuff.companion.schema.LicenseHeader;
import io.github.causewaystuff.companion.schema.Persistence;

class SchemaAssemblerTest {

    static Schema.ModuleNaming naming() {
        return new Schema.ModuleNaming("ns", "pkg");
    }

    @Test
    void assembleAndRoundtrip() {
        var schemaTestFileFolder = ResourceFolder.testResourceRoot().relativeFile("schema-test-files");
        var domain = DomainAssembler.assemble(naming(), schemaTestFileFolder);

        // test round-trip
        var yaml = domain.toYaml();
        assertEquals(
                domain,
                Schema.Domain.fromYaml(yaml));
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("javaSource")
    @UseReporter(DiffReporter.class)
    void javaGenerator(final String name, final String source) {
        Approvals.verify(source, Approvals.NAMES.withParameters(name)
            .withScrubber(ApprovalUtils.ignoreLineEndings()::scrub));
    }

    private static Stream<Arguments> javaSource() {
        var schemaTestFileFolder = ResourceFolder.testResourceRoot().relativeFile("schema-test-files");
        var domain = DomainAssembler.assemble(naming(), schemaTestFileFolder);
        assertEquals(naming(), domain.naming());

        var config = DomainGenerator.Config.builder()
                .domain(domain)
                .licenseHeader(LicenseHeader.ASF_V2)
                .persistence(Persistence.JDO)
                .build();

        return new DomainGenerator(config)
            .streamJavaModels()
            .map(javaModel->
                Arguments.of(javaModel.className().toString(), javaModel.buildJavaFile().toString()));
    }

}
