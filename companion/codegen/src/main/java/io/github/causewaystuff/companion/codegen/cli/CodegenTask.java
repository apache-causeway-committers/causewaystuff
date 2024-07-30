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

import java.util.List;

import io.github.causewaystuff.commons.base.types.ResourceFolder;
import io.github.causewaystuff.tooling.projectmodel.ProjectNode;

record CodegenTask(
        ProjectNode projectNode,
        ResourceFolder javaRoot,
        ResourceFolder resourcesRoot,
        List<CodegenResource> includes) {

    record CodegenResource(
            String include,
            String logicalNamespacePrefix,
            String packageNamePrefix,
            String entitiesModulePackageName,
            String entitiesModuleClassSimpleName
            ) {
    }

    void run() {

        includes().forEach(include->{
            var included = resourcesRoot.relative(include.include()).orElse(null);
            if(included==null) {
                return;
            }

            System.out.printf("CodegenTask: including %s:%s%n", this, included);

            var schemaAssembler = SchemaAssembler.assemble(included.root());
            schemaAssembler.writeAssembly(
                    resourcesRoot.relativeFile("%s.schema.yaml", include.include()));

            schemaAssembler.writeJavaFiles(cfg->cfg
                    .destinationFolder(javaRoot)
                    .logicalNamespacePrefix(include.logicalNamespacePrefix())
                    .packageNamePrefix(include.packageNamePrefix())
                    .onPurgeKeep(FileKeepStrategy.layout()
                            .or(FileKeepStrategy.javaNonGenerated())
                            )
                    .entitiesModulePackageName(include.entitiesModulePackageName())
                    .entitiesModuleClassSimpleName(include.entitiesModuleClassSimpleName()));
        });

    }

}
