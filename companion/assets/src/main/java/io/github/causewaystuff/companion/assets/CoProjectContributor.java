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
import java.nio.file.Path;

import org.apache.causeway.commons.internal.assertions._Assert;

import io.github.causewaystuff.companion.schema.CoModule;
import io.spring.initializr.generator.project.contributor.ProjectContributor;

public interface CoProjectContributor extends ProjectContributor {
    
    CoProjectDescription projectDescription();
    
    void contributeRoot();
    void contributeModule(CoModule coModule);
    
    @Override
    default void contribute(Path projectRoot) throws IOException {
        _Assert.assertEquals(projectDescription().getProjectRoot(), projectRoot);
        contributeRoot();
        projectDescription().getApplicationModel().modules().forEach(mod->{
            contributeModule(mod);
        });
    }
}
