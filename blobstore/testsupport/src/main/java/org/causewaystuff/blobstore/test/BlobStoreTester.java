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
package org.causewaystuff.blobstore.test;

import java.time.Instant;

import org.causewaystuff.blobstore.applib.BlobDescriptor;
import org.causewaystuff.blobstore.applib.BlobDescriptor.Compression;
import org.causewaystuff.blobstore.applib.BlobQualifier;
import org.causewaystuff.blobstore.applib.BlobStore;
import org.causewaystuff.commons.base.types.NamedPath;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.causeway.applib.value.Blob;
import org.apache.causeway.applib.value.NamedWithMimeType.CommonMimeType;
import org.apache.causeway.commons.collections.Can;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BlobStoreTester {

    public static record Scenario(
            @NonNull BlobDescriptor blobDescriptor,
            @NonNull Blob blob,
            @NonNull Can<Can<BlobQualifier>> matchingQualifiers,
            @NonNull Can<Can<BlobQualifier>> excludingQualifiers
            ) {
        public NamedPath path() {
            return blobDescriptor.path();
        }
    }

    final BlobStore blobStore;

    public enum ScenarioSample {
        SMALL_BINARY {
            @Override public Scenario create() {
                var name = "myblob.bin";
                var mime = CommonMimeType.BIN;
                var blob = Blob.of(name, mime, new byte[] {1, 2, 3, 4});
                var createdOn = Instant.now();
                var path = NamedPath.of("a", "b", name);
                var blobDesc = new BlobDescriptor(
                        path, mime, "scenario-sampler", createdOn, 4, Compression.NONE,
                        BlobQualifier.of());
                return new Scenario(blobDesc, blob, Can.empty(), Can.empty());
            }
        },
        SMALL_BINARY_QUALIFIED {
            @Override public Scenario create() {
                var name = "myblob.bin";
                var mime = CommonMimeType.BIN;
                var blob = Blob.of(name, mime, new byte[] {1, 2, 3, 4});
                var createdOn = Instant.now();
                var path = NamedPath.of("a", "b", name);
                var blobDesc = new BlobDescriptor(
                        path, mime, "scenario-sampler-qualified", createdOn, 4, Compression.NONE,
                        BlobQualifier.of("qa", "qb"));
                return new Scenario(blobDesc, blob,
                        Can.of(
                                Can.empty(),
                                BlobQualifier.of("qa"),
                                BlobQualifier.of("qb"),
                                BlobQualifier.of("qa", "qb")),
                        Can.of(BlobQualifier.of("qc")));
            }
        },
        ;
        public abstract Scenario create();
    }

    public void setup(final Scenario scenario) {
        assertNotNull(blobStore);

        var path = scenario.path();

        // expected precondition
        assertTrue(blobStore.lookupDescriptor(path).isEmpty());
        assertTrue(blobStore.lookupBlob(path).isEmpty());
        assertTrue(blobStore.listDescriptors(NamedPath.empty(), true).isEmpty());

        // when
        blobStore.putBlob(scenario.blobDescriptor(), scenario.blob());
    }

    public void cleanup(final Scenario scenario) {
        var path = scenario.path();

        // cleanup
        blobStore.deleteBlob(path);

        // expected postcondition
        assertTrue(blobStore.lookupDescriptor(path).isEmpty());
        assertTrue(blobStore.lookupBlob(path).isEmpty());

    }

    public void assertExpectations(final Scenario scenario) {
        var path = scenario.path();

        var blobDescRecovered = blobStore.lookupDescriptor(path).orElse(null);
        assertNotNull(blobDescRecovered);
        assertEquals(scenario.blobDescriptor(), blobDescRecovered);

        var blobRecovered = blobStore.lookupBlob(path).orElse(null);
        assertNotNull(blobRecovered);
        assertEquals(scenario.blob(), blobRecovered);

        // no qualifiers
        assertTrue(blobStore.listDescriptors(NamedPath.empty(), true).getCardinality().isOne());
        assertTrue(blobStore.listDescriptors(NamedPath.of("a"), true).getCardinality().isOne());
        assertTrue(blobStore.listDescriptors(NamedPath.of("b"), true).getCardinality().isZero());

        // with qualifiers
        scenario.matchingQualifiers().forEach(q->{
            assertTrue(blobStore.listDescriptors(NamedPath.empty(), q, true).getCardinality().isOne(),
                    ()->String.format("expeced to match %s, but did not", q));
        });
        scenario.excludingQualifiers().forEach(q->{
            assertTrue(blobStore.listDescriptors(NamedPath.empty(), q, true).getCardinality().isZero(),
                    ()->String.format("expeced to discriminate %s, but did not", q));
        });

        assertTrue(blobStore.listDescriptors(NamedPath.empty(), true).getCardinality().isOne());

    }

}
