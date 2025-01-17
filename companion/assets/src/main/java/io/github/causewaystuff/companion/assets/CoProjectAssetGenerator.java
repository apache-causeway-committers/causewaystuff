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
package io.github.causewaystuff.companion.assets;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import io.spring.initializr.generator.project.DefaultProjectAssetGenerator;
import io.spring.initializr.generator.project.ProjectAssetGenerator;
import io.spring.initializr.generator.project.ProjectDescription;
import io.spring.initializr.generator.project.ProjectDirectoryFactory;
import io.spring.initializr.generator.project.ProjectGenerationContext;
import io.spring.initializr.generator.project.contributor.ProjectContributor;

/**
 * Variant of #DefaultProjectAssetGenerator
 * @see DefaultProjectAssetGenerator
 */
class CoProjectAssetGenerator implements ProjectAssetGenerator<Path> {

    private final ProjectDirectoryFactory projectDirectoryFactory;
    private final Predicate<? super ProjectContributor> projectContributorFilter;

    /**
     * Create a new instance with the {@link ProjectDirectoryFactory} to use.
     * @param projectDirectoryFactory the project directory factory to use
     */
    public CoProjectAssetGenerator(ProjectDirectoryFactory projectDirectoryFactory, Set<String> excludedContributorsBySimpleName) {
        this.projectDirectoryFactory = projectDirectoryFactory;
        this.projectContributorFilter = (contributor) -> 
            !excludedContributorsBySimpleName.contains(contributor.getClass().getSimpleName()); 
    }

    @Override
    public Path generate(ProjectGenerationContext context) throws IOException {
        ProjectDescription description = context.getBean(ProjectDescription.class);
        Path projectRoot = projectDirectoryFactory.createProjectDirectory(description);
        Path projectDirectory = initializerProjectDirectory(projectRoot, description);
        List<ProjectContributor> contributors = context.getBeanProvider(ProjectContributor.class)
            .orderedStream()
            .filter(projectContributorFilter)
            .toList();
        for (ProjectContributor contributor : contributors) {
            contributor.contribute(projectDirectory);
        }
        return projectRoot;
    }

    private Path initializerProjectDirectory(Path rootDir, ProjectDescription description) throws IOException {
        Path projectDirectory = resolveProjectDirectory(rootDir, description);
        Files.createDirectories(projectDirectory);
        return projectDirectory;
    }

    private Path resolveProjectDirectory(Path rootDir, ProjectDescription description) {
        if (description.getBaseDirectory() != null) {
            return rootDir.resolve(description.getBaseDirectory());
        }
        return rootDir;
    }

}
