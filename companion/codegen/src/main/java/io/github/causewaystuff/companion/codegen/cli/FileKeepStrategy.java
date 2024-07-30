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
package io.github.causewaystuff.companion.codegen.cli;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.function.Predicate;

import org.apache.causeway.commons.io.TextUtils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class FileKeepStrategy {

    public Predicate<File> layout(){
        return file->file.getName().endsWith(".layout.xml")
                || file.getName().endsWith(".layout.fallback.xml");
    }

    public Predicate<File> javaNonGenerated(){
        return file->
            file.getName().endsWith(".java")
            && !TextUtils.readLinesFromFile(file, StandardCharsets.UTF_8)
                .stream()
                .anyMatch(line->line.startsWith("@Generated"));
    }

}