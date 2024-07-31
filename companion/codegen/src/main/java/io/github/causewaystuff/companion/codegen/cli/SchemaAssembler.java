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
import io.github.causewaystuff.companion.codegen.model.OrmModel;

record SchemaAssembler(LicenseHeader licenseHeader, OrmModel.Schema schema) {
    static SchemaAssembler assemble(final File yamlFolder) {
        FileUtils.existingDirectoryElseFail(yamlFolder);
        var schema = OrmModel.Schema.fromYamlFolder(yamlFolder);
        return new SchemaAssembler(LicenseHeader.ASF_V2, schema);
    }
    void writeAssembly(final File destinationSchemaFile) {
        schema.writeToFileAsYaml(
                destinationSchemaFile,
                licenseHeader);
    }
    void writeJavaFiles(
            final UnaryOperator<DomainGenerator.Config.ConfigBuilder> customizer) {
        var config = customizer.apply(DomainGenerator.Config.builder()
                //.datastore("store2") // DN Data Federation
                .schema(schema))
                .licenseHeader(licenseHeader)
                .build();
        config.destinationFolder().purgeFiles(config.onPurgeKeep());
        new DomainGenerator(config)
            .writeToDirectory(config.destinationFolder().root());
    }
}

