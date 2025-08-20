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
package io.github.causewaystuff.blobstore.test;

import java.time.Instant;
import java.util.Map;

import org.jspecify.annotations.NonNull;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.causeway.applib.value.Blob;
import org.apache.causeway.applib.value.NamedWithMimeType.CommonMimeType;
import org.apache.causeway.commons.collections.Can;
import org.apache.causeway.commons.collections.Cardinality;

import lombok.RequiredArgsConstructor;

import io.github.causewaystuff.blobstore.applib.BlobDescriptor;
import io.github.causewaystuff.blobstore.applib.BlobDescriptor.Compression;
import io.github.causewaystuff.blobstore.applib.BlobQualifier;
import io.github.causewaystuff.blobstore.applib.BlobStore;
import io.github.causewaystuff.commons.base.types.NamedPath;

@RequiredArgsConstructor
public class BlobStoreTester {

    public static record Scenario(
            @NonNull BlobDescriptor blobDescriptor,
            @NonNull Blob blob,
            @NonNull Map<String, String> expectedAttributes,
            @NonNull Can<Can<BlobQualifier>> matchingQualifiers,
            @NonNull Can<Can<BlobQualifier>> excludingQualifiers
            ) {
        public NamedPath path() {
            return blobDescriptor.path();
        }
        public BlobDescriptor customize(final BlobDescriptor in) {
            var out = in
                .withCompression(blobDescriptor.compression())
                .withCreatedOn(blobDescriptor.createdOn())
                .withCreatedBy(blobDescriptor.createdBy())
                .withQualifiers(blobDescriptor.qualifiers())
                .withAttributes(blobDescriptor.attributes());
            return out;
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
                        path, mime, "scenario-sampler", createdOn, 4, Compression.NONE);
                return new Scenario(blobDesc, blob, Map.of(), Can.empty(), Can.empty());
            }
        },
        SMALL_BINARY_ATTRIBUTED {
            @Override public Scenario create() {
                var name = "myblob.bin";
                var mime = CommonMimeType.BIN;
                var blob = Blob.of(name, mime, new byte[] {1, 2, 3, 4});
                var createdOn = Instant.now();
                var path = NamedPath.of("a", "b", name);
                var attributes = Map.of(
                        "attr1", "Hello",
                        "attr2", "World!");
                var blobDesc = new BlobDescriptor(
                        path, mime, "scenario-sampler-attributed", createdOn, 4, Compression.NONE,
                        attributes);
                return new Scenario(blobDesc, blob, attributes, Can.empty(), Can.empty());
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
                        Map.of(),
                        BlobQualifier.of("qa", "qb"));
                return new Scenario(blobDesc, blob, Map.of(),
                        Can.of(
                                Can.empty(),
                                BlobQualifier.of("qa"),
                                BlobQualifier.of("qb"),
                                BlobQualifier.of("qa", "qb")),
                        Can.of(BlobQualifier.of("qc")));
            }
        };
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
        blobStore.putBlob(scenario.blobDescriptor().path(), scenario.blob(), scenario::customize);
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
        assertEquals(scenario.expectedAttributes().entrySet(), blobDescRecovered.attributes().entrySet());

        // no qualifiers
        assertEquals(Cardinality.ONE, blobStore.listDescriptors(NamedPath.empty(), true).getCardinality());
        assertEquals(Cardinality.ONE, blobStore.listDescriptors(NamedPath.of("a"), true).getCardinality());
        assertEquals(Cardinality.ZERO, blobStore.listDescriptors(NamedPath.of("b"), true).getCardinality());

        // with qualifiers
        scenario.matchingQualifiers().forEach(q->{
            assertTrue(blobStore.listDescriptors(NamedPath.empty(), q, true).getCardinality().isOne(),
                    ()->String.format("expeced to match %s, but did not", q));
        });
        scenario.excludingQualifiers().forEach(q->{
            assertTrue(blobStore.listDescriptors(NamedPath.empty(), q, true).getCardinality().isZero(),
                    ()->String.format("expeced to discriminate %s, but did not", q));
        });

        assertTrue(descriptors().getCardinality().isOne());
        var baseDescriptor = firstDescriptor();

        // compress no-op
        var noCompressDescriptor = blobStore.compress(baseDescriptor, Compression.NONE).orElseThrow();
        assertTrue(descriptors().getCardinality().isOne());
        assertEquals(noCompressDescriptor, firstDescriptor());

        // zip
        var zipDescriptor = blobStore.compress(baseDescriptor, Compression.ZIP).orElseThrow();
        {
            assertTrue(descriptors().getCardinality().isOne());
            var firstDescriptor = firstDescriptor();
            assertEquals(Compression.ZIP, firstDescriptor.compression());
            assertEquals(
                    scenario.path().lastNameElseFail(),
                    firstDescriptor.path().lastNameElseFail());
            // verify zip exists
            var zippedBlob = blobStore.lookupBlob(zipDescriptor.path())
                    .orElseThrow();
            assertEquals(CommonMimeType.ZIP.mimeType().getBaseType(), zippedBlob.mimeType().getBaseType());
        }

        // 7z re-compress
        var sevenZDescriptor = blobStore.compress(zipDescriptor, Compression.SEVEN_ZIP).orElseThrow();
        {
            assertTrue(descriptors().getCardinality().isOne());
            var firstDescriptor = firstDescriptor();
            assertEquals(Compression.SEVEN_ZIP, firstDescriptor.compression());
            assertEquals(
                    scenario.path().lastNameElseFail(),
                    firstDescriptor.path().lastNameElseFail());
            // verify 7z exists
            var sevenZBlob = blobStore.lookupBlob(sevenZDescriptor.path())
                    .orElseThrow();
            assertEquals(CommonMimeType._7Z.mimeType().getBaseType(), sevenZBlob.mimeType().getBaseType());
        }

        // un-zip
        var unzipDescriptor = blobStore.compress(sevenZDescriptor, Compression.NONE).orElseThrow();
        {
            assertTrue(descriptors().getCardinality().isOne());
            var firstDescriptor = firstDescriptor();
            assertEquals(Compression.NONE, firstDescriptor.compression());
            assertEquals(
                    scenario.path().lastNameElseFail(),
                    firstDescriptor.path().lastNameElseFail());
            // verify zip exists
            var unzippedBlob = blobStore.lookupBlob(unzipDescriptor.path())
                    .orElseThrow();
            assertEquals(CommonMimeType.BIN.mimeType().getBaseType(), unzippedBlob.mimeType().getBaseType());
        }

    }

    private Can<BlobDescriptor> descriptors() {
        return blobStore.listDescriptors(NamedPath.empty(), true);
    }

    private BlobDescriptor firstDescriptor() {
        return descriptors().getFirstElseFail();
    }

}
