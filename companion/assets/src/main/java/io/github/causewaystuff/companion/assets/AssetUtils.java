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
package io.github.causewaystuff.companion.assets;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Predicate;

import org.springframework.lang.Nullable;
import org.springframework.util.function.ThrowingConsumer;

import org.apache.causeway.commons.functional.Try;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AssetUtils {
    
    /**
     * In support of override strategy, that is, only calls given contributor, 
     * if given file is generated or does not exist. (if generated will delete)
     */
    public void override(@Nullable Path file, Predicate<Path> isGenerated, ThrowingConsumer<Path> contributor) {
        var destFile = canOverride(file, isGenerated).orElse(null);
        if(destFile==null) return;
        contributor.accept(file);
    }
    
    /**
     * In support of override strategy, that is, only override if given file is generated.
     * @return given file only if it either does not exist or was generated (if generated will delete)
     */
    @SneakyThrows
    public Optional<Path> canOverride(@Nullable Path file, Predicate<Path> isGenerated) {
        if(file==null) return Optional.empty();
        if(Files.exists(file)) {
            if(isGenerated.test(file)) {
                Files.delete(file);
            } else {
                return Optional.empty(); // keep file
            }
        }
        return Optional.ofNullable(file);
    }
    
    // -- PREDICATES
    
    public Predicate<Path> anyLineContains(String magic) {
        return (file)->Try.call(()->Files.readAllLines(file).stream()
            .anyMatch(line->line.contains(magic)))
            .valueAsNonNullElseFail();
    }

}
