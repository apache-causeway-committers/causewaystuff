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

import java.nio.file.Files;

import org.springframework.core.Ordered;

import lombok.SneakyThrows;

import io.github.causewaystuff.companion.schema.CoModule;

/**
 * Generates the application's project folders.
 */
record MakedirProjectContributor(CoProjectDescription projectDescription) implements CoProjectContributor {

    @Override @SneakyThrows
    public void contributeRoot() {
        Files.createDirectories(projectDescription.getProjectRoot());
    }

    @Override @SneakyThrows
    public void contributeModule(CoModule coModule) {
        var modRoot = projectDescription.moduleRoot(coModule);
        Files.createDirectories(modRoot);
    }
    
    // directory structure needs to come first
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

}
