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

import org.jspecify.annotations.NonNull;

import org.apache.causeway.applib.value.NamedWithMimeType.CommonMimeType;
import org.apache.causeway.commons.internal.base._Strings;
import org.apache.causeway.commons.io.FileUtils;

import io.github.causewaystuff.blobstore.applib.BlobDescriptor;
import io.github.causewaystuff.blobstore.localfs.LocalFsBlobStore.DescriptorDto;
import io.github.causewaystuff.commons.base.types.NamedPath;
import io.github.causewaystuff.commons.base.types.ResourceFolder;

record FileLocator(
        BlobDescriptor blobDescriptor,
        File manifestFile,
        File blobFile) {

    static final String MANIFEST_SUFFIX = "..yaml";

    // -- FACTORIES

    static FileLocator of(
            final ResourceFolder rootDirectory,
            final @NonNull BlobDescriptor blobDescriptor) {
        var path = blobDescriptor.path();
        var destFolderAsNamedPath = path.parentElseFail();
        var manifestPath = destFolderAsNamedPath.add(path.lastNameElseFail() + MANIFEST_SUFFIX);
        var manifestFile = rootDirectory.relativeFile(manifestPath);
        return new FileLocator(
                blobDescriptor,
                manifestFile,
                resolveBlob(manifestFile, blobDescriptor));
    }

    static FileLocator forManifestFile(
            final ResourceFolder rootDirectory,
            final File manifestFile) {
        var blobDescriptor = blobDescriptor(rootDirectory, manifestFile);
        return new FileLocator(
                blobDescriptor,
                manifestFile,
                resolveBlob(manifestFile, blobDescriptor));
    }

    static FileLocator forBlobFile(
            final ResourceFolder rootDirectory,
            final File blobFile) {
        var dto = DescriptorDto.autoDetect(blobFile);
        var compression = dto.compression();
        var relPath = NamedPath.of(blobFile.getParentFile())
                .toRelativePath(NamedPath.of(rootDirectory.root()));
        var baseName = baseNameFromBlobName(blobFile.getName(), dto.mimeType(), compression);
        var fallbackDescriptor = dto
                .toBlobDescriptor(relPath.add(baseName));
        return new FileLocator(
                fallbackDescriptor,
                new File(blobFile.getParentFile(), baseName + FileLocator.MANIFEST_SUFFIX),
                blobFile);
    }

    // -- UTILS

    void makeDir() {
        FileUtils.makeDir(manifestFile.getParentFile());
    }

    boolean hasBlob() {
        return blobFile().exists();
    }

    // -- HELPER

    private static BlobDescriptor blobDescriptor(
            final ResourceFolder rootDirectory,
            final File manifestFile) {
        var relPath = NamedPath.of(manifestFile.getParentFile())
                .toRelativePath(NamedPath.of(rootDirectory.root()));
        return DescriptorDto.readFrom(manifestFile)
                .toBlobDescriptor(relPath
                        .add(baseNameFromManifestName(manifestFile.getName())));
    }

    private static File resolveBlob(
            final File manifestFile,
            final BlobDescriptor blobDescriptor) {
        var compression = blobDescriptor.compression()!=null
                ? blobDescriptor.compression()
                : DescriptorDto.readFrom(manifestFile).compression();
        var blobName = _Strings.asFileNameWithExtension(
                baseNameFromManifestName(manifestFile.getName()),
                blobDescriptor.mimeType().proposedFileExtensions())
            + compression.fileSuffix();
        return new File(manifestFile.getParentFile(), blobName);
    }

    private static String baseNameFromManifestName(final String manifestName) {
        return manifestName.substring(0, manifestName.length()-FileLocator.MANIFEST_SUFFIX.length());
    }
    private static String baseNameFromBlobName(final String blobName, final CommonMimeType mime, final BlobDescriptor.Compression compression) {
        return LocalFsBlobStore.baseNameNoExt(blobName.substring(0, blobName.length()-compression.fileSuffix().length()), mime);
    }

}
