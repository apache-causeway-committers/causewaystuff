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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.apache.causeway.commons.io.YamlUtils;

public record CoApplication(
    @JsonProperty("$schema") 
    String schema,
    String groupId,
    String artifactId,
    String version,
    String name,
    String description,
    String packageName,
    Persistence persistence,
    LicenseHeader license,
    List<CoModule> modules) {
    
    public CoApplication(
        String groupId,
        String artifactId,
        String version,
        String name, 
        String description,
        String packageName,
        Persistence persistence,
        LicenseHeader license,
        List<CoModule> modules) {
        this("https://apache-causeway-committers.github.io/causewaystuff/schema/companion/v1.0.0/companion.json", 
            groupId, artifactId, version, name, description, packageName, persistence, license, modules);
    }

    public static CoApplication fromYaml(String yaml) {
        return YamlUtils.tryRead(CoApplication.class, yaml)
            .valueAsNonNullElseFail();
    }
    
    public String toYaml() {
        return YamlUtils.toStringUtf8(this);
    }
}
