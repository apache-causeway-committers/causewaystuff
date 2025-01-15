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

import io.github.causewaystuff.companion.schema.CoApplication;
import io.github.causewaystuff.companion.schema.CoModule;
import io.github.causewaystuff.companion.schema.LicenseHeader;
import io.github.causewaystuff.companion.schema.Persistence;

public interface CoGenerator {
    
    public record Context(
        CoApplication appModel,
        File projectRoot,
        Persistence persistence,
        LicenseHeader license) {

        public File moduleRoot(CoModule coModule) {
            return new File(projectRoot(), coModule.id());
        }
    }
    
    void onApplication(Context context);
    void onModule(Context context, CoModule coModule);
    
}
