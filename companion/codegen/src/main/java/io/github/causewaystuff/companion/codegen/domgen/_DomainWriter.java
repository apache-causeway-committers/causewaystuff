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
package io.github.causewaystuff.companion.codegen.domgen;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import io.github.causewaystuff.companion.codegen.domgen.DomainGenerator.JavaFileModel;
import io.github.causewaystuff.companion.schema.LicenseHeader;
import io.github.causewaystuff.tooling.javapoet.JavaFile;
import org.springframework.lang.Nullable;

import org.apache.causeway.commons.internal.assertions._Assert;
import org.apache.causeway.commons.internal.base._Strings;
import org.apache.causeway.commons.internal.exceptions._Exceptions;
import org.apache.causeway.commons.io.FileUtils;
import org.apache.causeway.commons.io.TextUtils;

import lombok.NonNull;
import lombok.SneakyThrows;

import lombok.experimental.UtilityClass;

@UtilityClass
class _DomainWriter {

    public void writeToDirectory(final @Nullable Stream<JavaFileModel> modelStream, final @NonNull File destDir) {

        if(modelStream==null) {
            return;
        }

        FileUtils.existingDirectoryElseFail(destDir);

        modelStream
        .forEach(javaFileModel->{

                final String className = javaFileModel.className().canonicalName();
                var javaFile = javaFileModel.buildJavaFile();

                //System.out.printf("------%s----%n", className);
                //System.out.printf("%s%n", javaFile.toString());

                try {
                    writeToPath(javaFile, destDir.toPath(), javaFileModel.licenseHeader());
                } catch (IOException e) {
                    throw _Exceptions.unrecoverable(e, "failed to write java file %s", className);
                }
        });

    }

    /**
     * Workaround the fact, that JavaPoet (at the time of writing) has no support for multi-line file comments.
     * <p>
     * Writes this to {@code directory} using the standard directory structure.
     * Returns the {@link Path} instance to which source is actually written.
     * @see JavaFile#writeToPath(Path, Charset)
     */
    private Path writeToPath(final JavaFile javaFile, final Path directory, final LicenseHeader licenseHeader) throws IOException {
        _Assert.assertTrue(Files.notExists(directory) || Files.isDirectory(directory),
                ()->String.format("path %s exists but is not a directory.", directory));
        Path outputDirectory = directory;
        if (!javaFile.packageName.isEmpty()) {
            for (String packageComponent : javaFile.packageName.split("\\.")) {
                outputDirectory = outputDirectory.resolve(packageComponent);
            }
            Files.createDirectories(outputDirectory);
        }
        final Path outputPath = outputDirectory.resolve(javaFile.typeSpec.name + ".java");
        // don't override existing files
        if(Files.exists(outputPath)) {
            return outputPath;
        }
        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(outputPath), StandardCharsets.UTF_8)) {
            _Strings.nonEmpty(licenseHeader.text())
                .ifPresent(licenseText->writeMultilineComment(writer, licenseText));
            javaFile.writeTo(writer);
        }
        return outputPath;
    }

    @SneakyThrows
    private void writeMultilineComment(final Writer writer, final String text) {
        writer.write("/*\n");
        for(var line : TextUtils.readLines(text)) {
            writer.write(" * ");
            writer.write(line);
            writer.write("\n");
        }
        writer.write(" */\n");
    }

}
