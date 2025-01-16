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

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.causewaystuff.companion.schema.CoApplication;
import io.spring.initializr.generator.buildsystem.BuildSystem;
import io.spring.initializr.generator.io.IndentingWriterFactory;
import io.spring.initializr.generator.io.SimpleIndentStrategy;
import io.spring.initializr.generator.language.Language;
import io.spring.initializr.generator.packaging.Packaging;
import io.spring.initializr.generator.project.DefaultProjectAssetGenerator;
import io.spring.initializr.generator.project.MutableProjectDescription;
import io.spring.initializr.generator.project.ProjectAssetGenerator;
import io.spring.initializr.generator.project.ProjectDescription;
import io.spring.initializr.generator.project.ProjectDirectoryFactory;
import io.spring.initializr.generator.version.Version;
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
    public ProjectAssetGenerator<Path> projectAssetGenerator() {
        return new DefaultProjectAssetGenerator();
    }
    
    static interface ProjectDescriptionFactory {
        ProjectDescription create(Path baseDirectory, CoApplication appModel);
    }
    
    @Bean 
    public ProjectDescriptionFactory projectDescriptionFactory(InitializrMetadata metadata) {
        return new ProjectDescriptionFactory() {

            @Override
            public ProjectDescription create(Path baseDirectory, CoApplication appModel) {
                var description = new MutableProjectDescription();
                description.setApplicationName(appModel.name()); //TODO not uniquely mapped
                description.setArtifactId(appModel.artifactId());
                description.setBaseDirectory(baseDirectory.toAbsolutePath().toString());
                description.setBuildSystem(getBuildSystem("maven-project", metadata));
                description.setDescription(appModel.description());
                description.setGroupId(appModel.groupId());
                description.setLanguage(Language.forId("java", "21"));
                description.setName(appModel.name());
                description.setPackageName(appModel.packageName());
                description.setPackaging(Packaging.forId("jar"));
                description.setPlatformVersion(Version.parse("3.4.1"));
                description.setVersion(appModel.version());
                //TODO allow for dependencies to be declared
//                resolvedDependencies.forEach((dependency) -> description.addDependency(dependency.getId(),
//                        MetadataBuildItemMapper.toDependency(dependency)));
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
