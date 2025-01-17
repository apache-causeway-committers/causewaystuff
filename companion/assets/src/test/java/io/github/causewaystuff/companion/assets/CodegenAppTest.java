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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

import org.apache.causeway.testing.unittestsupport.applib.annotations.DisabledIfRunningWithSurefire;

import io.github.causewaystuff.commons.base.types.ResourceFolder;
import io.github.causewaystuff.companion.schema.CoApplication;
import io.github.causewaystuff.companion.schema.LicenseHeader;
import io.github.causewaystuff.companion.schema.Persistence;

class CodegenAppTest {

    @DisabledIfRunningWithSurefire // WIP
    @Test
    //@UseReporter(DiffReporter.class)
    void appGenerator() throws IOException {
        
        var schemaFile = ResourceFolder.testResourceRoot().relativeFile("petclinic/companion-app-petclinic.yaml").toPath();
        var yaml = Files.readString(schemaFile);
        var projdir = new File("d:/tmp/companion/petclinic"); 
            //FileUtils.tempDir("causewaystuff-companion-test");
        var projDescription = new CoMutableProjectDescription(projdir.toPath(), CoApplication.fromYaml(yaml))
            .setLicenseHeader(LicenseHeader.ASF_V2)
            .setPersistenceMechanism(Persistence.JPA);
        
        var generator = new CoAppGenerator(projDescription);
        generator.generate();
        
        //Approvals.verify(source);
    }

}
