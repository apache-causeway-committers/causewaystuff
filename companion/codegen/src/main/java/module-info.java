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
module io.github.causewaystuff.companion.codegen {
    exports io.github.causewaystuff.companion.codegen.domgen;
    exports io.github.causewaystuff.companion.codegen.model;
    exports io.github.causewaystuff.companion.codegen.structgen;

    requires lombok;
    requires jakarta.inject;
    requires java.compiler;
    requires transitive org.apache.causeway.applib;
    requires transitive org.apache.causeway.commons;
    requires transitive io.github.causewaystuff.tooling.javapoet;
    requires transitive io.github.causewaystuff.tooling.structurizr;
    requires transitive io.github.causewaystuff.tooling.projectmodel;
    requires transitive io.github.causewaystuff.commons.base;
    requires transitive io.github.causewaystuff.companion.applib;
    requires transitive io.github.causewaystuff.companion.schema;
    requires spring.context;
    requires spring.core;
    requires maven.model;

    opens io.github.causewaystuff.companion.codegen.cli to com.fasterxml.jackson.databind;
}