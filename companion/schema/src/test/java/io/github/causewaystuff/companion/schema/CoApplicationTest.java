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
package io.github.causewaystuff.companion.schema;

import java.util.EnumSet;
import java.util.List;

import org.approvaltests.Approvals;
import org.approvaltests.reporters.DiffReporter;
import org.approvaltests.reporters.UseReporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.causeway.testing.unittestsupport.applib.util.ApprovalUtils;

class CoApplicationTest {
    
    CoApplication app;
    
    @BeforeEach
    void setup(){
        var idField = new CoField("id", "unique identifier", Long.class, EnumSet.of(CoFieldFlag.PRIMARY_KEY));
        var nameField = new CoField("name", "the name of this entity", String.class, EnumSet.noneOf(CoFieldFlag.class));
        
        this.app = new CoApplication(
            "io.example.petclinic", "petclinic", "1.0.0-SNAPSHOT",
            "Petclinic", "Petclinic sample application.",
            "io.example.petclinic",
            Persistence.JPA,
            LicenseHeader.ASF_V2,
            List.of(
                new CoModule("petowner", "Pet Owner Module", "petclinic's pet owner module", List.of(
                    new CoEntity("dom.petowner", "Pet", "an individual pet, known by the Petclinic", List.of(
                        idField, nameField)),
                    new CoEntity("dom.petowner", "PetOwner", "an individual pet owner, known by the Petclinic", List.of(
                        idField, nameField))
                    )),
                new CoModule("visit", "Visit Module", "petclinic's visit module", List.of(
                    new CoEntity("dom.visit", "Visit", "a specivic visit to the petclinic", List.of(
                        idField, nameField))
                    ))
            ));
    }
    
    @Test
    @UseReporter(DiffReporter.class)
    void petclinic() {
        var yaml = app.toYaml();
        Approvals.verify(yaml, ApprovalUtils.ignoreLineEndings()
            .forFile().withExtension(".yaml"));
    }
    
    @Test
    void roundtrip() {
        var yaml = app.toYaml();
        assertEquals(
                app,
                CoApplication.fromYaml(yaml));
    }
    
}
