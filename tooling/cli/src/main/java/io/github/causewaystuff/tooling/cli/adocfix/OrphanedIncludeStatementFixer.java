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
package io.github.causewaystuff.tooling.cli.adocfix;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.SortedSet;

import org.apache.causeway.commons.collections.Can;
import org.apache.causeway.commons.internal.base._Refs;
import org.apache.causeway.commons.io.TextUtils;
import org.apache.causeway.valuetypes.asciidoc.builder.include.IncludeStatement;
import org.apache.causeway.valuetypes.asciidoc.builder.include.IncludeStatements;

import lombok.NonNull;

import lombok.extern.log4j.Log4j2;

import io.github.causewaystuff.tooling.cli.CliConfig;
import io.github.causewaystuff.tooling.j2adoc.J2AdocContext;

@Log4j2
public final class OrphanedIncludeStatementFixer {

    public static void fixIncludeStatements(
            final @NonNull SortedSet<File> adocFiles,
            final @NonNull CliConfig cliConfig,
            final @NonNull J2AdocContext j2aContext) {

        if(cliConfig.getCommands().getIndex().isDryRun()) {
            log.debug("IncludeStatementFixer: skip (dry-run)");
            return;
        }

        if(!cliConfig.getCommands().getIndex().isFixOrphanedAdocIncludeStatements()) {
            log.debug("IncludeStatementFixer: skip (disabled via config, fixOrphandedAdocIncludeStatements=false)");
            return;
        }

        log.debug("IncludeStatementFixer: about to process {} adoc files", adocFiles.size());

        var totalFixed = _Refs.intRef(0);

        adocFiles.forEach(adocFile->{
            //_Probe.errOut("adoc file found: %s", adocFile);

            var fixedCounter = _Refs.intRef(0);
            var originLines = TextUtils.readLinesFromFile(adocFile, StandardCharsets.UTF_8);

            var lines = IncludeStatements.rewrite(originLines, include->{
                final boolean inGlobalIndex =
                        "refguide".equals(include.getComponent())               // TODO should be reasoned from config
                        && include.getNamespace().startsWith(Can.of("index"));  // TODO should be reasoned from config
                if(include.isLocal() || !inGlobalIndex) {
                    return null; // keep original line, don't mangle
                }

                var correctedIncludeStatement = _Refs.<IncludeStatement>objectRef(null);
                var typeSimpleName = include.getCanonicalName();

                j2aContext.findUnitByTypeSimpleName(typeSimpleName)
                .ifPresent(unit->{

                    var module = unit.getNamespace().stream()
                            .skip(j2aContext.getNamespacePartsSkipCount())
                            .findFirst().get();
                    var expected = IncludeStatement.builder()
                    .component("refguide")
                    .module(module)
                    .type("page")
                    .namespace(unit.getNamespace().stream()
                            .skip(j2aContext.getNamespacePartsSkipCount() + 1) // +1 because is part of the module
                            .collect(Can.toCan())
                            .add(0, "index") // TODO this is antora config specific
                            )
                    .canonicalName(typeSimpleName)
                    .ext(".adoc")
                    .options("[leveloffset=+2]")
                    .build();

                    var includeLineShouldBe = expected.toAdocAsString();

                    if(!includeLineShouldBe.equals(include.getMatchingLine())) {
                        log.warn("mismatch\n {}\n {}\n", includeLineShouldBe, include.getMatchingLine());
                        correctedIncludeStatement.setValue(expected);
                        fixedCounter.incAndGet();
                    }

                });

                return correctedIncludeStatement
                        .getValue()
                        .orElse(null); // keep original line, don't mangle

            });

            if(fixedCounter.getValue()>0) {

                // write lines to file
                TextUtils.writeLinesToFile(lines, adocFile, StandardCharsets.UTF_8);

                totalFixed.update(n->n + fixedCounter.getValue());
            }

        });

        log.debug("IncludeStatementFixer: all done. ({} orphanded inlcudes fixed)", totalFixed.getValue());

    }

    // -- HELPER


}
