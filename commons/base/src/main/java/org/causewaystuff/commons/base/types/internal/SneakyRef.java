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
package org.causewaystuff.commons.base.types.internal;

import org.springframework.util.ClassUtils;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Specifically designed to be used with Java record types,
 * to allow circular references such as parent child relations.
 * <p>
 * Nonnull immutable variant of {@link ObjectRef}
 *
 * @see ObjectRef
 * @apiNote perhaps we can refactor dependent record types to not require this at all
 */
@RequiredArgsConstructor(staticName = "of")
public final class SneakyRef<T> {

    @Getter @Accessors(fluent=true)
    private final @NonNull T value;

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof SneakyRef;
    }

    @Override
    public int hashCode() {
        return 1;
    }

    @Override
    public String toString() {
        return ClassUtils.isPrimitiveOrWrapper(value.getClass())
                    ? value.toString()
                    : String.format("SneakyRef[%s]", value.getClass().getSimpleName());
    }

}
