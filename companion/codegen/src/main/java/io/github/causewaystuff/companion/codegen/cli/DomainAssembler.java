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
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import org.apache.causeway.commons.io.FileUtils;

import io.github.causewaystuff.commons.base.types.ResourceFolder;
import io.github.causewaystuff.companion.codegen.model.Schema;
import io.github.causewaystuff.companion.codegen.model.Schema.Domain;
import io.github.causewaystuff.companion.codegen.model.Schema.ModuleNaming;

record DomainAssembler() {

    static Map<String, Domain> assemble(final CodegenModel.Project project) {
        var domainsByNamespace = new TreeMap<String, Domain>();
        project.subProjectsByNamespace().forEach((namespace, subProject)->{
            var naming = new ModuleNaming(namespace, subProject.moduleDto().javaPackage());
            var domain = subProject.streamFragments()
                .map(f->assemble(naming, f))
                .reduce(Schema.Domain::concat)
                .orElse(null);
            if(domain==null) return;
            domainsByNamespace.put(namespace, domain);
        });

        // resolve dependencies
        project.subProjectsByNamespace().forEach((namespace, subProject)->{
            var imports = subProject.moduleDto().imports();
            if(imports.isEmpty()) return;

            var domain = domainsByNamespace.get(namespace);
            if(domain==null) return;

            imports.stream()
                .map(domainsByNamespace::get)
                .filter(Objects::nonNull)
                .forEach(domain.dependencies()::add);
        });

        return domainsByNamespace;
    }

    static Schema.Domain assemble(final ModuleNaming naming, final ResourceFolder fragmentFolder) {
        System.out.printf("DomainAssembler: including %s%n", fragmentFolder);
        return assemble(naming, fragmentFolder.root());
    }

    static Schema.Domain assemble(final ModuleNaming naming, final File yamlFolder) {
        FileUtils.existingDirectoryElseFail(yamlFolder);
        return Schema.Domain.fromYamlFolder(naming, yamlFolder);
    }

}

