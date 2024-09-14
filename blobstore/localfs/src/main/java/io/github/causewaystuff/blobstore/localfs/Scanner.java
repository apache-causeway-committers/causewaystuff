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

import java.util.HashMap;
import java.util.Map;

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
            .map(manifestFile->FileLocator.forManifestFile(rootDirectory, manifestFile))
            .map(FileLocator::blobDescriptor)
            .forEach(descriptor->descriptorsByPath.put(descriptor.path(), descriptor));
        // scan non-manifest files and add to scan result
        FileUtils.searchFiles(rootDirectory.root(), dir->true, file->!file.getName().endsWith(FileLocator.MANIFEST_SUFFIX))
            .stream()
            .map(blobFile->FileLocator.forBlobFile(rootDirectory, blobFile))
            .map(FileLocator::blobDescriptor)
            .forEach(descriptor->descriptorsByPath.merge(descriptor.path(), descriptor, this::mergeBlobDescriptors));
        return descriptorsByPath;
    }

    private BlobDescriptor mergeBlobDescriptors(final BlobDescriptor fromManifest, final BlobDescriptor fromBlob) {
        return fromManifest; //TODO update size?
    }

}
