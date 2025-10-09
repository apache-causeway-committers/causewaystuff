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
import org.apache.causeway.commons.internal.base._Strings;
import org.apache.causeway.commons.io.DataSource;
import org.apache.causeway.commons.io.FileUtils;
import org.apache.causeway.commons.io.TextUtils;

import lombok.experimental.UtilityClass;

import io.github.causewaystuff.companion.codegen.model.Schema.Entity;
import io.github.causewaystuff.companion.codegen.model.Schema.ModuleNaming;
import io.github.causewaystuff.companion.codegen.model._Parser.ParserHint;
import io.github.causewaystuff.companion.schema.LicenseHeader;

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
        var root = FileUtils.existingDirectoryElseFail(rootDirectory);
        var domain = FileUtils.searchFiles(root, dir->true, file->file.getName().endsWith(".yaml"))
            .stream()
            .map(file->{
                var ds = DataSource.ofFile(file);
                var yaml = ds.tryReadAsStringUtf8().valueAsNonNullElseFail();
                return _Parser.parseSchema(yaml,
                        new ParserHint(_Strings.substring(file.getName(), 0, -5)));
            })
            .reduce((a, b)->a.concat(b))
            .get();
        return domain.toYaml();
    }

    void writeSchemaToFile(final ModuleNaming naming, final Schema.Domain schema, final File file, final LicenseHeader licenseHeader) {
        var lic = _FileUtils.licenseHeaderAsYaml(licenseHeader);
        var yaml = TextUtils.readLines(_Writer.toYaml(naming, schema));
        TextUtils.writeLinesToFile(lic.addAll(yaml), file, StandardCharsets.UTF_8);
    }

    void writeEntitiesToIndividualFiles(final Iterable<Entity> entities, final File rootDirectory,
            final LicenseHeader licenseHeader) {
        var dir0 = FileUtils.makeDir(rootDirectory);
        var dir1 = FileUtils.existingDirectoryElseFail(dir0);
        var lic = _FileUtils.licenseHeaderAsYaml(licenseHeader);
        entities.forEach(entity->{
            var destFile = new File(dir1, entity.name() + ".yaml");
            var yaml = TextUtils.readLines(entity.toYaml());
            TextUtils.writeLinesToFile(lic.addAll(yaml), destFile, StandardCharsets.UTF_8);
        });
    }

}
