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

import java.io.File;
import java.util.function.UnaryOperator;

import org.apache.causeway.commons.io.FileUtils;

import io.github.causewaystuff.companion.codegen.domgen.DomainGenerator;
import io.github.causewaystuff.companion.codegen.domgen.LicenseHeader;
import io.github.causewaystuff.companion.codegen.model.Schema;

record SchemaAssembler(LicenseHeader licenseHeader, Schema.Domain domain) {
    static SchemaAssembler assemble(final File yamlFolder) {
        FileUtils.existingDirectoryElseFail(yamlFolder);
        var domain = Schema.Domain.fromYamlFolder(yamlFolder);
        return new SchemaAssembler(LicenseHeader.ASF_V2, domain);
    }
    void writeAssembly(final File destinationSchemaFile) {
        domain.writeToFileAsYaml(
                destinationSchemaFile,
                licenseHeader);
    }
    void writeJavaFiles(
            final UnaryOperator<DomainGenerator.Config.ConfigBuilder> customizer) {
        var config = customizer.apply(DomainGenerator.Config.builder()
                .domain(domain))
                .licenseHeader(licenseHeader)
                .build();
        config.destinationFolder().purgeFiles(config.onPurgeKeep());
        new DomainGenerator(config)
            .writeToDirectory(config.destinationFolder().root());
    }
}

