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
module io.github.causewaystuff.companion.assets {
    exports io.github.causewaystuff.companion.assets;
    exports io.github.causewaystuff.companion.assets.build.maven;
    exports io.github.causewaystuff.companion.assets.code.java;

    requires static lombok;
    
    requires java.xml;
    
    requires io.github.causewaystuff.commons.base;
    requires transitive io.github.causewaystuff.companion.schema;
    
    requires org.apache.causeway.commons;
    
    requires spring.beans;
    requires spring.context;
    requires spring.core;
    
    requires transitive initializr.metadata;
    requires transitive initializr.generator;
    requires transitive initializr.generator.spring;
    
    requires maven.api.model;
    requires maven.model;
    requires maven.support;
    
    // make io.github.causewaystuff.companion.assets.build.maven.PomPackagingFactory accessible 
    opens io.github.causewaystuff.companion.assets.build.maven to spring.core;
}