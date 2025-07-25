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
package io.github.causewaystuff.tooling.projectmodel.maven;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Repository;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.model.building.UrlModelSource;
import org.apache.maven.model.resolution.InvalidRepositoryException;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;

import org.apache.causeway.commons.io.FileUtils;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SimpleModelResolver implements ModelResolver {

    //non interpolated models
    private final Map<String, Model> projectPomCatalog = new HashMap<>();
    private final Map<String, String> pathToArtifactMap = new HashMap<>();
    private final Map<String, Repository> repositories = new LinkedHashMap<>();
    private final Set<String> directoriesToIgnore = new HashSet<>();

    @Getter private Model rootModel;

    public SimpleModelResolver(final File projectRoot) {
        directoriesToIgnore.add("target");
        directoriesToIgnore.add("target-ide");
        populateCatalogs(projectRoot);
    }

    @Override
    public ModelSource resolveModel(final String groupId, final String artifactId, final String version)
            throws UnresolvableModelException {

        var key = String.format("%s:%s:%s", groupId, artifactId, version);

        log.info("resolveModel {}", key);

        try {

            var pomModel = projectPomCatalog.get(key);
            if(pomModel!=null) {
                return new FileModelSource(pomModel.getPomPath().toFile());
            }

            if(repositories.size()==0) {
                throw new RuntimeException("no repo registered");
            }

            for(var entry : repositories.entrySet()) {
                var repo = entry.getValue();

                var pomUrl = new URI(String.format("%s/%s/%s/%s/%s-%s.pom",
                        repo.getUrl(),
                        groupId.replace('.', '/'),
                        artifactId,
                        version,
                        artifactId,
                        version))
                    .toURL();

                try {
                    var urlConn = pomUrl.openConnection();
                    var is = urlConn.getInputStream(); // throws if not found
                    is.close();
                    return new UrlModelSource(pomUrl);
                } catch (Exception e) {
                    // try next
                }
            }

            log.warn("No repo found that serves {}", key);

            throw new RuntimeException(String.format("No repo found that serves %s", key));

        } catch (Exception ex) {
            throw new UnresolvableModelException(ex.getMessage(), groupId, artifactId, version);
        }
    }

    @Override
    public ModelSource resolveModel(final Parent parent) throws UnresolvableModelException {
        log.info("resolveModel-parent");
        return resolveModel(parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
    }

    @Override
    public ModelSource resolveModel(final Dependency dependency) throws UnresolvableModelException {
        log.info("resolveModel-dependency");
        return resolveModel(
                dependency.getGroupId(),
                dependency.getArtifactId(),
                dependency.getVersion());
    }

    @Override
    public void addRepository(final Repository repository) throws InvalidRepositoryException {
        log.info("adding repository {}", repository.getUrl());
        repositories.put(repository.getId(), repository);
    }

    @Override
    public void addRepository(final Repository repository, final boolean replace) throws InvalidRepositoryException {
        addRepository(repository);
    }

    @Override
    public ModelResolver newCopy() {
        return this;
    }

    public Model lookupCatalogForSubmoduleOf(final Model mavenProj, final String realtivePath) {

        final String localPath;
        try {
            localPath = new File(mavenProj.getPomPath().toFile().getParentFile(), realtivePath)
                    .getCanonicalPath();
        } catch (Exception e) {
            log.error("cannot resolve local path {} relative to {}", realtivePath, mavenProj.getPomPath().toFile().getParent(), e);
            return null;
        }

        var artifactKey = pathToArtifactMap.get(localPath);
        if(artifactKey==null) {
            return null;
        }

        var subProj = projectPomCatalog.get(artifactKey);
        if(subProj==null) {
            return null;
        }
        return subProj;
    }

    @SneakyThrows
    private void populateCatalogs(final File projectRoot) {

        var localRootPath = projectRoot.getCanonicalPath();

        FileUtils.searchFiles(projectRoot,
                file->
                    !file.getName().startsWith(".")
                    && !directoriesToIgnore.contains(file.getName()),
                file->"pom.xml".equals(file.getName()))
        .stream()
        .forEach(pomFile->{

            var model = MavenModelFactory.readModel(pomFile);

            try {

                var localPath = pomFile.getParentFile().getCanonicalPath();

                if(localPath.equals(localRootPath)) {
                    rootModel = model;
                }

                var artifactKey = MavenModelFactory.readArtifactKey(model);
                if(artifactKey!=null) {
                    log.debug("found {} at {}", artifactKey, model.getPomFile().getAbsolutePath());
                    projectPomCatalog.put(artifactKey, model);
                    pathToArtifactMap.put(localPath, artifactKey);
                }

            } catch (Exception e) {
                log.error("cannot resolve local path {}", pomFile.getParentFile().getAbsolutePath(), e);
            }
        });

    }

}
