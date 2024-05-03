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
package io.github.causewaystuff.commons.compression;

import java.nio.charset.StandardCharsets;

import org.apache.commons.compress.archivers.sevenz.SevenZMethod;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.causeway.applib.value.Clob;
import org.apache.causeway.applib.value.NamedWithMimeType.CommonMimeType;

class SevenZUtilsTest {

    String sampleText = """
            Lorem ipsum dolor sit amet, consectetur adipiscing elit,
            sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.
            Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.
            Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur.
            Excepteur sint occaecat cupidatat non proident,
            sunt in culpa qui officia deserunt mollit anim id est laborum.
            """;

    @Test
    void roundtripOnBytes() {
        var originBytes = sampleText.getBytes(StandardCharsets.UTF_8);
        var compressedBytes = SevenZUtils.compress(originBytes, "dummy", SevenZMethod.LZMA2);
        var textAfterRoundtrip = new String(SevenZUtils.decompress(compressedBytes), StandardCharsets.UTF_8);
        assertEquals(sampleText, textAfterRoundtrip);
    }

    @Test
    void roundtripOnDataSource() {
        var originDataSource = Clob.of("sampleText", CommonMimeType.TXT, sampleText)
                .toBlobUtf8()
                .asDataSource();
        var compressed = SevenZUtils.compress(originDataSource, "dummy", SevenZMethod.LZMA2);
        var dataSourceAfterRoundtrip = SevenZUtils.decompress(compressed);

        assertEquals(sampleText, dataSourceAfterRoundtrip.tryReadAsStringUtf8().valueAsNonNullElseFail());
    }

    @Test
    void roundtripOnClob() {
        var origin = Clob.of("sampleText", CommonMimeType.TXT, sampleText);
        var compressed = SevenZUtils.compress(origin.toBlobUtf8(), SevenZMethod.LZMA2);
        var clobAfterRoundtrip = SevenZUtils.decompress(compressed, CommonMimeType.TXT);

        assertEquals(sampleText, clobAfterRoundtrip.toClob(StandardCharsets.UTF_8).asString());
    }

}
