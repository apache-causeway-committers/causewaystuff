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
package io.github.causewaystuff.blobstore.applib;

import java.util.Locale;
import java.util.function.Function;

import org.jspecify.annotations.NonNull;

import org.apache.causeway.applib.value.Blob;
import org.apache.causeway.commons.functional.Try;
import org.apache.causeway.commons.internal.base._Timing;
import lombok.extern.slf4j.Slf4j;

import io.github.causewaystuff.commons.base.cache.CacheHandler;

@Slf4j
public record BlobCacheHandler<T>(
    BlobDescriptor descriptor,
    BlobStore blobStore,
    Function<Blob, T> reader,
    Function<T, Blob> writer
    ) implements CacheHandler<T> {

    @Override
    public void invalidate() {
        blobStore.deleteBlob(descriptor.path());
    }

    @Override
    public Try<T> tryRead() {
        var watch = _Timing.now();
        var blob = blobStore
            .lookupBlobAndUncompress(descriptor.path())
            .orElse(null);
        if(blob==null) return Try.success(null);

        var result = Try.call(()->reader.apply(blob));
        watch.stop();
        log.info(String.format(Locale.US, "Calling '%s' took %d ms", "read from cache at %s".formatted(descriptor.path()), watch.getMillis()));
        return result;
    }

    @Override
    public Try<Void> tryWrite(@NonNull T t) {
        return _Timing.callVerbose(log, "write to cache at %s".formatted(descriptor.path()), ()->
            Try.run(()->
                blobStore.compressAndPutBlob(descriptor, writer.apply(t))));
    }
}
