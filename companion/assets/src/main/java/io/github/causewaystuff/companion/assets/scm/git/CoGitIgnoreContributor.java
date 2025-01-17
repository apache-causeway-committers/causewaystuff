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
package io.github.causewaystuff.companion.assets.scm.git;

import java.io.IOException;
import java.nio.file.Path;

import io.github.causewaystuff.companion.assets.AssetUtils;
import io.spring.initializr.generator.project.contributor.SingleResourceProjectContributor;
import io.spring.initializr.generator.spring.scm.git.GitIgnore;
import io.spring.initializr.generator.spring.scm.git.GitIgnoreContributor;

/**
 * A {@link SingleResourceProjectContributor} that contributes a {@code .gitignore} file
 * to a project.
 * 
 * @see GitIgnoreContributor
 */
class CoGitIgnoreContributor extends GitIgnoreContributor {

    static final String GENERATED_NOTICE = "### GENERATED NOTICE ###";
    
    public CoGitIgnoreContributor(GitIgnore gitIgnore) {
        super(gitIgnore);
    }
    
    @Override
    public void contribute(Path projectRoot) throws IOException {
        AssetUtils.override(
            projectRoot.resolve(".gitignore"), 
            AssetUtils.anyLineContains(GENERATED_NOTICE), 
            file->super.contribute(projectRoot));
    }

}
