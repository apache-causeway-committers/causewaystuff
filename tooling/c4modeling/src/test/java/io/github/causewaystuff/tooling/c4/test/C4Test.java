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
package io.github.causewaystuff.tooling.c4.test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import io.github.causewaystuff.tooling.structurizr.Workspace;
import io.github.causewaystuff.tooling.structurizr.export.plantuml.StructurizrPlantUMLExporter;
import io.github.causewaystuff.tooling.structurizr.model.Person;
import io.github.causewaystuff.tooling.structurizr.model.SoftwareSystem;
import io.github.causewaystuff.tooling.structurizr.view.SystemContextView;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.apache.causeway.commons.internal.base._Text;
import org.apache.causeway.commons.io.TextUtils;

import io.github.causewaystuff.tooling.c4.C4;

class C4Test {

    @BeforeEach
    void setUp() throws Exception {
    }

    @AfterEach
    void tearDown() throws Exception {
    }

    /**
     * see https://www.baeldung.com/structurizr
     */
    @Test
    void testStructurizr_native() throws IOException {

        // First, we need to create a Workspace and a Model:

        var workspace = new Workspace("Payment Gateway", "Payment Gateway");
        var model = workspace.getModel();

        // We also define a user and two software systems within that model:

        Person user = model.addPerson("Merchant", "Merchant");
        SoftwareSystem paymentTerminal = model
                .addSoftwareSystem("Payment Terminal", "Payment Terminal");
        user.uses(paymentTerminal, "Makes payment");
        SoftwareSystem fraudDetector = model
                .addSoftwareSystem("Fraud Detector", "Fraud Detector");
        paymentTerminal.uses(fraudDetector, "Obtains fraud score");

        // Now that our system is defined, we can create a view
        // Here we created a view that includes all software systems and persons.

        var viewSet = workspace.getViews();

        SystemContextView contextView = viewSet
                .createSystemContextView(paymentTerminal, "context", "Payment Gateway Diagram");
        contextView.addAllSoftwareSystems();
        contextView.addAllPeople();

        // Now the view needs to be rendered.

        var sb = new StringBuffer();
        var plantUMLExporter = new StructurizrPlantUMLExporter();
        plantUMLExporter.export(workspace).forEach(diagram->sb.append(diagram.getDefinition()));
        var plantUmlSource = sb.toString();

        dump(plantUmlSource);

        _Text.assertTextEquals(
                TextUtils.readLinesFromResource(this.getClass(), "baeldung-example-v1.puml", StandardCharsets.UTF_8),
                plantUmlSource);
    }

    /**
     * see https://www.baeldung.com/structurizr
     */
    @Test
    void testStructurizr_usingFactory() throws IOException {

        var c4 = C4.of("Payment Gateway", "Payment Gateway");

        // We also define a user and two software systems within that model:

        Person user = c4.person("Merchant", "Merchant");
        SoftwareSystem paymentTerminal = c4.softwareSystem("Payment Terminal", "Payment Terminal");
        SoftwareSystem fraudDetector = c4.softwareSystem("Fraud Detector", "Fraud Detector");

        user.uses(paymentTerminal, "Makes payment");
        paymentTerminal.uses(fraudDetector, "Obtains fraud score");

        // Now that our system is defined, we can create a view
        // Here we created a view that includes all software systems and persons.

        SystemContextView contextView = c4.systemContextView(paymentTerminal, "context", "Payment Gateway Diagram");
        contextView.addAllSoftwareSystems();
        contextView.addAllPeople();

        // Now the view needs to be rendered.

        var plantUmlSource = c4.toPlantUML(contextView);

        dump(plantUmlSource);

        _Text.assertTextEquals(
                TextUtils.readLinesFromResource(this.getClass(), "baeldung-example-v2.puml", StandardCharsets.UTF_8),
                plantUmlSource);

    }

    // -- HELPER

    // debug
    private void dump(final String plantUmlSource){
        // System.err.println("---");
        // System.out.println(plantUmlSource);
        // System.err.println("---");
    }

}
