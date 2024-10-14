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
package io.github.causewaystuff.commons.base.util;

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import lombok.experimental.UtilityClass;

@UtilityClass
public class StreamUtils {

    public <T> Stream<T> toStream(final Iterator<T> sourceIterator) {
        return toStream(sourceIterator, false);
    }

    public <T> Stream<T> toStream(final Iterator<T> sourceIterator, final boolean parallel) {
        Iterable<T> iterable = () -> sourceIterator;
        return StreamSupport.stream(iterable.spliterator(), parallel);
    }

    /**
     * Creates a {@link Stream} from a producer that supports the visitor pattern.
     * <p>
     * Limitation: an element once produced must not be modified by the producer later,
     *      as 2 threads are racing for access to the element
     * <p>
     * Usage:
     * {@snippet :
     * StreamUtils.toStream(collector->{
     *      someProducer.visit(collector::collect);
     * });
     * }
     */
    public <T> Stream<T> toStream(final Consumer<Consumer<T>> consumerConsumer) {
        return Stream.of((T)null)
                .mapMulti((__, downstream)->{
                    consumerConsumer.accept(downstream);
                });
    }

}
