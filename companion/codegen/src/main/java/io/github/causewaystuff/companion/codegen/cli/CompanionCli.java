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

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.apache.causeway.commons.collections.Can;
import org.apache.causeway.commons.internal.base._Strings;
import org.apache.causeway.commons.io.DataSource;
import org.apache.causeway.commons.io.YamlUtils;

import io.github.causewaystuff.commons.base.types.ResourceFolder;
import io.github.causewaystuff.companion.codegen.cli.CodegenTask.CodegenResource;
import io.github.causewaystuff.tooling.projectmodel.ProjectNode;
import io.github.causewaystuff.tooling.projectmodel.ProjectNodeFactory;

public class CompanionCli {

    public static void main(final String[] args) {
        var argsModel = ArgsModel.parse(args);
        var projTree = ProjectNodeFactory.maven(argsModel.projectRoot().root());
        projTree.depthFirst(projModel ->
            CodegenYamlModel.createTask(projModel)
            .ifPresent(CodegenTask::run));
    }

    // -- HELPER

    record CodegenYamlModel(
            List<CodegenResource> codegen) {
        static Optional<CodegenTask> createTask(final ProjectNode projectNode) {
            final ResourceFolder artifactRoot = ResourceFolder.ofFile(projectNode.getProjectDirectory());
            final ResourceFolder javaRoot = artifactRoot.relative("src/main/java")
                    .orElse(null);
            final ResourceFolder resourcesRoot = artifactRoot.relative("src/main/resources")
                    .orElse(null);
            if(javaRoot==null
                    || resourcesRoot==null) {
                return Optional.empty();
            }
            var companionYaml = resourcesRoot.relativeFile("companion.yaml");
            if(!companionYaml.exists()) {
                return Optional.empty();
            }
            return YamlUtils.tryRead(CodegenYamlModel.class, DataSource.ofFile(companionYaml))
                    .getValue()
                    .map(codegenYamlModel->new CodegenTask(projectNode, javaRoot, resourcesRoot, codegenYamlModel.codegen()));
        }
    }

    private record ArgsModel(ResourceFolder projectRoot) {
        static ArgsModel parse(final String[] args) {
            if(args.length==0) {
                printUsageAndExit();
                return null;
            }
            var map = new HashMap<String, ResourceFolder>();
            Can.ofArray(args).stream()
                .map(kv->_Strings.parseKeyValuePair(kv, '=').orElseThrow())
                .forEach(kvp->map.put(kvp.getKey(), ResourceFolder.ofFileName(kvp.getValue()) ));
            var projectRoot = map.get("projectRoot");
            if(projectRoot==null) {
                printUsageAndExit();
                return null;
            }
            return new ArgsModel(map.get("projectRoot"));
        }
        static void printUsageAndExit() {
            System.err.println(
                    """
                    please provide the project root directory as input parameter like e.g.
                    projectRoot=/path/to/project""");
            System.exit(1);
        }
    }

}
