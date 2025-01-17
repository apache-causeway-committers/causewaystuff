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

import java.util.List;
import java.util.Set;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Exclusion;

import lombok.experimental.UtilityClass;

import io.spring.initializr.generator.buildsystem.BillOfMaterials;
import io.spring.initializr.generator.buildsystem.BomContainer;
import io.spring.initializr.generator.buildsystem.DependencyContainer;
import io.spring.initializr.generator.buildsystem.DependencyScope;
import io.spring.initializr.generator.buildsystem.maven.MavenDependency;

@UtilityClass
class _Maven {
    
    // -- TYPES

    String scopeForType(DependencyScope type) {
        if (type == null) return null;
        
        return switch (type) {
            case ANNOTATION_PROCESSOR, COMPILE, COMPILE_ONLY -> null;
            case PROVIDED_RUNTIME -> "provided";
            case RUNTIME -> "runtime";
            case TEST_COMPILE, TEST_RUNTIME -> "test";
        };
    }
    
    boolean isOptional(io.spring.initializr.generator.buildsystem.Dependency dependency) {
        if (dependency instanceof MavenDependency mavenDependency && mavenDependency.isOptional()) {
            return true;
        }
        return (dependency.getScope() == DependencyScope.ANNOTATION_PROCESSOR
                || dependency.getScope() == DependencyScope.COMPILE_ONLY);
    }
    
    Dependency dependency(io.spring.initializr.generator.buildsystem.Dependency dependency) {
        var mavenDependency = new Dependency();
        mavenDependency.setArtifactId(dependency.getArtifactId());
        mavenDependency.setClassifier(dependency.getClassifier());
        mavenDependency.setExclusions(exclusions(dependency.getExclusions()));
        mavenDependency.setGroupId(dependency.getGroupId());
        //mavenDependency.setImportedFrom();
        //mavenDependency.setLocation();
        if(isOptional(dependency)) {
            mavenDependency.setOptional(true);
        }
        mavenDependency.setScope(scopeForType(dependency.getScope()));
        //mavenDependency.setSystemPath();
        mavenDependency.setType(dependency.getType());
        
        if(dependency.getVersion()!=null) {
            mavenDependency.setVersion(dependency.getVersion().getValue());
        }
        return mavenDependency;
    }
    
    Exclusion exclusion(io.spring.initializr.generator.buildsystem.Dependency.Exclusion exclusion) {
        var mavenExclusion = new Exclusion();
        mavenExclusion.setArtifactId(exclusion.getArtifactId());
        mavenExclusion.setGroupId(exclusion.getGroupId());
        //mavenExclusion.setImportedFrom();
        //mavenExclusion.setLocation();
        return mavenExclusion;
    }
    
    Dependency dependency(BillOfMaterials bom) {
        var mavenDependency = new Dependency();
        mavenDependency.setArtifactId(bom.getArtifactId());
        //mavenDependency.setClassifier();
        //mavenDependency.setExclusions();
        mavenDependency.setGroupId(bom.getGroupId());
        //mavenDependency.setImportedFrom();
        //mavenDependency.setLocation();
        //mavenDependency.setOptional();
        //mavenDependency.setScope();
        //mavenDependency.setSystemPath();
        //mavenDependency.setType();
        if(bom.getVersion()!=null) {
            mavenDependency.setVersion(bom.getVersion().getValue());
        }
        return mavenDependency;

    }
    
    // -- CONTAINERS
    
    List<Dependency> dependencies(DependencyContainer dependencyContainer) {
        return dependencyContainer.items()
            .map(_Maven::dependency)
            .toList();
    }
    
    List<Exclusion> exclusions(Set<io.spring.initializr.generator.buildsystem.Dependency.Exclusion> exclusions) {
        return exclusions.stream()
            .map(_Maven::exclusion)
            .toList();
    }

    DependencyManagement dependencyManagement(BomContainer boms) {
        var dependencyManagement = new DependencyManagement();
        dependencyManagement.setDependencies(boms.items()
            .map(_Maven::dependency)
            .toList());
        return dependencyManagement;
    }
    
}
