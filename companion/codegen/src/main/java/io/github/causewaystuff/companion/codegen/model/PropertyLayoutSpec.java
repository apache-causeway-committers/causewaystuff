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

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import org.apache.causeway.applib.annotation.LabelPosition;
import org.apache.causeway.applib.annotation.Navigable;
import org.apache.causeway.applib.annotation.Where;

import lombok.Builder;

@Builder(toBuilder = true)
public record PropertyLayoutSpec(
        @Nullable String named,
        @Nullable String cssClass,
        @Nullable String fieldSet,
        @Nullable String sequence,
        @Nullable Multiline description,
        @Nullable Integer multiLine,
        @Nullable Where hiddenWhere,
        @Nullable Navigable navigable,
        @Nullable LabelPosition labelPosition) {

    @Nullable
    public String describedAs() {
        return Optional.ofNullable(description).map(Multiline::describedAs).orElse(null);
    }

    public PropertyLayoutSpec overrideWith(final @Nullable PropertyLayoutSpec successor) {
        if(successor == null) return this;
        return this.toBuilder()
                .named(_TypeUtil.override(named, successor.named))
                .cssClass(_TypeUtil.override(cssClass, successor.cssClass))
                .fieldSet(_TypeUtil.override(fieldSet, successor.fieldSet))
                .sequence(_TypeUtil.override(sequence, successor.sequence))
                .description(_TypeUtil.override(description, successor.description))
                .multiLine(_TypeUtil.override(multiLine, successor.multiLine))
                .hiddenWhere(_TypeUtil.override(hiddenWhere, successor.hiddenWhere))
                .navigable(_TypeUtil.override(navigable, successor.navigable))
                .labelPosition(_TypeUtil.override(labelPosition, successor.labelPosition))
                .build();
    }

    // -- INTERNAL

    static PropertyLayoutSpec fromMap(@SuppressWarnings("rawtypes") final Map map) {
        return builder()
                .named((String) map.get("named"))
                .cssClass((String) map.get("cssClass"))
                .fieldSet((String) map.get("fieldSet"))
                .sequence((String) map.get("sequence"))
                .description(Multiline.parseMultilineString((String)map.get("description")))
                .multiLine(_TypeUtil.parseNullableIntegerWithBounds((Integer)map.get("multiLine"), 2, 1000))
                .hiddenWhere(_TypeUtil.parseNullableEnum(Where.class, (String) map.get("hiddenWhere")))
                .navigable(_TypeUtil.parseNullableEnum(Navigable.class, (String) map.get("navigable")))
                .labelPosition(_TypeUtil.parseNullableEnum(LabelPosition.class, (String) map.get("labelPosition")))
            .build();
    }

    /**
     * skips null valued attributes
     */
    Stream<Attribute> streamAttributes() {
        return Stream.of(
                new Attribute("named", named),
                new Attribute("cssClass", cssClass),
                new Attribute("fieldSet", fieldSet),
                new Attribute("sequence", sequence),
                new Attribute("described", description),
                new Attribute("multiLine", multiLine),
                new Attribute("hiddenWhere", hiddenWhere),
                new Attribute("navigable", navigable),
                new Attribute("labelPosition", labelPosition))
            .filter(a->a.value()!=null);
    }

}