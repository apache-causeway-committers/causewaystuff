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
package io.github.causewaystuff.tooling.projectexplorer;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.List;

import org.approvaltests.Approvals;
import org.approvaltests.reporters.DiffReporter;
import org.approvaltests.reporters.UseReporter;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.causeway.commons.io.TextUtils;
import org.apache.causeway.testing.integtestsupport.applib.ApprovalsOptions;

import io.github.causewaystuff.tooling.projectexplorer.ProjectExplorer.ResolvedClass;
import io.github.causewaystuff.tooling.projectexplorer.ProjectExplorer.ResolvedProject;

class ProjectExplorerTest {

    @Test
    @UseReporter(DiffReporter.class)
    void projectToYaml() {

        var explorer = ProjectExplorerSamples.getDefault();

        explorer.projectByName()
            .forEach((final String name, final ResolvedProject proj)->{
                var yaml = proj.toYaml();
                Approvals.verify(yaml, ApprovalsOptions.defaultOptions()
                        .withScrubber(s -> TextUtils.readLines(s)
                                .map(line->line.startsWith("  projPath: ")
                                        ? "  projPath: \"<suppressed>\""
                                        : line)
                                .join("\n"))
                        .forFile()
                        .withExtension(".yaml"));
                //debug System.out.println(yaml));
            });
    }

    @Test
    void findAllSubTypes() {

        var explorer = ProjectExplorerSamples.getDefault();

        var publicIMultiplier = explorer.classByQualifiedName()
                .get("io.github.causewaystuff.tooling.projectexplorertest.PublicIMultiplier");
        assertThat(simpleNames(explorer.subTypesOf(publicIMultiplier)))
            .containsExactly("PublicAbstractClass", "PublicClass", "PublicIAdderMultiplier");

        var publicIDivider = explorer.classByQualifiedName()
                .get("io.github.causewaystuff.tooling.projectexplorertest.PublicIDivider");
        assertThat(simpleNames(explorer.subTypesOf(publicIDivider)))
            .containsExactly("PublicClass");

        var publicClass = explorer.classByQualifiedName()
                .get("io.github.causewaystuff.tooling.projectexplorertest.PublicClass");
        assertThat(simpleNames(explorer.subTypesOf(publicClass))).isEmpty();
    }

    @Test
    void readSource() throws IOException {

        var explorer = ProjectExplorerSamples.getDefault();

        var publicIMultiplier = explorer.classByQualifiedName()
                .get("io.github.causewaystuff.tooling.projectexplorertest.PublicIMultiplier");

        var path = publicIMultiplier.sourcePath(explorer).orElseThrow();
        var lines = Files.readAllLines(path);
        assertThat(lines).contains("public interface PublicIMultiplier {");
    }

    // -- HELPER

    static List<String> simpleNames(final Collection<ResolvedClass> resolvedClasses) {
        return resolvedClasses.stream().map(ResolvedClass::simpleName).toList();
    }

}
