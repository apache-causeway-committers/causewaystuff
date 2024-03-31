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
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import io.github.causewaystuff.blobstore.applib.BlobDescriptor;
import io.github.causewaystuff.blobstore.applib.BlobDescriptor.Compression;
import io.github.causewaystuff.blobstore.applib.BlobQualifier;
import io.github.causewaystuff.blobstore.applib.BlobStore;
import io.github.causewaystuff.blobstore.applib.BlobStoreFactory.BlobStoreConfiguration;
import io.github.causewaystuff.commons.base.types.NamedPath;
import io.github.causewaystuff.commons.base.types.ResourceFolder;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import org.apache.causeway.applib.value.Blob;
import org.apache.causeway.applib.value.NamedWithMimeType.CommonMimeType;
import org.apache.causeway.commons.collections.Can;
import org.apache.causeway.commons.functional.Try;
import org.apache.causeway.commons.internal.assertions._Assert;
import org.apache.causeway.commons.internal.base._Strings;
import org.apache.causeway.commons.internal.functions._Predicates;
import org.apache.causeway.commons.io.DataSink;
import org.apache.causeway.commons.io.DataSource;
import org.apache.causeway.commons.io.FileUtils;
import org.apache.causeway.commons.io.YamlUtils;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.Synchronized;
import lombok.extern.log4j.Log4j2;

@Repository
@Log4j2
public class LocalFsBlobStore implements BlobStore {

    private final ResourceFolder rootDirectory;
    private final Map<NamedPath, BlobDescriptor> descriptorsByPath;

    public LocalFsBlobStore(final BlobStoreConfiguration config) {
        this.rootDirectory = ResourceFolder.ofFile(new File(config.resource()));
        this.descriptorsByPath = scan();
    }

    @Override @Synchronized
    public void putBlob(final @NonNull BlobDescriptor blobDescriptor, final @NonNull Blob blob) {

        var locator = Locator.of(rootDirectory, blobDescriptor.path());
        locator.makeDir();

        // TODO specify override behavior
        blob.writeTo(locator.blobFile());
        DescriptorDto.of(blobDescriptor).writeTo(locator.manifestFile());

        descriptorsByPath.put(blobDescriptor.path(), blobDescriptor);
        log.info("Blob written {}", blobDescriptor);
    }

    @Override @Synchronized
    public Can<BlobDescriptor> listDescriptors(
            final @Nullable NamedPath path,
            final @Nullable Can<BlobQualifier> qualifiers,
            final boolean recursive) {
        var qualifierDiscriminator = satisfiesAll(qualifiers);
        return recursive
                ? descriptorsByPath.values().stream()
                    .filter(descriptor->descriptor.path().startsWith(path))
                    .filter(qualifierDiscriminator)
                    .collect(Can.toCan())
                : descriptorsByPath.values().stream()
                    .filter(descriptor->descriptor.path().parentElseFail().equals(path))
                    .filter(qualifierDiscriminator)
                    .collect(Can.toCan());
    }

    @Override @Synchronized
    public Optional<BlobDescriptor> lookupDescriptor(final @Nullable NamedPath path) {
        return Optional.ofNullable(descriptorsByPath.get(path));
    }

    @Override @Synchronized
    public Optional<Blob> lookupBlob(final @Nullable NamedPath path) {
        var descriptor = lookupDescriptor(path).orElse(null);
        if(descriptor==null) {
            return Optional.empty();
        }
        var locator = Locator.of(rootDirectory, path);
        //_Assert.assertTrue(locator.hasManifest()); allow on-the-fly descriptors, that only exist in memory
        _Assert.assertTrue(locator.hasBlob());
        return Blob.tryRead(descriptor.path().names().getLastElseFail(), descriptor.mimeType(), locator.blobFile())
                .getValue();
    }

    @Override @Synchronized
    public void deleteBlob(final @Nullable NamedPath path) {
        var locator = Locator.of(rootDirectory, path);
        var manifestFile = locator.manifestFile();
        var blobFile = locator.blobFile();

        if(blobFile.exists()) {
            Try.run(()->FileUtils.deleteFile(blobFile));
        }
        if(manifestFile.exists()) {
            Try.run(()->FileUtils.deleteFile(manifestFile));
        }
        descriptorsByPath.remove(path);
    }

    // -- HELPER

    /** used for serializing to file */
    static record DescriptorDto(
            CommonMimeType mimeType,
            String createdBy,
            Instant createdOn,
            long size,
            Compression compression,
            Map<String, String> attributes,
            Can<BlobQualifier> qualifiers) {
        static DescriptorDto of(final BlobDescriptor blobDescriptor) {
            return new DescriptorDto(blobDescriptor.mimeType(),
                    blobDescriptor.createdBy(),
                    blobDescriptor.createdOn(),
                    blobDescriptor.size(),
                    blobDescriptor.compression(),
                    blobDescriptor.attributes(),
                    blobDescriptor.qualifiers());
        }
        static DescriptorDto readFrom(final File file) {
            var descriptorDto = YamlUtils.tryRead(DescriptorDto.class, DataSource.ofFile(file))
                    .valueAsNonNullElseFail();
            return descriptorDto;
        }
        @SneakyThrows
        static DescriptorDto autoDetect(final File blobFile) {
            var attr = Files.readAttributes(blobFile.toPath(), BasicFileAttributes.class);
            var creationTime = attr.creationTime().toInstant();

            var fileNameParts = NamedPath.parse(blobFile.getName(), ".");

            var last = fileNameParts.nameCount()>1
                    ? fileNameParts.names().getLastElseFail().toUpperCase()
                    : "NONE";
            var middle = fileNameParts.nameCount()>2
                    ? fileNameParts.names().getRelativeToLastElseFail(-1).toUpperCase()
                    : null;

            final Compression compression = switch(last) {
                case "ZIP" -> Compression.ZIP;
                case "7Z" -> Compression.SEVEN_ZIP;
                default -> Compression.NONE;
            };

            final CommonMimeType mime = compression == Compression.NONE
                    ? CommonMimeType.valueOfFileExtension(last)
                            .orElse(CommonMimeType.BIN)
                    : middle!=null
                            ? CommonMimeType.valueOfFileExtension(middle)
                                    .orElse(CommonMimeType.BIN)
                            : CommonMimeType.valueOfFileExtension(last)
                                    .orElse(CommonMimeType.BIN);

            var blobDescriptor = new DescriptorDto(
                    mime,
                    "unknown",
                    creationTime,
                    attr.size(),
                    compression,
                    Map.of(),
                    Can.empty());
            return blobDescriptor;
        }
        void writeTo(final File file) {
            YamlUtils.write(this, DataSink.ofFile(file));
        }
        BlobDescriptor toBlobDescriptor(final NamedPath path) {
            var blobDescriptor = new BlobDescriptor(
                    path,
                    mimeType,
                    createdBy,
                    createdOn,
                    size,
                    compression,
                    attributes,
                    qualifiers);
            return blobDescriptor;
        }
    }

    private static record Locator(
            NamedPath relativeFolderAsPath,
            File manifestFile,
            File blobFile) {
        static final String MANIFEST_SUFFIX = "~.yaml";
        static Locator of(
                final ResourceFolder rootDirectory,
                final NamedPath path) {
            var destFolderAsNamedPath = path.parentElseFail();
            var blobPath = destFolderAsNamedPath.add(path.names().getLastElseFail());
            var manifestPath = destFolderAsNamedPath.add(path.names().getLastElseFail() + MANIFEST_SUFFIX);
            return new Locator(
                    null,
                    rootDirectory.relativeFile(manifestPath),
                    rootDirectory.relativeFile(blobPath));
        }
        static Locator forManifestFile(
                final ResourceFolder rootDirectory,
                final File manifestFile) {
            var relPath = NamedPath.of(manifestFile.getParentFile())
                    .toRelativePath(NamedPath.of(rootDirectory.root()));
            return new Locator(
                    relPath,
                    manifestFile,
                    new File(manifestFile.getParentFile(),
                            _Strings.substring(manifestFile.getName(), 0, -MANIFEST_SUFFIX.length())));
        }
        static Locator forBlobFile(
                final ResourceFolder rootDirectory,
                final File blobFile) {
            var relPath = NamedPath.of(blobFile.getParentFile())
                    .toRelativePath(NamedPath.of(rootDirectory.root()));
            return new Locator(
                    relPath,
                    new File(blobFile.getParentFile(), blobFile.getName() + MANIFEST_SUFFIX),
                    blobFile);
        }
        void makeDir() {
            FileUtils.makeDir(manifestFile.getParentFile());
        }
        boolean hasBlob() {
            return blobFile().exists();
        }
//        boolean hasManifest() {
//            return manifestFile().exists();
//        }
    }

    /**
     * Scan all {@link BlobDescriptor}(s), as recovered from file-system on the fly.
     */
    @SneakyThrows
    private Map<NamedPath, BlobDescriptor> scan() {
        log.info("scanning folder {}", rootDirectory);
        var descriptorsByPath = new HashMap<NamedPath, BlobDescriptor>();
        // read all manifest files
        FileUtils.searchFiles(rootDirectory.root(), dir->true, file->file.getName().endsWith(Locator.MANIFEST_SUFFIX))
            .stream()
            .map(manifestFile->Locator.forManifestFile(rootDirectory, manifestFile))
            .map(this::blobDescriptorForManifest)
            .forEach(descriptor->descriptorsByPath.put(descriptor.path(), descriptor));
        // scan non-manifest files and add to scan result
        FileUtils.searchFiles(rootDirectory.root(), dir->true, file->!file.getName().endsWith(Locator.MANIFEST_SUFFIX))
            .stream()
            .map(blobFile->Locator.forBlobFile(rootDirectory, blobFile))
            .map(this::blobDescriptorForBlob)
            .forEach(descriptor->descriptorsByPath.merge(descriptor.path(), descriptor, this::mergeBlobDescriptors));

        return descriptorsByPath;
    }

    private BlobDescriptor blobDescriptorForManifest(final Locator locator) {
        var blobDescriptor = DescriptorDto.readFrom(locator.manifestFile())
            .toBlobDescriptor(locator.relativeFolderAsPath().add(locator.blobFile().getName()));
        return blobDescriptor;
    }

    @SneakyThrows
    private BlobDescriptor blobDescriptorForBlob(final Locator locator) {
        var blobFile = locator.blobFile();
        return DescriptorDto.autoDetect(blobFile)
            .toBlobDescriptor(locator.relativeFolderAsPath().add(locator.blobFile().getName()));
    }

    private BlobDescriptor mergeBlobDescriptors(final BlobDescriptor fromManifest, final BlobDescriptor fromBlob) {
        return fromManifest; //TODO update size?
    }

    private Predicate<BlobDescriptor> satisfiesAll(final @Nullable Can<BlobQualifier> requiredQualifiers) {
        if(requiredQualifiers==null
                || requiredQualifiers.isEmpty()) {
            return _Predicates.alwaysTrue();
        }
        var required = requiredQualifiers.toSet();
        return desc -> {
            return desc.qualifiers().toSet().containsAll(required);
        };
    }

}
