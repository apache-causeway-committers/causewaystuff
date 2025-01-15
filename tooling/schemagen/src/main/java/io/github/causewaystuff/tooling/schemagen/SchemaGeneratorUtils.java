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
package io.github.causewaystuff.tooling.schemagen;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.generator.impl.TypeContextFactory;
import com.github.victools.jsonschema.generator.impl.module.MethodExclusionModule;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

@UtilityClass
public class SchemaGeneratorUtils {

    @SneakyThrows
    public SchemaGenerator schemaGeneratorDefault() {
        var config = defaultConfig();
        var typeContext = TypeContextFactory.createDefaultTypeContext(config);
        return new SchemaGenerator(config, typeContext);
    }
    
    @SneakyThrows
    public SchemaGenerator schemaGeneratorWithAnnotationTypeSupport() {
        var config = defaultConfig();
        var typeContext = TypeContextFactory.createDefaultTypeContext(config);
        return new SchemaGenerator(config, SchemaGeneratorPatcher.patch(typeContext));
    }
    
    @SneakyThrows
    public String prettyPrint(JsonNode jsonSchema) {
        var objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT); // pretty-printing
        var prettyJsonString = objectMapper
            .writerWithDefaultPrettyPrinter()
            .writeValueAsString(jsonSchema);
        return prettyJsonString;
    }
    
    // -- HELPER
    
    private SchemaGeneratorConfig defaultConfig() {
        var configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON);
        var config = configBuilder
                .with(new MethodExclusionModule(methodScope->
                        methodScope.getRawMember().getName().equals("toString")
                        || methodScope.getRawMember().getParameterCount()>0))
                .with(Option.NONSTATIC_NONVOID_NONGETTER_METHODS)
                .with(Option.EXTRA_OPEN_API_FORMAT_VALUES)
                .without(Option.FLATTENED_ENUMS_FROM_TOSTRING)
                .build();
        return config;
    }

}
