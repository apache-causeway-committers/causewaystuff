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
package io.github.causewaystuff.tooling.schemagen.test;

import java.io.IOException;

import org.approvaltests.Approvals;
import org.approvaltests.reporters.DiffReporter;
import org.approvaltests.reporters.UseReporter;
import org.junit.jupiter.api.Test;

import org.apache.causeway.testing.unittestsupport.applib.util.ApprovalUtils;

import io.github.causewaystuff.companion.schema.CoApplication;
import io.github.causewaystuff.tooling.schemagen.SchemaGeneratorUtils;

class CompanionSchemaTest {

    @Test
    @UseReporter(DiffReporter.class)
    void coApplication() throws IOException  {
        var generator = SchemaGeneratorUtils.schemaGeneratorWithRecordTypeSupport();
        var jsonSchema = generator.generateSchema(CoApplication.class);
        var json = SchemaGeneratorUtils.prettyPrint(jsonSchema);
        
        Approvals.verify(json, ApprovalUtils.ignoreLineEndings()
            .forFile().withExtension(".json"));
    }

}
