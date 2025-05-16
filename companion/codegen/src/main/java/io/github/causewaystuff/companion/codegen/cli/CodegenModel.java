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
import java.util.Optional;

import org.apache.causeway.commons.io.DataSource;
import org.apache.causeway.commons.io.YamlUtils;

import lombok.experimental.UtilityClass;

import io.github.causewaystuff.commons.base.types.ResourceFolder;
import io.github.causewaystuff.tooling.projectmodel.ProjectNode;

@UtilityClass
class CodegenModel {

    /**
     * A folder with fragments that contribute to the sub-project's domain.
     */
    record ResourceFragmentFolder (
        String include,
        String logicalNamespacePrefix,
        String packageNamePrefix,
        String entitiesGenerator,
        String entitiesModulePackageName,
        String entitiesModuleClassSimpleName) {
    }

    /**
     * All the resources, that contribute to a single sub-project domain.
     */
    record SubProject(
        ProjectNode projectNode,
        ResourceFolder javaRoot,
        ResourceFolder resourcesRoot,
        List<ResourceFragmentFolder> includes) {
    }

    record CodegenYamlModel(
        List<ResourceFragmentFolder> codegen) {
    }

    Optional<SubProject> readSubProject(final ProjectNode projectNode) {
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
                .map(codegenYamlModel->new SubProject(projectNode, javaRoot, resourcesRoot, codegenYamlModel.codegen()));
    }

}
