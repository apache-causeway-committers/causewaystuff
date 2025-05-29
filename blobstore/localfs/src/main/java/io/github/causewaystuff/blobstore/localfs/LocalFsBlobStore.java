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
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import org.springframework.stereotype.Repository;

import org.apache.causeway.applib.value.Blob;
import org.apache.causeway.applib.value.NamedWithMimeType.CommonMimeType;
import org.apache.causeway.commons.collections.Can;
import org.apache.causeway.commons.functional.Try;
import org.apache.causeway.commons.internal.assertions._Assert;
import org.apache.causeway.commons.internal.functions._Predicates;
import org.apache.causeway.commons.io.DataSink;
import org.apache.causeway.commons.io.DataSource;
import org.apache.causeway.commons.io.FileUtils;
import org.apache.causeway.commons.io.YamlUtils;

import lombok.SneakyThrows;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;

import io.github.causewaystuff.blobstore.applib.BlobDescriptor;
import io.github.causewaystuff.blobstore.applib.BlobDescriptor.Compression;
import io.github.causewaystuff.blobstore.applib.BlobQualifier;
import io.github.causewaystuff.blobstore.applib.BlobStore;
import io.github.causewaystuff.blobstore.applib.BlobStoreFactory.BlobStoreConfiguration;
import io.github.causewaystuff.commons.base.types.NamedPath;
import io.github.causewaystuff.commons.base.types.ResourceFolder;

@Repository
@Slf4j
public class LocalFsBlobStore implements BlobStore {

    private final ResourceFolder rootDirectory;
    private final Map<NamedPath, BlobDescriptor> descriptorsByPath;

    public LocalFsBlobStore(final BlobStoreConfiguration config) {
        this.rootDirectory = ResourceFolder.ofFile(new File(config.resource()));
        this.descriptorsByPath = new Scanner(rootDirectory).scan();
    }

    @Override @Synchronized
    public void putBlob(final @NonNull BlobDescriptor blobDescriptor, final @NonNull Blob blob) {

        var blobCompression =
                BlobDescriptor.Compression.valueOf(blob.mimeType());
        _Assert.assertEquals(
                blobDescriptor.compression(),
                blobCompression);

        var locator = FileLocator.of(rootDirectory, blobDescriptor);
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

    @Override
    public Optional<Blob> lookupBlob(final @Nullable NamedPath path) {
        return lookupBlob(path, null);
    }

    @Override
    public Optional<Blob> lookupBlobAndUncompress(final @Nullable NamedPath path) {
        return lookupBlob(path, Compression.NONE);
    }

    @Synchronized
    private Optional<Blob> lookupBlob(
            @Nullable final NamedPath path,
            @Nullable final Compression desiredCompression) {
        var descriptor = lookupDescriptor(path).orElse(null);
        if(descriptor==null) {
            return Optional.empty();
        }
        var locator = FileLocator.of(rootDirectory, descriptor);
        _Assert.assertTrue(locator.hasBlob(),
                ()->String.format("missing blob for path %s", path));
        var blobAsStored =  Blob.tryRead(
                descriptor.path().lastNameElseFail(),
                switch (descriptor.compression()) {
                    case NONE -> descriptor.mimeType();
                    case ZIP -> CommonMimeType.ZIP;
                    case SEVEN_ZIP -> CommonMimeType._7Z;
                },
                locator.blobFile())
                .getValue();
        return desiredCompression!=null
                ? blobAsStored.map(blob->CompressUtils.recompressBlob(
                                blob,
                                descriptor.mimeType(),
                                descriptor.compression(),
                                desiredCompression))
                : blobAsStored;
    }

    @Override @Synchronized
    public void deleteBlob(final @Nullable NamedPath path) {
        var descriptor = lookupDescriptor(path).orElse(null);
        if(descriptor==null) {
            return;
        }
        var locator = FileLocator.of(rootDirectory, descriptor);
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

    @Override
    public BlobDescriptor compress(
            final @NonNull BlobDescriptor blobDescriptor,
            final BlobDescriptor.@NonNull Compression compression) {
        var oldCompression = blobDescriptor.compression();
        if(oldCompression.equals(compression)) {
            return blobDescriptor;
        }
        var newDescriptor = blobDescriptor.asBuilder()
                .compression(compression)
                .build();
        var oldBlob = lookupBlob(blobDescriptor.path())
                .orElse(null);
        if(oldBlob==null) {
            return newDescriptor;
        }
        deleteBlob(blobDescriptor.path());
        putBlob(newDescriptor, CompressUtils.recompressBlob(
                oldBlob, blobDescriptor.mimeType(),
                oldCompression, compression));
        return newDescriptor;
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
