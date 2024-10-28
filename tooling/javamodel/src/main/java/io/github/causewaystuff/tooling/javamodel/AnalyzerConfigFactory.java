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
package io.github.causewaystuff.tooling.javamodel;

import java.io.File;
import java.util.EnumSet;
import java.util.List;

import org.apache.causeway.commons.io.FileUtils;

import lombok.experimental.UtilityClass;

import io.github.causewaystuff.tooling.codeassert.config.AnalyzerConfig;
import io.github.causewaystuff.tooling.codeassert.config.Language;
import io.github.causewaystuff.tooling.codeassert.config.ProjectLayout.Maven;

@UtilityClass
public class AnalyzerConfigFactory {

    public static Maven maven(final File projDir, final Language ... languages) {
        return new MavenExt(projDir, languages);
    }

    public static Maven mavenTest(final File projDir, final Language ... languages) {
        return new MavenExtTest(projDir, languages);
    }

    // -- HELPER

    private static class AnalyzerConfigExt extends AnalyzerConfig {
        public AnalyzerConfigExt(final EnumSet<Language> languages, final List<Path> sources, final List<Path> classes) {
            super(languages, sources, classes);
        }
    }

    private static class MavenExt extends Maven {

        private final File projDir;

        public MavenExt(final File projDir, final Language ... languages) {
            super(null, languages);
            this.projDir = projDir;
        }

        @Override
        public AnalyzerConfig main(final String... packages) {
            return new AnalyzerConfigExt(getLanguages(),
                    path(packages, canonicalPath("src/main/$language/")),
                    path(packages, canonicalPath("target/classes/")));
        }

        protected String canonicalPath(final String relPath) {
            return FileUtils.canonicalPath(new File(projDir, relPath))
                    .orElse(relPath);
        }

    }

    private static class MavenExtTest extends MavenExt {

        public MavenExtTest(final File projDir, final Language[] languages) {
            super(projDir, languages);
        }

        @Override
        public AnalyzerConfig main(final String... packages) {
            return new AnalyzerConfigExt(getLanguages(),
                    path(packages, canonicalPath("src/test/$language/")),
                    path(packages, canonicalPath("target/test-classes/")));
        }

    }

}
