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
package io.github.causewaystuff.companion.cli;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.BeansException;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Import;

import org.apache.causeway.commons.collections.Can;
import org.apache.causeway.commons.internal.base._Casts;
import org.apache.causeway.commons.internal.base._Strings;

import lombok.SneakyThrows;

import io.github.causewaystuff.commons.base.types.ResourceFolder;
import io.github.causewaystuff.companion.assets.CoMutableProjectDescription;
import io.github.causewaystuff.companion.schema.CoApplication;
import io.spring.initializr.generator.project.ProjectAssetGenerator;
import io.spring.initializr.generator.project.ProjectGenerationContext;
import io.spring.initializr.generator.project.ProjectGenerator;
import io.spring.initializr.metadata.InitializrMetadata;

@SpringBootApplication
@Import({
    CompanionCliConfiguration.class
})
public class CompanionCli implements ApplicationRunner, ApplicationContextAware  {

    public static void main(final String[] args) {
        SpringApplication.run(CompanionCli.class, args);
    }

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
        springContextRef.set(applicationContext);
    }

    @Override
    public void run(final ApplicationArguments args) throws Exception {
        var argsModel = ArgsModel.parse(args.getSourceArgs());
        var springContext = springContextRef.get();

        var projectGenerator = new ProjectGenerator(
            projectGenerationContext -> customizeProjectGenerationContext(projectGenerationContext, argsModel));

        var initializrMetadata = springContext.getBean(InitializrMetadata.class);

        var projectDescription = new CoMutableProjectDescription(
            argsModel.projectRoot().root().toPath(),
            argsModel.appModel(),
            initializrMetadata);

        var assetGenerator = _Casts.<ProjectAssetGenerator<Path>>uncheckedCast(springContext.getBean("companionAssetGenerator"));
        projectGenerator.generate(projectDescription, assetGenerator);
    }

    private void customizeProjectGenerationContext(
            final ProjectGenerationContext projectGenerationContext,
            final ArgsModel argsModel) {
        projectGenerationContext.setParent(springContextRef.get());
    }

    // -- HELPER

    final AtomicReference<ApplicationContext> springContextRef = new AtomicReference<>();

    record ArgsModel(
        ResourceFolder projectRoot,
        CoApplication appModel) {

        @SneakyThrows
        static ArgsModel parse(final String[] args) {
            if(args.length==0) {
                printUsageAndExit();
                return null;
            }
            var map = new HashMap<String, String>();
            Can.ofArray(args).stream()
                .map(kv->_Strings.parseKeyValuePair(kv, '=').orElseThrow())
                .forEach(kvp->map.put(kvp.key(), kvp.value() ));
            var projectRoot = ResourceFolder.ofFileName(map.get("projectRoot"));
            if(projectRoot==null) {
                printUsageAndExit();
                return null;
            }
            //TODO if schema is not given, perhaps reassemble from resource folders
            var schema = _Strings.nonEmpty(map.get("schema")).map(File::new).orElse(null);
            if(schema==null) {
                printUsageAndExit();
                return null;
            }
            return new ArgsModel(
                projectRoot,
                CoApplication.fromYaml(Files.readString(schema.toPath(), StandardCharsets.UTF_8)));
        }

        static void printUsageAndExit() {
            System.err.println(
                    """
                    - please provide the project root directory as input parameter like e.g.
                    projectRoot=/path/to/project
                    - also provide the project companion-app-xxx.yaml file location as input parameter like e.g.
                    schema=/path/to/schema""");
            System.exit(1);
        }
    }
}