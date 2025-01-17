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
package io.github.causewaystuff.companion.assets.build.maven;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.apache.maven.api.model.Model;

import org.apache.causeway.commons.collections.Can;

import lombok.SneakyThrows;

import io.github.causewaystuff.companion.assets.AssetUtils;
import io.github.causewaystuff.companion.assets.CoProjectContributor;
import io.github.causewaystuff.companion.assets.CoProjectDescription;
import io.github.causewaystuff.companion.schema.CoApplication;
import io.github.causewaystuff.companion.schema.CoModule;
import io.spring.initializr.generator.buildsystem.maven.MavenBuild;
import io.spring.initializr.generator.io.IndentingWriterFactory;
import io.spring.initializr.generator.project.contributor.ProjectContributor;
import io.spring.initializr.generator.spring.build.maven.MavenBuildProjectContributor;

/**
 * {@link ProjectContributor} to contribute the files for a {@link MavenBuild}.
 * 
 * @see MavenBuildProjectContributor
 */
record MavenMultiModuleBuildProjectContributor(
    CoProjectDescription projectDescription,
    MavenMultiModuleBuild build, 
    IndentingWriterFactory indentingWriterFactory
    ) implements CoProjectContributor {
    
    static final String GENERATED_NOTICE = "# GENERATED NOTICE";

    @Override
    public void contributeRoot() {
        var mavenModel = createRootModel(projectDescription.getApplicationModel());
        write(mavenModel, projectDescription.getProjectRoot());
    }

    @Override @SneakyThrows
    public void contributeModule(CoModule coModule) {
        var modRoot = projectDescription.moduleRoot(coModule);
        Files.createDirectories(modRoot.resolve("src/main/java"));
        var mavenModel = createModuleModel(projectDescription.getApplicationModel(), coModule);
        write(mavenModel, modRoot);
    }
    
    // -- HELPER
    
    @SneakyThrows
    private void write(Model mavenModel, Path dir) {
        AssetUtils.override(
            dir.resolve("pom.xml"), 
            AssetUtils.anyLineContains(GENERATED_NOTICE), 
            file->{
                
                var generatedNoticeLines = Can.of(
                    GENERATED_NOTICE,
                    "# this file was generated by " + MavenMultiModuleBuildProjectContributor.class.getName(),
                    "# to prevent this generator from overriding any changes you make, delete the GENERATED NOTICE line");
                
                new PomWriter(mavenModel, projectDescription.getLicenseHeader(), generatedNoticeLines)
                    .write(file);
            });
    }

    private org.apache.maven.model.Model createEmptyModel() {
        var mavenModel = new org.apache.maven.model.Model();
        mavenModel.setModelVersion("4.0.0");
        return mavenModel;
    }
    
    private Model createRootModel(CoApplication applicationModel) {
        var mavenModel = createEmptyModel();
        mavenModel.setGroupId(applicationModel.groupId());
        mavenModel.setArtifactId(applicationModel.artifactId());
        mavenModel.setVersion(applicationModel.version());
        mavenModel.setName(applicationModel.name());
        mavenModel.setDescription(applicationModel.description());
        mavenModel.setPackaging("pom");
        
        mavenModel.setProperties(rootProperties());
        mavenModel.setModules(applicationModel.modules().stream().map(CoModule::id).toList());
        
        mavenModel.setDependencies(_Maven.dependencies(build.dependencies()));
        mavenModel.setDependencyManagement(_Maven.dependencyManagement(build.boms()));

//        MavenBuildSettings settings = build.getSettings();
//        var bw = new MavenBuildWriter();
//        
//          writeParent(writer, build);
//          writeProjectCoordinates(writer, settings);
//          writePackaging(writer, settings);
//          writeProjectName(writer, settings);
//          writeCollectionElement(writer, "licenses", settings.getLicenses(), this::writeLicense);
//          writeCollectionElement(writer, "developers", settings.getDevelopers(), this::writeDeveloper);
//          writeScm(writer, settings.getScm());
//          writeProperties(writer, build.properties());

//          writeBuild(writer, build);
//          writeRepositories(writer, build.repositories(), build.pluginRepositories());
//          writeDistributionManagement(writer, build.getDistributionManagement());
//          writeProfiles(writer, build);
        
        return mavenModel.getDelegate(); 
    }
    
    private Properties rootProperties() {
        var props = new Properties();
        props.put("project.build.sourceEncoding", "UTF-8");
        props.put("maven.compiler.release", "23");
        props.put("maven.compiler.arg", "-parameters");
        
        props.put("causeway.version", "3.0.0-SNAPSHOT");
        props.put("causewaystuff.version", "1.0.0-SNAPSHOT");
        return props;
    }

    private org.apache.maven.api.model.Model createModuleModel(
            CoApplication applicationModel,
            CoModule coModule) {
        var mavenModel = createEmptyModel();
        mavenModel.setArtifactId(applicationModel.artifactId() + "-" + coModule.id());
        mavenModel.setName(coModule.name());
        mavenModel.setDescription(coModule.description());
        mavenModel.setParent(createRootModelAsParentRef(applicationModel));
        mavenModel.setPackaging("jar");
        return mavenModel.getDelegate(); 
    }
    
    private org.apache.maven.model.Parent createRootModelAsParentRef(CoApplication applicationModel) {
        var rootAsParent = new org.apache.maven.model.Parent();
        rootAsParent.setGroupId(applicationModel.groupId());
        rootAsParent.setArtifactId(applicationModel.artifactId());
        rootAsParent.setVersion(applicationModel.version());
        return rootAsParent; 
    }

}
