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

import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.apache.maven.model.v4.MavenStaxWriter;

import org.apache.causeway.commons.io.TextUtils;

import lombok.SneakyThrows;

import io.github.causewaystuff.companion.assets.CoProjectContributor;
import io.github.causewaystuff.companion.assets.CoProjectDescription;
import io.github.causewaystuff.companion.schema.CoApplication;
import io.github.causewaystuff.companion.schema.CoModule;
import io.github.causewaystuff.companion.schema.LicenseHeader;
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

    @Override
    public void contributeRoot() {
        var mavenModel = createRootModel(projectDescription.getApplicationModel());
        new PomWriter(mavenModel).write(projectDescription.getLicenseHeader(), projectDescription.getProjectRoot());
    }

    @Override @SneakyThrows
    public void contributeModule(CoModule coModule) {
        var modRoot = projectDescription.moduleRoot(coModule);
        Files.createDirectories(modRoot.resolve("src/main/java"));
        var mavenModel = createModuleModel(projectDescription.getApplicationModel(), coModule);
        new PomWriter(mavenModel).write(projectDescription.getLicenseHeader(), modRoot);
    }
    
//    @Override @Deprecated
//    public void contribute(Path projectRoot) throws IOException {
//        Path pomFile = Files.createFile(projectRoot.resolve("pom.xml"));
//        try (IndentingWriter writer = this.indentingWriterFactory.createIndentingWriter("maven", Files.newBufferedWriter(pomFile))) {
//            writeTo(writer, this.build);
//        }
//    }
    
//    /**
//     * Write a {@linkplain MavenBuild pom.xml} using the specified
//     * {@linkplain IndentingWriter writer}.
//     * @param writer the writer to use
//     * @param build the maven build to write
//     */
//    public void writeTo(IndentingWriter writer, MavenMultiModuleBuild build) {
//        MavenBuildSettings settings = build.getSettings();
//        var delegate = new MavenBuildWriter();
//        delegate.writeTo(writer, build);
//        
////        writeProject(writer, () -> {
////            writeParent(writer, build);
////            writeProjectCoordinates(writer, settings);
////            writePackaging(writer, settings);
////            writeProjectName(writer, settings);
////            writeCollectionElement(writer, "licenses", settings.getLicenses(), this::writeLicense);
////            writeCollectionElement(writer, "developers", settings.getDevelopers(), this::writeDeveloper);
////            writeScm(writer, settings.getScm());
////            writeProperties(writer, build.properties());
////            writeDependencies(writer, build.dependencies());
////            writeDependencyManagement(writer, build.boms());
////            writeBuild(writer, build);
////            writeRepositories(writer, build.repositories(), build.pluginRepositories());
////            writeDistributionManagement(writer, build.getDistributionManagement());
////            writeProfiles(writer, build);
////        });
//    }
    
    // -- HELPER
    
    private org.apache.maven.model.Model createEmptyModel() {
        var mavenModel = new org.apache.maven.model.Model();
        mavenModel.setModelVersion("4.0.0");
        return mavenModel;
    }
    
    private org.apache.maven.model.Model createRootModel(CoApplication applicationModel) {
        var mavenModel = createEmptyModel();
        mavenModel.setGroupId(applicationModel.groupId());
        mavenModel.setArtifactId(applicationModel.artifactId());
        mavenModel.setVersion(applicationModel.version());
        mavenModel.setName(applicationModel.name());
        mavenModel.setDescription(applicationModel.description());
        mavenModel.setPackaging("pom");
        
        mavenModel.setProperties(rootProperties());
        mavenModel.setModules(applicationModel.modules().stream().map(CoModule::id).toList());
        return mavenModel; 
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

    private org.apache.maven.model.Model createModuleModel(
            CoApplication applicationModel,
            CoModule coModule) {
        var mavenModel = createEmptyModel();
        mavenModel.setArtifactId(applicationModel.artifactId() + "-" + coModule.id());
        mavenModel.setName(coModule.name());
        mavenModel.setDescription(coModule.description());
        mavenModel.setParent(createRootModelAsParentRef(applicationModel));
        mavenModel.setPackaging("jar");
        return mavenModel; 
    }
    
    private org.apache.maven.model.Parent createRootModelAsParentRef(CoApplication applicationModel) {
        var rootAsParent = new org.apache.maven.model.Parent();
        rootAsParent.setGroupId(applicationModel.groupId());
        rootAsParent.setArtifactId(applicationModel.artifactId());
        rootAsParent.setVersion(applicationModel.version());
        return rootAsParent; 
    }
    
    private record PomWriter(org.apache.maven.api.model.Model model) {
        
        PomWriter(org.apache.maven.model.Model model) {
            this(model.getDelegate());
        }
        
        @SneakyThrows
        void write(LicenseHeader license, Path destinationDir) {
            var destFile = destinationDir.resolve("pom.xml").toFile();
            
            var isVetoOverride = destFile.exists()
                && !TextUtils.readLinesFromFile(destFile, StandardCharsets.UTF_8)
                    .stream()
                    .limit(LicenseHeader.MAX_LINES + 20)
                    .anyMatch(line->line.trim().startsWith("GENERATED "));

            if(isVetoOverride) return;
            if(destFile.exists()) destFile.delete();

            // head comment
            var sw = new StringWriter();
            new MavenStaxWriter().write((Writer)sw, model);
            var lines = TextUtils.readLines(sw.toString());
            
            var generatedMarker = "GENERATED by " + MavenMultiModuleBuildProjectContributor.class.getName();
            
            var headComment = switch (license) {
                case ASF_V2 -> license.text() + "\n\n" + generatedMarker;
                case NONE -> generatedMarker;
            };
            
            lines = lines.add(1, "<!--\n%s\n-->".formatted(headComment));
            
            TextUtils.writeLinesToFile(lines, destFile, StandardCharsets.UTF_8);
        }
    }

}
