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

import java.nio.file.attribute.FileTime;
import java.time.Instant;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.sevenz.SevenZMethod;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;

import org.springframework.lang.Nullable;

import org.apache.causeway.applib.value.Blob;
import org.apache.causeway.applib.value.NamedWithMimeType.CommonMimeType;
import org.apache.causeway.commons.internal.base._Bytes;
import org.apache.causeway.commons.io.DataSource;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

@UtilityClass
public class SevenZUtils {

    // -- DE-COMPRESSION

    @SneakyThrows
    @Nullable
    public byte[] decompress(final @Nullable byte[] inputData) {
        if(inputData==null) return null;
        var sevenZBuilder = SevenZFile.builder()
                .setByteArray(inputData);
        try(var sevenZFile = sevenZBuilder.get()) {
            var firstEntry = sevenZFile.getNextEntry();
            return _Bytes.of(sevenZFile.getInputStream(firstEntry));
        }
    }

    @SneakyThrows
    public DataSource decompress(final @Nullable DataSource inputData) {
        return inputData!=null
                ? DataSource.ofBytes(decompress(inputData.bytes()))
                : DataSource.empty();
    }

    @SneakyThrows
    @Nullable
    public Blob decompress(final @Nullable Blob inputBlob, final @NonNull CommonMimeType resultingMimeType) {
        if(inputBlob==null) return null;
        return Blob.of(inputBlob.getName(), resultingMimeType, decompress(inputBlob.getBytes()));
    }

    // -- COMPRESSION

    @SneakyThrows
    @Nullable
    public byte[] compress(final @Nullable byte[] inputData, final @NonNull String entryName, final @NonNull SevenZMethod method) {
        if(inputData==null) return null;
        var now = FileTime.from(Instant.now());
        var channel = new SeekableInMemoryByteChannel();
        try(var sevenZOutput = new SevenZOutputFile(channel)) {
            sevenZOutput.setContentCompression(method);
            var entry = new SevenZArchiveEntry();
            entry.setDirectory(false);
            entry.setName(entryName);
            entry.setLastModifiedTime(now);
            entry.setCreationTime(now);
            entry.setAccessTime(now);
            sevenZOutput.putArchiveEntry(entry);
            sevenZOutput.write(inputData);
            sevenZOutput.closeArchiveEntry();
            sevenZOutput.finish();
            return channel.array();
        }
    }

    @SneakyThrows
    public DataSource compress(final @Nullable DataSource inputData, final @NonNull String entryName, final @NonNull SevenZMethod method) {
        return inputData!=null
                ? DataSource.ofBytes(compress(inputData.bytes(), entryName, method))
                : DataSource.empty();
    }

    @SneakyThrows
    @Nullable
    public Blob compress(final @Nullable Blob inputBlob, final SevenZMethod method) {
        if(inputBlob==null) return null;
        return Blob.of(inputBlob.getName(), CommonMimeType._7Z, compress(inputBlob.getBytes(), inputBlob.getName(), method));
    }

}
