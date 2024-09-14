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
import java.util.HashMap;
import java.util.Map;

import org.apache.causeway.commons.internal.base._Strings;
import org.apache.causeway.commons.io.FileUtils;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import io.github.causewaystuff.blobstore.applib.BlobDescriptor;
import io.github.causewaystuff.commons.base.types.NamedPath;
import io.github.causewaystuff.commons.base.types.ResourceFolder;

@Log4j2
record Scanner(
        ResourceFolder rootDirectory) {

    /**
     * Scan all {@link BlobDescriptor}(s), as recovered from file-system on the fly.
     */
    @SneakyThrows
    Map<NamedPath, BlobDescriptor> scan() {
        log.info("scanning folder {}", rootDirectory);
        var descriptorsByPath = new HashMap<NamedPath, BlobDescriptor>();
        // read all manifest files
        FileUtils.searchFiles(rootDirectory.root(), dir->true, file->file.getName().endsWith(FileLocator.MANIFEST_SUFFIX))
            .stream()
            .map(manifestFile->locatorForManifestFile(rootDirectory, manifestFile))
            .map(FileLocator::blobDescriptorForManifest)
            .forEach(descriptor->descriptorsByPath.put(descriptor.path(), descriptor));
        // scan non-manifest files and add to scan result
        FileUtils.searchFiles(rootDirectory.root(), dir->true, file->!file.getName().endsWith(FileLocator.MANIFEST_SUFFIX))
            .stream()
            .map(blobFile->locatorForBlobFile(rootDirectory, blobFile))
            .map(FileLocator::blobDescriptorForBlob)
            .forEach(descriptor->descriptorsByPath.merge(descriptor.path(), descriptor, this::mergeBlobDescriptors));
        return descriptorsByPath;
    }

    private static FileLocator locatorForManifestFile(
            final ResourceFolder rootDirectory,
            final File manifestFile) {
        var relPath = NamedPath.of(manifestFile.getParentFile())
                .toRelativePath(NamedPath.of(rootDirectory.root()));
        return new FileLocator(
                relPath,
                manifestFile);
    }

    private static FileLocator locatorForBlobFile(
            final ResourceFolder rootDirectory,
            final File blobFile) {
        var relPath = NamedPath.of(blobFile.getParentFile())
                .toRelativePath(NamedPath.of(rootDirectory.root()));
        var compression = BlobDescriptor.Compression.valueOf(blobFile);
        var baseName = _Strings.substring(blobFile.getName(), 0, -compression.fileSuffix().length());
        return new FileLocator(
                relPath,
                new File(blobFile.getParentFile(), baseName + FileLocator.MANIFEST_SUFFIX),
                blobFile);
    }

    private BlobDescriptor mergeBlobDescriptors(final BlobDescriptor fromManifest, final BlobDescriptor fromBlob) {
        return fromManifest; //TODO update size?
    }

}
