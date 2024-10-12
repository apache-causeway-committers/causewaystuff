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
package io.github.causewaystuff.companion.codegen.model;

import java.util.Optional;

import org.springframework.lang.Nullable;

import org.apache.causeway.applib.annotation.LabelPosition;
import org.apache.causeway.applib.annotation.Navigable;
import org.apache.causeway.applib.annotation.Where;
import org.apache.causeway.commons.internal.base._Strings;

import lombok.NonNull;
import lombok.experimental.UtilityClass;

@UtilityClass
class _TypeUtil {

    // -- PARSE

    @Nullable Integer parseNullableIntegerWithBounds(
            final @Nullable Integer value, final int lowerBound, final int upperBound) {
        return value==null
                || value < lowerBound
                || value > upperBound
                ? null
                : value;
    }

    @Nullable
    <E extends Enum<E>> E parseNullableEnum(@NonNull final Class<E> enumType, @Nullable final String literal) {
        return _Strings.isNotEmpty(literal)
                ? Enum.valueOf(enumType, literal)
                : null;
    }

    // -- OVERRIDE

    @Nullable String override(@Nullable final String primed, @Nullable final String override) {
        return _Strings.nonEmpty(override).orElse(primed);
    }

    @Nullable Integer override(@Nullable final Integer primed, @Nullable final Integer override) {
        return Optional.ofNullable(override).orElse(primed);
    }

    @Nullable Navigable override(@Nullable final Navigable primed, @Nullable final Navigable override) {
        return Optional.ofNullable(override).orElse(primed);
    }

    @Nullable LabelPosition override(@Nullable final LabelPosition primed, @Nullable final LabelPosition override) {
        return Optional.ofNullable(override).orElse(primed);
    }

    @Nullable Where override(@Nullable final Where primed, @Nullable final Where override) {
        return Optional.ofNullable(override).orElse(primed);
    }

    Multiline override(@Nullable final Multiline primed, @Nullable final Multiline override) {
        var _primed = Optional.ofNullable(primed).orElse(Multiline.empty());
        var _override = Optional.ofNullable(override).orElse(Multiline.empty());
        return _override.isEmpty()
                ? _primed
                : _override;
    }

}
