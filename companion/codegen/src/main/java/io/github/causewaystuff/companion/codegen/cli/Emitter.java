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
import io.github.causewaystuff.companion.codegen.cli.CodegenModel.SubProject;
import io.github.causewaystuff.companion.codegen.domgen.DomainGenerator;
import io.github.causewaystuff.companion.codegen.model.Schema;
import io.github.causewaystuff.companion.codegen.model.Schema.Domain;

record Emitter(
    CodegenModel.Project project) {

    void emitAll() {
        var domainsByNamespace = DomainAssembler.assemble(project);
        project.subProjectsByNamespace().values().forEach(subProject->emit(subProject, domainsByNamespace.get(subProject.namespace())));
    }

    void emit(final SubProject subProject, final Domain domain) {
        if(domain==null) return;

        var moduleDto = subProject.moduleDto();
        emitJavaFiles(DomainGenerator.Config.builder()
            .domain(domain)
            .licenseHeader(project.dto().license())
            .destinationFolder(subProject.javaRoot())
            .logicalNamespace(moduleDto.namespace())
            .javaPackageName(moduleDto.javaPackage())
            .onPurgeKeep(FileKeepStrategy.nonGenerated())
            .persistence(project.dto().persistence())
            .moduleClassSimpleName(moduleDto.moduleClassSimpleName())
            .build());

        emitDomainAsYaml(domain, subProject.resourcesRoot().relativeFile("companion-schema.yaml"));
    }

    void emitDomainAsYaml(
            final Schema.Domain domain,
            final File destFile) {
        domain.writeToFileAsYaml(destFile, project.dto().license());
    }

    void emitJavaFiles(final DomainGenerator.Config config) {
        config.destinationFolder().purgeFiles(config.onPurgeKeep());
        new DomainGenerator(config)
            .writeToDirectory(config.destinationFolder().root());
    }

}
