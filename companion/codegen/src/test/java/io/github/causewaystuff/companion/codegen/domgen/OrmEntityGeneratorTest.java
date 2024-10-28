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
package io.github.causewaystuff.companion.codegen.domgen;

import org.junit.jupiter.api.Test;

class OrmEntityGeneratorTest {

    @Test
    void entityGen() {
        var domain = OrmSchemaExamples.examples().getElseFail(0);

        var config = DomainGenerator.Config.builder()
                .logicalNamespacePrefix("test.logical")
                .packageNamePrefix("test.actual")
                .licenseHeader(LicenseHeader.ASF_V2)
                .domain(domain)
                .entitiesModulePackageName("mod")
                .entitiesModuleClassSimpleName("MyEntitiesModule")
                .build();

        var entityGen = new DomainGenerator(config);

        entityGen.createDomainModel().streamJavaModels()
            .forEach(sample->{
                System.err.println("---------------------------------------");
                System.err.printf("%s%n", sample.buildJavaFile().toString());
            });
    }

}
