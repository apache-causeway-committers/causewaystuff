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
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import io.github.causewaystuff.companion.assets.CoProjectContributor;
import io.github.causewaystuff.companion.assets.CoProjectDescription;
import io.spring.initializr.generator.buildsystem.BuildItemResolver;
import io.spring.initializr.generator.buildsystem.maven.MavenBuildSystem;
import io.spring.initializr.generator.condition.ConditionalOnBuildSystem;
import io.spring.initializr.generator.condition.ConditionalOnPackaging;
import io.spring.initializr.generator.io.IndentingWriterFactory;
import io.spring.initializr.generator.project.ProjectGenerationConfiguration;
import io.spring.initializr.generator.spring.build.BuildCustomizer;
import io.spring.initializr.generator.spring.util.LambdaSafe;

/**
 * Configuration for contributions specific to the generation of a project that will use
 * Maven as its build system.
 * 
 * @see io.spring.initializr.generator.spring.build.maven.MavenProjectGenerationConfiguration 
 */
@ProjectGenerationConfiguration
@ConditionalOnBuildSystem(MavenBuildSystem.ID)
public class MavenProjectGenerationConfiguration {
    
    @Bean
    @Primary
    @ConditionalOnPackaging(PomPackaging.ID)
    public MavenMultiModuleBuild mavenMultiModuleBuild(ObjectProvider<BuildItemResolver> buildItemResolver,
            ObjectProvider<BuildCustomizer<?>> buildCustomizers) {
        return createBuild(buildItemResolver.getIfAvailable(),
                buildCustomizers.orderedStream().collect(Collectors.toList()));
    }

    @SuppressWarnings("unchecked")
    private MavenMultiModuleBuild createBuild(BuildItemResolver buildItemResolver, List<BuildCustomizer<?>> buildCustomizers) {
        MavenMultiModuleBuild build = (buildItemResolver != null) 
            ? new MavenMultiModuleBuild(buildItemResolver)
            : new MavenMultiModuleBuild();
        LambdaSafe.callbacks(BuildCustomizer.class, buildCustomizers, build)
            .invoke((customizer) -> customizer.customize(build));
        return build;
    }
    
    @Bean
    public CoProjectContributor mavenMultiModuleBuildProjectContributor(
            CoProjectDescription projectDescription,
            MavenMultiModuleBuild build,
            IndentingWriterFactory indentingWriterFactory) {
        return new MavenMultiModuleBuildProjectContributor(projectDescription, build, indentingWriterFactory);
    }
    
    @Bean
    @ConditionalOnPackaging(PomPackaging.ID)
    public BuildCustomizer<MavenMultiModuleBuild> mavenPomPackagingConfigurer() {
        return (build) -> build.settings()
            .packaging("pom");
    }
}
