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

import java.util.Optional;

import org.jspecify.annotations.Nullable;

import org.apache.causeway.applib.value.Blob;
import org.apache.causeway.commons.collections.Can;

import org.apache.commons.compress.archivers.sevenz.SevenZMethod;
import org.jspecify.annotations.NonNull;

import io.github.causewaystuff.commons.base.types.NamedPath;
import io.github.causewaystuff.commons.compression.SevenZUtils;

public interface BlobStore {

    /**
     * Puts a {@link Blob} onto the store, using {@link BlobDescriptor}'s path as the key.
     * Any existing blob and descriptor associated with this key will be overwritten.
     */
    void putBlob(@NonNull BlobDescriptor blobDescriptor, @NonNull Blob blob);

    default void compressAndPutBlob(BlobDescriptor descriptor, Blob uncompressedBlob) {
        putBlob(descriptor, switch (descriptor.compression()) {
            case NONE -> uncompressedBlob;
            case ZIP -> uncompressedBlob.zip();
            case SEVEN_ZIP -> SevenZUtils.compress(uncompressedBlob, SevenZMethod.LZMA2);
        });
    }

    /**
     * Returns all the {@link BlobDescriptor}(s) from given path that match
     * <em>all</em> given {@code qualifiers}. If {@code qualifiers} is empty or {@code null}
     * the match does not discriminate. (includes all)
     * @param recursive whether or not to include sub-paths of given path
     */
    Can<BlobDescriptor> listDescriptors(
            @Nullable NamedPath path, @Nullable Can<BlobQualifier> qualifiers, boolean recursive);

    /**
     * Same as {@link #listDescriptors(NamedPath, Can, boolean)} but not discriminating by any qualifier.
     */
    default Can<BlobDescriptor> listDescriptors(@Nullable final NamedPath path, final boolean recursive) {
        return listDescriptors(path, Can.empty(), recursive);
    }

    /**
     * Optionally returns the {@link BlobDescriptor} thats stored under given {@link NamedPath},
     * based on existence.
     */
    Optional<BlobDescriptor> lookupDescriptor(@Nullable NamedPath path);

    /**
     * Optionally returns the {@link Blob} thats stored under given {@link NamedPath},
     * based on existence.
     */
    Optional<Blob> lookupBlob(@Nullable NamedPath path);
    /**
     * Optionally returns the {@link Blob} thats stored under given {@link NamedPath},
     * based on existence. The blob if any is also uncompressed (if required).
     */
    Optional<Blob> lookupBlobAndUncompress(@Nullable NamedPath path);

    /**
     * Deletes blob and descriptor that are associated with given {@link NamedPath} (if any).
     */
    void deleteBlob(@Nullable NamedPath path);

    /**
     * Compresses the {@link Blob} that is associated with given {@link BlobDescriptor} using
     * given {@link BlobDescriptor.Compression}.
     * <p>
     * Acts as a no-op if given blob descriptor's compression equals given compression parameter.
     * @param blobDescriptor
     * @param compression
     * @return new {@link BlobDescriptor} with new compression value from given compression parameter.
     */
    BlobDescriptor compress(@NonNull BlobDescriptor blobDescriptor, BlobDescriptor.@NonNull Compression compression);

}
