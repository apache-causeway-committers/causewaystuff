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
import java.util.Optional;

import org.apache.causeway.commons.io.FileUtils;

import io.github.causewaystuff.companion.codegen.cli.CodegenModel.SubProject;
import io.github.causewaystuff.companion.codegen.model.Schema;
import io.github.causewaystuff.companion.codegen.model.Schema.Domain;

record DomainAssembler() {

    static Optional<Domain> assemble(final SubProject subProject) {
        return subProject.streamFragments()
            .map(includedFolder->{
                System.out.printf("DomainAssembler: including %s:%s%n", subProject, includedFolder);
                return assemble(includedFolder.root());
            })
            .reduce(Schema.Domain::concat);
    }

    static Schema.Domain assemble(final File yamlFolder) {
        FileUtils.existingDirectoryElseFail(yamlFolder);
        return Schema.Domain.fromYamlFolder(yamlFolder);
    }

}

