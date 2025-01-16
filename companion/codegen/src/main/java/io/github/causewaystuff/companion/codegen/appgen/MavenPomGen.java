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
package io.github.causewaystuff.companion.codegen.appgen;

import java.io.File;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.apache.maven.model.v4.MavenStaxWriter;

import org.apache.causeway.commons.io.FileUtils;
import org.apache.causeway.commons.io.TextUtils;

import lombok.SneakyThrows;

import io.github.causewaystuff.companion.schema.CoApplication;
import io.github.causewaystuff.companion.schema.CoModule;
import io.github.causewaystuff.companion.schema.LicenseHeader;

/**
 * Generates the application's Maven POM files.
 */
public record MavenPomGen() implements CoGenerator{

    @Override
    public void onApplication(Context context) {
        var mavenModel = createRootModel(context.appModel());
        new PomWriter(mavenModel).write(context.license(), context.projectRoot());
    }

    @Override
    public void onModule(Context context, CoModule coModule) {
        var modRoot = context.moduleRoot(coModule);
        FileUtils.makeDir(new File(modRoot, "src/main/java"));
            
        var mavenModel = createModuleModel(context.appModel(), coModule);
        new PomWriter(mavenModel).write(context.license(), modRoot);
    }
    
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
        void write(LicenseHeader license, File destinationDir) {
            var destFile = new File(destinationDir, "pom.xml");
            
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
            
            var generatedMarker = "GENERATED by " + MavenPomGen.class.getName();
            
            var headComment = switch (license) {
                case ASF_V2 -> license.text() + "\n\n" + generatedMarker;
                case NONE -> generatedMarker;
            };
            
            lines = lines.add(1, "<!--\n%s\n-->".formatted(headComment));
            
            TextUtils.writeLinesToFile(lines, destFile, StandardCharsets.UTF_8);
        }
    }

}
