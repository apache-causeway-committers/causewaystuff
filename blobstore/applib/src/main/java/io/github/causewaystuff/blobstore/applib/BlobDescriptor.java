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

import java.io.File;
import java.time.Instant;
import java.util.Map;

import jakarta.activation.MimeType;

import org.jspecify.annotations.Nullable;

import org.apache.causeway.applib.value.NamedWithMimeType.CommonMimeType;
import org.apache.causeway.commons.collections.Can;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import io.github.causewaystuff.commons.base.types.NamedPath;

@Builder
public record BlobDescriptor(
        NamedPath path,
        CommonMimeType mimeType,
        String createdBy,
        Instant createdOn,
        long size,
        Compression compression,
        Map<String, String> attributes,
        Can<BlobQualifier> qualifiers) {

    @RequiredArgsConstructor
    public enum Compression {
        NONE("", null),
        ZIP(".zip", CommonMimeType.ZIP),
        SEVEN_ZIP(".7z", CommonMimeType._7Z);
        @Getter @Accessors(fluent=true)
        final String fileSuffix;
        final @Nullable CommonMimeType commonMimeType;
        public static Compression valueOf(final @Nullable MimeType mimeType) {
            if(mimeType==null) return Compression.NONE;
            for(var c : Compression.values()) {
                if(c==NONE) continue;
                if(c.commonMimeType.matches(mimeType)) return c;
            }
            return Compression.NONE;
        }
        public static Compression valueOf(final @Nullable File file) {
            if(file==null) return Compression.NONE;
            var fileName = file.getName().toLowerCase();
            for(var c : Compression.values()) {
                if(c==NONE) continue;
                if(fileName.endsWith(c.fileSuffix)) return c;
            }
            return Compression.NONE;
        }
    }

    public BlobDescriptor(
            final NamedPath path,
            final CommonMimeType mimeType,
            final String createdBy,
            final Instant createdOn,
            final long size,
            final Compression compression) {
        this(path, mimeType, createdBy, createdOn, size, compression, Map.of(), Can.empty());
    }

    public BlobDescriptor(
            final NamedPath path,
            final CommonMimeType mimeType,
            final String createdBy,
            final Instant createdOn,
            final long size,
            final Compression compression,
            final Map<String, String> attributes) {
        this(path, mimeType, createdBy, createdOn, size, compression, attributes, Can.empty());
    }

//validation
//    public BlobDescriptor(
//            final NamedPath path,
//            final CommonMimeType mimeType,
//            final String createdBy,
//            final Instant createdOn,
//            final long size,
//            final Compression compression,
//            final Map<String, String> attributes,
//            final Can<BlobQualifier> qualifiers) {
//        if(path.lastNameElseFail().endsWith(".zip")) {
//            throw _Exceptions.illegalArgument("%s", path);
//        }
//        this.path = path;
//        this.mimeType = mimeType;
//        this.createdBy = createdBy;
//        this.createdOn = createdOn;
//        this.size = size;
//        this.compression = compression;
//        this.attributes = attributes;
//        this.qualifiers = qualifiers;
//    }

    public BlobDescriptorBuilder asBuilder() {
        return BlobDescriptor.builder()
                .path(path)
                .mimeType(mimeType)
                .createdBy(createdBy)
                .createdOn(createdOn)
                .size(size)
                .compression(compression)
                .attributes(attributes)
                .qualifiers(qualifiers);
    }

}
