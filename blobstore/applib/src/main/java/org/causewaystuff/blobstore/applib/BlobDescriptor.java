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
package org.causewaystuff.blobstore.applib;

import java.time.Instant;

import org.causewaystuff.commons.base.types.NamedPath;

import org.apache.causeway.applib.value.NamedWithMimeType.CommonMimeType;
import org.apache.causeway.commons.collections.Can;

public record BlobDescriptor(
        NamedPath path,
        CommonMimeType mimeType,
        String createdBy,
        Instant createdOn,
        long size,
        Compression compression,
        Can<BlobQualifier> qualifiers) {

    public enum Compression {
        NONE,
        ZIP,
        SEVEN_ZIP;
    }

}
