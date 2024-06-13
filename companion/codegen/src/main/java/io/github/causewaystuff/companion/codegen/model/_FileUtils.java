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
package io.github.causewaystuff.companion.codegen.model;

import java.io.File;
import java.nio.charset.StandardCharsets;

import org.apache.causeway.commons.collections.Can;
import org.apache.causeway.commons.io.DataSource;
import org.apache.causeway.commons.io.FileUtils;
import org.apache.causeway.commons.io.TextUtils;

import lombok.val;
import lombok.experimental.UtilityClass;

import io.github.causewaystuff.companion.codegen.domgen.LicenseHeader;
import io.github.causewaystuff.companion.codegen.model.OrmModel.Entity;

@UtilityClass
class _FileUtils {

    Can<String> licenseHeaderAsYaml(final LicenseHeader licenseHeader){
        return Can.of("-----------------------------------------------------------")
                .addAll(TextUtils.readLines(licenseHeader.text()))
                .add("-----------------------------------------------------------")
                .map(s->"# " + s)
                .add("");
    }

    String collectSchemaFromFolder(final File rootDirectory) {
        val root = FileUtils.existingDirectoryElseFail(rootDirectory);
        val sb = new StringBuilder();
        FileUtils.searchFiles(root, dir->true, file->file.getName().endsWith(".yaml"))
            .stream()
            .map(DataSource::ofFile)
            .forEach(ds->{
                sb.append(ds.tryReadAsStringUtf8().valueAsNonNullElseFail()).append("\n\n");
            });
        return sb.toString();
    }

    void writeSchemaToFile(final OrmModel.Schema schema, final File file, final LicenseHeader licenseHeader) {
        val lic = _FileUtils.licenseHeaderAsYaml(licenseHeader);
        val yaml = TextUtils.readLines(_Writer.toYaml(schema));
        TextUtils.writeLinesToFile(lic.addAll(yaml), file, StandardCharsets.UTF_8);
    }

    void writeEntitiesToIndividualFiles(final Iterable<Entity> entities, final File rootDirectory,
            final LicenseHeader licenseHeader) {
        val dir0 = FileUtils.makeDir(rootDirectory);
        val dir1 = FileUtils.existingDirectoryElseFail(dir0);
        val lic = _FileUtils.licenseHeaderAsYaml(licenseHeader);
        entities.forEach(entity->{
            val destFile = new File(dir1, entity.name() + ".yaml");
            val yaml = TextUtils.readLines(entity.toYaml());
            TextUtils.writeLinesToFile(lic.addAll(yaml), destFile, StandardCharsets.UTF_8);
        });
    }
}
