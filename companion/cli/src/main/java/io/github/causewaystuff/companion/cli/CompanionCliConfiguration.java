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
package io.github.causewaystuff.companion.cli;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.causewaystuff.companion.assets.CoMutableProjectDescription;
import io.github.causewaystuff.companion.assets.CoProjectDescription;
import io.github.causewaystuff.companion.cli.CompanionCli.ArgsModel;
import io.spring.initializr.generator.buildsystem.BuildSystem;
import io.spring.initializr.generator.io.IndentingWriterFactory;
import io.spring.initializr.generator.io.SimpleIndentStrategy;
import io.spring.initializr.generator.project.ProjectAssetGenerator;
import io.spring.initializr.generator.project.ProjectDirectoryFactory;
import io.spring.initializr.metadata.InitializrMetadata;
import io.spring.initializr.metadata.InitializrMetadataBuilder;
import io.spring.initializr.metadata.InitializrProperties;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(InitializrProperties.class)
class CompanionCliConfiguration {

    @Bean
    public ProjectDirectoryFactory projectDirectoryFactory() {
        return (description) -> Files.createTempDirectory("project-");
    }
    
    @Bean
    public IndentingWriterFactory indentingWriterFactory() {
        return IndentingWriterFactory.create(new SimpleIndentStrategy("    "),
                (builder) -> builder.indentingStrategy("yaml", new SimpleIndentStrategy("  ")));
    }
    
    @Bean(name = "initializrMetadata")
    public InitializrMetadata initializrMetadata(InitializrProperties properties) {
        return InitializrMetadataBuilder.fromInitializrProperties(properties).build();
    }
    
    @Bean(name = "projectAssetGenerator") 
    public ProjectAssetGenerator<Path> projectAssetGenerator(ProjectDirectoryFactory projectDirectoryFactory) {
        return new CoProjectAssetGenerator(projectDirectoryFactory, 
            // exclusions
            Set.of("MavenWrapperContributor", "MavenBuildProjectContributor"));
    }
    
    static interface ProjectDescriptionFactory {
        CoProjectDescription create(ArgsModel argsModel);
    }
    
    @Bean 
    public ProjectDescriptionFactory projectDescriptionFactory(InitializrMetadata metadata) {
        return new ProjectDescriptionFactory() {

            @Override
            public CoProjectDescription create(ArgsModel argsModel) {
                var description = new CoMutableProjectDescription(
                    argsModel.projectRoot().root().toPath(), 
                    argsModel.appModel());
                description.setBuildSystem(getBuildSystem("maven-project", metadata));
                return description;
            }
            
            private BuildSystem getBuildSystem(String buildSystemId, InitializrMetadata metadata) {
                Map<String, String> typeTags = metadata.getTypes().get(buildSystemId).getTags();
                String id = typeTags.get("build");
                String dialect = typeTags.get("dialect");
                return BuildSystem.forIdAndDialect(id, dialect);
            }
        };
    }
    
}
