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
package io.github.causewaystuff.tooling.javamodel.test;

import java.io.File;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;



class ProjectSamples {

    static File causewaystuffRoot() {
        final File projRootFolder = new File("./").getAbsoluteFile().getParentFile().getParentFile().getParentFile();
        return projRootFolder;
    }

    static File causewaystuffCommonsBase() {
        return new File(causewaystuffRoot(), "commons/base");
    }
    
    static File causewaystuffBlobstoreApplib() {
        return new File(causewaystuffRoot(), "blobstore/applib");
    }

    static File self() {
        return new File("./").getAbsoluteFile();
    }

    static void assertHasBlobstoreApplibServices(final Stream<String> classNames) {

        var components = classNames
        .map(s->s.replace("io.github.causewaystuff.", "~."))
        //.peek(System.out::println) //debug
        .collect(Collectors.toSet());

        assertTrue(components.contains("~.blobstore.applib.BlobStoreFactory"));
    }

    static void assertHasCommonsBaseSourceFiles(final Stream<String> sourcePaths) {

        var sources = sourcePaths
        .map(s->s.replace("\\", "/"))
        .map(s->s.replace("/src/main/java/io/github/causewaystuff/", "~/"))
        //.peek(System.out::println) //debug
        .collect(Collectors.toSet());

        assertTrue(sources.contains("~/commons/base/types/NamedPath.java"));
        assertTrue(sources.contains("~/commons/base/types/ResourceFolder.java"));
    }

}
