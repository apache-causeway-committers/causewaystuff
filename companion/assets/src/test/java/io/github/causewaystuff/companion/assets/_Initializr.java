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

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.core.io.ByteArrayResource;

import lombok.experimental.UtilityClass;

import io.spring.initializr.metadata.InitializrMetadata;
import io.spring.initializr.metadata.InitializrMetadataBuilder;
import io.spring.initializr.metadata.InitializrProperties;

@UtilityClass
class _Initializr {

    InitializrMetadata initializrMetadata() {
        return InitializrMetadataBuilder.fromInitializrProperties(loadInitializrProperties()).build();
    }
    
    // -- HELPER
    
    private String config = """
        initializr:
          javaVersions:
            - id: 21
              default: true
            - id: 23
              default: false
          languages:
            - name: Java
              id: java
              default: true
          packagings:
            - name: Jar
              id: jar
              default: true
          bootVersions:
            - id: 3.4.2-SNAPSHOT
              name: 3.4.2 (SNAPSHOT)
              default: false
            - id: 3.4.1.RELEASE
              name: 3.4.1
              default: true
          types:
            - name: Maven Project
              id: maven-project
              description: Generate a Maven based project archive
              tags:
                build: maven
                format: project
              default: true
              action: /starter.zip
            - name: Gradle Project
              id: gradle-project
              description: Generate a Gradle based project archive
              tags:
                build: gradle
                format: project
              default: false
              action: /starter.zip
        """;

    private InitializrProperties loadInitializrProperties() {
        var factoryBean = new YamlPropertiesFactoryBean();
        factoryBean.setResources(new ByteArrayResource(config.getBytes(StandardCharsets.UTF_8)));

        var properties = factoryBean.getObject();
        var propertySource = new MapConfigurationPropertySource(properties);
        var binder = new Binder(propertySource);

        return binder.bind("initializr", InitializrProperties.class).get();
    }
}
