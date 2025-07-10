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
package io.github.causewaystuff.commons.compression;

import java.io.File;
import java.util.function.Function;

import org.apache.commons.compress.archivers.sevenz.SevenZMethod;
import org.jspecify.annotations.NonNull;

import org.springframework.util.function.ThrowingSupplier;

import org.apache.causeway.commons.functional.Try;
import org.apache.causeway.commons.internal.base._Timing;
import org.apache.causeway.commons.io.DataSink;
import org.apache.causeway.commons.io.DataSource;
import org.apache.causeway.commons.io.FileUtils;

import lombok.extern.slf4j.Slf4j;

import io.github.causewaystuff.commons.base.cache.CacheHandler;

@Slf4j
public record SevenZCacheHandler<T>(
    File zipFile,
    Function<DataSource, T> reader,
    Function<T, byte[]> writer) implements CacheHandler<T> {

    @Override
    public void invalidate() {
        FileUtils.deleteFile(zipFile);
    }

    @Override
    public Try<T> tryRead() {
        if(!zipFile().exists()) return Try.success(null);
        return _Timing.callVerbose(log, "read from cache at %s".formatted(zipFile.getAbsolutePath()), ()->
            Try.call(()->
                reader.apply(asDataSource())));
    }

    @Override
    public Try<Void> tryWrite(@NonNull final T t) {
        return _Timing.callVerbose(log, "write to cache at %s".formatted(zipFile.getAbsolutePath()), ()->
            Try.run(()->asDataSink()
                .writeAll(os->
                    os.write(SevenZUtils.compress(writer.apply(t), "data", SevenZMethod.LZMA2)))));
    }

    @Override
    public T onGatherFromOrigin(final ThrowingSupplier<? extends T> supplier) {
        return _Timing.callVerbose(log, "gather from origin", supplier);
    }

    // -- HELPER

    private DataSource asDataSource() {
        return SevenZUtils.decompress(DataSource.ofFile(zipFile));
    }

    private DataSink asDataSink() {
        return DataSink.ofFile(zipFile);
    }

}
