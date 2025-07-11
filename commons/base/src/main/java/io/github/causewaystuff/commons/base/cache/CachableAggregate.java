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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

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

    /**
     * @param supplier assembles the aggregate (considered costly)
     * @param cacheHandler handles marshaling to and from some store;
     *      when {@code null} no marshaling is performed
     */
    public CachableAggregate(@NonNull ThrowingSupplier<? extends T> supplier, @Nullable CacheHandler<T> cacheHandler) {
        this.lazy = cacheHandler!=null
            ? _Lazy.threadSafe(()->intercept(supplier, cacheHandler))
            : _Lazy.threadSafe(supplier);
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

    // -- UTIL

    /**
     * Acts as a normal {@link _Lazy} without persistent caching.
     */
    public static <T> CachableAggregate<T> noCache(@NonNull ThrowingSupplier<? extends T> supplier) {
        return new CachableAggregate<>(supplier, null);
    }

    // -- HELPER

    static <T> T intercept(ThrowingSupplier<? extends T> supplier, CacheHandler<T> cacheHandler) {
        var fromCache = cacheHandler
                .tryRead()
                .ifFailure(Throwable::printStackTrace) // don't fail the request, but log to console
                .getValue()
                .orElse(null);
        if(fromCache!=null) return fromCache;

        var fromOriging = cacheHandler.onGatherFromOrigin(supplier);
        if(fromOriging!=null) {
            cacheHandler
                .tryWrite(fromOriging)
                .ifFailure(Throwable::printStackTrace); // don't fail the request, but log to console
        }
        return fromOriging;
    }

}
