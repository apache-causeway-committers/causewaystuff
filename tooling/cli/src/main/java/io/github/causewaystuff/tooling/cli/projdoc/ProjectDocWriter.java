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
package io.github.causewaystuff.tooling.cli.projdoc;

import java.io.File;
import java.util.function.BiConsumer;

import org.asciidoctor.ast.Document;

import org.apache.causeway.commons.internal.base._Refs;
import org.apache.causeway.commons.io.FileUtils;
import org.apache.causeway.valuetypes.asciidoc.builder.AsciiDocWriter;

import org.jspecify.annotations.NonNull;
import lombok.SneakyThrows;

import lombok.extern.slf4j.Slf4j;

import io.github.causewaystuff.tooling.cli.CliConfig;
import io.github.causewaystuff.tooling.j2adoc.J2AdocContext;
import io.github.causewaystuff.tooling.j2adoc.J2AdocUnit;

@Slf4j
final class ProjectDocWriter {

    @SneakyThrows
    static void write(
            final @NonNull CliConfig cliConfig,
            final @NonNull Document overviewAdoc,
            final @NonNull J2AdocContext j2aContext,
            final ProjectDocModel.@NonNull Mode mode) {

        @SuppressWarnings("unused")
        var global = cliConfig.getGlobal();
        var overview = cliConfig.getCommands().getOverview();
        var index = cliConfig.getCommands().getIndex();

        final BiConsumer<Document, File> overviewDocWriter = overview.isDryRun()
                ? (doc, file)->AsciiDocWriter.print(doc) // print to system out only (dry run)
                : AsciiDocWriter::writeToFile;

        final BiConsumer<Document, File> indexDocWriter = index.isDryRun()
                ? (doc, file)->AsciiDocWriter.print(doc) // print to system out only (dry run)
                : AsciiDocWriter::writeToFile;

        var currentUnit = _Refs.<J2AdocUnit>objectRef(null);

        var overviewPagesFolder = overview.getPagesFolder();
        var indexRootFolder = index.getRootFolder();

        var deleteCount = _Refs.intRef(0);
        int writeCount = 0;

        try {

            if (mode.includeOverview()) {

                // write system overview
                var overviewFile = new File(overviewPagesFolder, overview.getSystemOverviewFilename());
                log.info("writing system overview: {}", overviewFile.getName());
                overviewDocWriter.accept(overviewAdoc, overviewFile);
                ++writeCount;
            }

            if(mode.includeIndex()) {

                // delete all generated documents in the index
                FileUtils.searchFiles(indexRootFolder, dir->true, file-> {
                    var fileName = file.getName();
                    var fileAbsolutePath = file.getAbsolutePath();
                    return fileName.endsWith(".adoc") &&
                           !fileAbsolutePath.contains(File.separatorChar + "hooks" + File.separatorChar) &&
                           !fileName.equals(overview.getSystemOverviewFilename());
                })
                .stream()
                .peek(adocFile->log.debug("deleting file: {}", adocFile.getAbsolutePath()))
                .peek(__->deleteCount.incAndGet())
                .forEach(FileUtils::deleteFile);

                // write document index
                for(var unit : j2aContext.getUnitIndex().values()) {

                    currentUnit.setValue(unit);

                    var adocIndexFile = adocDestinationFileForUnit(unit, index);

                    log.info("writing file: {}", adocIndexFile.getName());

                    final Document asciiDoc = unit.toAsciiDoc(j2aContext, adocIndexFile);
                    indexDocWriter.accept(
                            asciiDoc,
                            adocIndexFile);

                    ++writeCount;
                }

                // summary
                log.info(
                        "ProjectDocWriter: all done. (deleted: {}, written: {})",
                        deleteCount.getValue(), writeCount);
            }

        } catch (Exception e) {
            System.err.printf(
                    "failed to write adoc for unit %s%n",
                    currentUnit.getValue().map(J2AdocUnit::getCanonicalName).orElse("none"));
            e.printStackTrace();
            System.exit(1);
        }
    }

    // generate output file based on unit's namespace and unit's name
    private static File adocDestinationFileForUnit(
            final @NonNull J2AdocUnit unit,
            final CliConfig.Commands.@NonNull Index index) {

        // eg: antora/components/refguide-index
        final File indexRootFolder = index.getRootFolder();
        var indexFolder = indexRootFolder;

        var destFolderBuilder = _Refs.<File>objectRef(indexFolder);

        // eg org/apache/causeway/applib/annotation
        unit.getNamespace().stream()
        // eg applib/annotation
        .skip(index.getNamespacePartsSkipCount())
        .findFirst()
        .ifPresent(moduleName-> {
            // applib
            // ... so updates to antora/components/refguide-index/modules/applib/pages/index
            destFolderBuilder.update(currentDir -> {
                final File modules = new File(currentDir, "modules");
                final File thisModule = new File(modules, moduleName);
                final File pages = new File(thisModule, "pages");
                return new File(pages, "index");
            });
        });

        // eg org/apache/causeway/applib/annotation
        unit.getNamespace().stream()
                // eg applib/annotation
        .skip(index.getNamespacePartsSkipCount() + 1)
        .forEach(subDir-> {
            // annotation
            // ... so updates to antora/components/refguide-index/modules/applib/pages/index/annotation
            destFolderBuilder.update(currentDir -> new File(currentDir, subDir));
        });

        var destFolder = destFolderBuilder.getValueElseDefault(indexFolder);
        destFolder.mkdirs();

        final String fileName = unit.getCanonicalName() + ".adoc";
        return new File(destFolder, fileName);

    }

}
