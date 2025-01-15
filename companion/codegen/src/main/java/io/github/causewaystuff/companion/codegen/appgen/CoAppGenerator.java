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
package io.github.causewaystuff.companion.codegen.appgen;

import java.io.File;
import java.util.List;

import io.github.causewaystuff.companion.schema.CoApplication;
import io.github.causewaystuff.companion.schema.LicenseHeader;
import io.github.causewaystuff.companion.schema.Persistence;

public record CoAppGenerator(
    CoGenerator.Context context,
    List<CoGenerator> generators) {
    
    public CoAppGenerator(
        CoApplication appModel,
        File projectRoot,
        Persistence persistence,
        LicenseHeader license) {
        this(
            new CoGenerator.Context(appModel, projectRoot, persistence, license), 
            List.of(
                new MakeDirGen(),
                new MavenPomGen()));
    }
    
    public void generate() {
        generators.forEach(gen->{
            gen.onApplication(context);
            context.appModel().modules().forEach(mod->{
                gen.onModule(context, mod);
            });
        });
    }
    
}
