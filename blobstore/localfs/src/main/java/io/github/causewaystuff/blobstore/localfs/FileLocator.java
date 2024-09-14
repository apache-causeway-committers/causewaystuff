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

import java.io.File;

import org.springframework.lang.Nullable;

import org.apache.causeway.commons.internal.base._Strings;
import org.apache.causeway.commons.io.FileUtils;

import lombok.NonNull;
import lombok.SneakyThrows;

import io.github.causewaystuff.blobstore.applib.BlobDescriptor;
import io.github.causewaystuff.blobstore.localfs.LocalFsBlobStore.DescriptorDto;
import io.github.causewaystuff.commons.base.types.NamedPath;
import io.github.causewaystuff.commons.base.types.ResourceFolder;

record FileLocator(
        NamedPath relativeFolderAsPath,
        File manifestFile,
        File blobFile) {

    static final String MANIFEST_SUFFIX = "~.yaml";

    // -- FACTORIES

    static FileLocator of(
            final ResourceFolder rootDirectory,
            final @NonNull BlobDescriptor blobDescriptor) {
        var path = blobDescriptor.path();
        var destFolderAsNamedPath = path.parentElseFail();
        var manifestPath = destFolderAsNamedPath.add(path.lastNameElseFail() + MANIFEST_SUFFIX);
        var manifestFile = rootDirectory.relativeFile(manifestPath);
        return new FileLocator(
                null,
                manifestFile,
                honorCompressionExtension(manifestFile, blobDescriptor.compression()));
    }

    // -- CONSTRUTION

    public FileLocator(
            final NamedPath relativeFolderAsPath,
            final File manifestFile) {
        this(relativeFolderAsPath, manifestFile, honorCompressionExtension(manifestFile, null));
    }

    // -- UTILS

    void makeDir() {
        FileUtils.makeDir(manifestFile.getParentFile());
    }

    boolean hasBlob() {
        return blobFile().exists();
    }

    BlobDescriptor blobDescriptorForManifest() {
        var blobDescriptor = DescriptorDto.readFrom(manifestFile())
            .toBlobDescriptor(relativeFolderAsPath().add(blobFile().getName()));
        return blobDescriptor;
    }

    @SneakyThrows
    BlobDescriptor blobDescriptorForBlob() {
        var blobFile = blobFile();
        return DescriptorDto.autoDetect(blobFile)
            .toBlobDescriptor(relativeFolderAsPath().add(blobFile().getName()));
    }

    // -- HELPER

    private static String honorCompressionExtension(
            final String baseName, final BlobDescriptor.Compression compression) {
        return baseName + compression.fileSuffix();
    }

    private static File honorCompressionExtension(
            final File manifestFile,
            final @Nullable BlobDescriptor.Compression compressionIfAny) {
        var compression = compressionIfAny!=null
                ? compressionIfAny
                : DescriptorDto.readFrom(manifestFile).compression();
        var blobName = honorCompressionExtension(
                baseNameFromManifestName(manifestFile.getName()), compression);
        return new File(manifestFile.getParentFile(), blobName);
    }

    private static String baseNameFromManifestName(final String manifestName) {
        return _Strings.substring(manifestName, 0, -FileLocator.MANIFEST_SUFFIX.length());
    }

}
