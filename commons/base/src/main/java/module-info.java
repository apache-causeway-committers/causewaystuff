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
module io.github.causewaystuff.commons.base {
    exports io.github.causewaystuff.commons.base.cache;
    exports io.github.causewaystuff.commons.base.listing;
    exports io.github.causewaystuff.commons.base.types;
    exports io.github.causewaystuff.commons.base.types.internal; //TODO restrict
    exports io.github.causewaystuff.commons.base.util;

    requires static lombok;

    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires transitive org.apache.causeway.commons;
    requires transitive org.apache.causeway.applib;
    requires spring.core;
    requires org.slf4j;
    requires org.apache.causeway.core.config;
}