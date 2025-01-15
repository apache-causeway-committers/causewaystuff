/*
v *  Licensed to the Apache Software Foundation (ASF) under one
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

import org.springframework.util.StringUtils;

public enum Persistence {
    NONE,
    JDBC,
    JPA,
    JDO;
    
    public boolean isNone() { return this==NONE; }
    public boolean isJdbc() { return this==JDBC; }
    public boolean isJpa() { return this==JPA; }
    public boolean isJdo() { return this==JDO; }
    
    public static Persistence parse(final String value) {
        if(!StringUtils.hasLength(value)) return NONE;
        return switch (value.toLowerCase()) {
            case "jdbc" -> JDBC;
            case "jpa" -> JPA;
            case "jdo" -> JDO;
            default -> NONE;
        };
    }
}
