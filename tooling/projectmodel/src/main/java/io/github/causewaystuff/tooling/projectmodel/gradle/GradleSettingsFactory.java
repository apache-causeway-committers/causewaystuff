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

import org.apache.causeway.commons.internal.base._Strings;
import org.apache.causeway.commons.internal.exceptions._Exceptions;
import org.apache.causeway.commons.io.FileUtils;

import lombok.NonNull;

import io.github.causewaystuff.tooling.projectmodel.ArtifactCoordinates;
import io.github.causewaystuff.tooling.projectmodel.ProjectNode;
import io.github.causewaystuff.tooling.projectmodel.ProjectNodeFactory;

public class GradleSettingsFactory {

    public static GradleSettings generateFromMaven(final File projRootFolder, final String rootProjectName) {
        var projTree = ProjectNodeFactory.maven(projRootFolder);
        return generateFromMaven(projTree, rootProjectName);
    }

    public static GradleSettings generateFromMaven(final ProjectNode projTree, final String rootProjectName) {

        var rootPath = FileUtils.canonicalPath(projTree.getProjectDirectory())
                .orElseThrow(()->_Exceptions.unrecoverable("cannot resolve project root"));

        var gradleSettings = new GradleSettings(rootProjectName);
        var folderByArtifactKey = gradleSettings.getBuildArtifactsByArtifactKey();

        projTree.depthFirst(projModel -> {
            folderByArtifactKey.put(projModel.getArtifactCoordinates(), gradleBuildArtifactFor(projModel, rootPath));
        });

        return gradleSettings;
    }

    // -- HELPER

    private static GradleBuildArtifact gradleBuildArtifactFor(final ProjectNode projModel, final String rootPath) {
        var name = toCanonicalBuildName(projModel.getArtifactCoordinates());
        var realtivePath = toCanonicalRelativePath(projModel, rootPath);
        return GradleBuildArtifact.of(name, realtivePath, projModel.getProjectDirectory());
    }

    private static String toCanonicalBuildName(final @NonNull ArtifactCoordinates artifactKey) {
        return String.format(":%s:%s", artifactKey.getGroupId(), artifactKey.getArtifactId());
    }

    private static String toCanonicalRelativePath(final ProjectNode projModel, final String rootPath) {
        var canonicalProjDir = FileUtils.canonicalPath(projModel.getProjectDirectory())
                .orElseThrow(()->_Exceptions.unrecoverable("cannot resolve relative path"));

        var relativePath = FileUtils.toRelativePath(rootPath, canonicalProjDir);
        return _Strings.prefix(relativePath.replace('\\', '/'), "/");
    }

}
