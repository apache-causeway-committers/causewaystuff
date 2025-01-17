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

import lombok.Getter;

import io.github.causewaystuff.companion.schema.CoApplication;
import io.github.causewaystuff.companion.schema.LicenseHeader;
import io.github.causewaystuff.companion.schema.Persistence;
import io.spring.initializr.generator.language.Language;
import io.spring.initializr.generator.packaging.Packaging;
import io.spring.initializr.generator.project.MutableProjectDescription;
import io.spring.initializr.generator.version.Version;

public class CoMutableProjectDescription 
extends MutableProjectDescription 
implements CoProjectDescription {

    @Getter private final CoApplication applicationModel;
    @Getter private final Path projectRoot;
    @Getter private final LicenseHeader licenseHeader;

    public CoMutableProjectDescription(Path projectRoot, CoApplication appModel) {
        this.applicationModel = appModel;
        this.projectRoot = projectRoot;
        this.licenseHeader = appModel.license();
        
        super.setApplicationName(appModel.name()); //TODO not uniquely mapped
        super.setArtifactId(appModel.artifactId());
        super.setBaseDirectory(projectRoot.toAbsolutePath().toString());
        //super.setBuildSystem(getBuildSystem("maven-project", metadata));
        super.setDescription(appModel.description());
        super.setGroupId(appModel.groupId());
        super.setLanguage(Language.forId("java", "21"));
        super.setName(appModel.name());
        super.setPackageName(appModel.packageName());
        super.setPackaging(Packaging.forId("pom"));
        super.setPlatformVersion(Version.parse("3.4.1"));
        super.setVersion(appModel.version());
        
        //TODO allow for dependencies to be declared
//        resolvedDependencies.forEach((dependency) -> description.addDependency(dependency.getId(),
//                MetadataBuildItemMapper.toDependency(dependency)));
    }

    @Override
    public Persistence getPersistenceMechanism() {
        return applicationModel.persistence();
    }
    
}
