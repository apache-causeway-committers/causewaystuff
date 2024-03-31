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

import org.apache.causeway.commons.collections.Can;
import org.apache.causeway.commons.internal.assertions._Assert;

/**
 * Has non-empty name, with characters conforming to {@link Character#isJavaIdentifierPart(int)}.
 * <p>
 * A blob may provide zero ore more qualifiers via its descriptor.
 * A blob-store search can then be refined to filter for results that satisfy <b>all</b> the requested
 * qualifiers.
 */
public record BlobQualifier(
        String name) {

    public static Can<BlobQualifier> of(final String ...names) {
        return Can.ofArray(names).map(BlobQualifier::new);
    }

    public BlobQualifier(final String name) {
        _Assert.assertNotEmpty(name);
        _Assert.assertTrue(name.chars().allMatch(Character::isJavaIdentifierPart));
        this.name = name;
    }
}
