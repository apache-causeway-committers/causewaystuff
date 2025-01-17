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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.apache.causeway.testing.unittestsupport.applib.annotations.DisabledIfRunningWithSurefire;

import io.github.causewaystuff.commons.base.types.ResourceFolder;
import io.github.causewaystuff.companion.assets.build.maven.MavenMultiModuleBuild;
import io.github.causewaystuff.companion.assets.build.maven.MavenProjectGenerationConfiguration;
import io.github.causewaystuff.companion.schema.CoApplication;
import io.spring.initializr.generator.io.IndentingWriterFactory;

class AssetGenerationTest {

    private Path projdir = Path.of("d:/tmp/companion/petclinic"); //TODO use FileUtils.tempDir("causewaystuff-companion-test")
    
    private IndentingWriterFactory indentingWriterFactory;
    
    private CoProjectDescription projDescription;
    private CoProjectContributor makedirProjectContributor;
    private CoProjectContributor mavenBuildProjectContributor;
    
    @BeforeEach
    void setup() throws IOException {
        
        this.indentingWriterFactory = new CoAssetGenerationConfiguration().indentingWriterFactory();
        
        var schemaFile = ResourceFolder.testResourceRoot().relativeFile("petclinic/companion-app-petclinic.yaml").toPath();
        var yaml = Files.readString(schemaFile);
        this.projDescription = new CoMutableProjectDescription(
            projdir, 
            CoApplication.fromYaml(yaml), 
            _Initializr.initializrMetadata());
        
        this.makedirProjectContributor = new CoAssetGenerationConfiguration().makedirProjectContributor(projDescription);
        this.mavenBuildProjectContributor = new MavenProjectGenerationConfiguration()
            .mavenMultiModuleBuildProjectContributor(projDescription, new MavenMultiModuleBuild(), indentingWriterFactory);
    }
    
    @DisabledIfRunningWithSurefire // WIP
    @Test
    //@UseReporter(DiffReporter.class)
    void appGenerator() throws IOException {
        
        makedirProjectContributor.contribute(projdir);
        mavenBuildProjectContributor.contribute(projdir);
        
        //Approvals.verify(source);
    }

}
