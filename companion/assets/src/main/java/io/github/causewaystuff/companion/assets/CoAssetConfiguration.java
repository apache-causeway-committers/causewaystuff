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

import java.nio.file.Path;
import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.spring.initializr.generator.project.ProjectAssetGenerator;
import io.spring.initializr.generator.project.ProjectDirectoryFactory;

@Configuration
public class CoAssetConfiguration {
    
    /**
     * Excludes some contributors from the initializr-spring artifact, so we can provide our own.
     * 
     * @see io.spring.initializr.generator.spring.properties.ApplicationPropertiesContributor
     * @see io.spring.initializr.generator.spring.scm.git.GitIgnoreContributor
     * @see io.spring.initializr.generator.spring.build.maven.MavenWrapperContributor
     * @see io.spring.initializr.generator.spring.build.maven.MavenBuildProjectContributor
     * @see io.spring.initializr.generator.spring.code.MainSourceCodeProjectContributor
     * @see io.spring.initializr.generator.spring.code.TestSourceCodeProjectContributor
     */
    @Bean(name = "companionAssetGenerator")
    public ProjectAssetGenerator<Path> companionAssetGenerator(ProjectDirectoryFactory projectDirectoryFactory) {
        return new CoProjectAssetGenerator(projectDirectoryFactory, 
            // exclusions
            Set.of(
                "ApplicationPropertiesContributor", "GitIgnoreContributor",
                "MavenWrapperContributor", "MavenBuildProjectContributor",
                "MainSourceCodeProjectContributor", "TestSourceCodeProjectContributor"));
    }
}
