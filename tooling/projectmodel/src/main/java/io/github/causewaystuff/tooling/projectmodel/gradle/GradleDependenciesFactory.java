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
package io.github.causewaystuff.tooling.projectmodel.gradle;

import java.io.File;

import io.github.causewaystuff.tooling.projectmodel.ProjectNode;
import io.github.causewaystuff.tooling.projectmodel.ProjectNodeFactory;

public class GradleDependenciesFactory {

    public static GradleDependencies generateFromMaven(final File projRootFolder, final String rootProjectName) {
        var projTree = ProjectNodeFactory.maven(projRootFolder);
        return generateFromMaven(projTree, rootProjectName);
    }

    public static GradleDependencies generateFromMaven(final ProjectNode projTree, final String rootProjectName) {

        var gradleDependencies = new GradleDependencies();
        var dependenciesByShortName = gradleDependencies.getDependenciesByShortName();

        projTree.depthFirst(projModel -> {

            projModel.getDependencies().stream()
            .filter(dependency->dependency.getLocation().isExternal())
            .forEach(extDependency->{
                dependenciesByShortName.put(extDependency.getShortName(), extDependency);
            });

        });

        return gradleDependencies;
    }

}
