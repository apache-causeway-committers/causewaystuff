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
package io.github.causewaystuff.blobstore.localfs;

import org.apache.commons.compress.archivers.sevenz.SevenZMethod;

import org.apache.causeway.applib.value.Blob;
import org.apache.causeway.applib.value.NamedWithMimeType.CommonMimeType;

import lombok.NonNull;
import lombok.experimental.UtilityClass;

import io.github.causewaystuff.blobstore.applib.BlobDescriptor;
import io.github.causewaystuff.commons.compression.SevenZUtils;

@UtilityClass
class CompressUtils {

    Blob recompressBlob(
            final @NonNull Blob blob,
            final @NonNull CommonMimeType mimeForUncompressed,
            final @NonNull BlobDescriptor.Compression compressionIn,
            final @NonNull BlobDescriptor.Compression compressionOut) {
        if(compressionIn == compressionOut) return blob;
        final Blob base = switch (compressionIn) {
            case NONE -> blob;
            case ZIP -> blob.unZip(mimeForUncompressed);
            case SEVEN_ZIP -> SevenZUtils.decompress(blob, mimeForUncompressed);
        };
        final Blob compressed = switch (compressionOut) {
            case NONE -> base;
            case ZIP -> base.zip();
            case SEVEN_ZIP -> SevenZUtils.compress(base, SevenZMethod.LZMA2);
        };
        return compressed;
    }

}
