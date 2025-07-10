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
package io.github.causewaystuff.commons.base.cache;

import java.util.Optional;

import org.springframework.util.function.ThrowingSupplier;

import org.apache.causeway.commons.internal.base._Lazy;
import lombok.Synchronized;

/**
 * Lazily holds an aggregate T in memory, that is assembled via given {@code supplier}.
 * The supplier is considered costly, hence an alternative strategy is provided via a
 * {@link CacheHandler}, that knows how to marshal the type T to and from some store
 * (e.g. the local file-system).
 *
 * @param <T> aggregate type
 */
public final class CachableAggregate<T> {

    private final _Lazy<T> lazy;
    private final CacheHandler<T> cacheHandler;

    public CachableAggregate(ThrowingSupplier<? extends T> supplier, CacheHandler<T> cacheHandler) {
        this.lazy = _Lazy.threadSafe(()->intercept(supplier, cacheHandler));
        this.cacheHandler = cacheHandler;
    }

    @Synchronized public boolean isMemoized() { return lazy.isMemoized(); }
    @Synchronized public Optional<T> getMemoized() { return lazy.getMemoized(); }
    @Synchronized public T get() { return lazy.get(); }

    @Synchronized
    public void invalidate() {
        cacheHandler.invalidate();
        lazy.clear();
    }

    // -- HELPER

    static <T> T intercept(ThrowingSupplier<? extends T> supplier, CacheHandler<T> cacheHandler) {
        var fromCache = cacheHandler.tryRead().getValue().orElse(null);
        if(fromCache!=null) return fromCache;

        var fromOriging = cacheHandler.onGatherFromOrigin(supplier);
        if(fromOriging!=null) {
            cacheHandler.tryWrite(fromOriging);
        }
        return fromOriging;
    }

}
