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
package io.github.causewaystuff.companion.applib.jpa;

import java.util.stream.Stream;

import jakarta.persistence.AttributeConverter;

@FunctionalInterface
public interface EnumConverter<E extends EnumWithCode<T>, T> extends AttributeConverter<E, T> {

    E[] values();

    @Override default T convertToDatabaseColumn(final E _enum) {
        if (_enum == null)  return null;
        return _enum.code();
    }

    @Override default E convertToEntityAttribute(final T code) {
        if (code == null) return null;
        return Stream.of(values())
          .filter(_enum -> _enum.code().equals(code))
          .findFirst()
          .orElseThrow(IllegalArgumentException::new);
    }
}
