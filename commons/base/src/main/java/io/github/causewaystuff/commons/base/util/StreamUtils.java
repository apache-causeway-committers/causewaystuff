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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import lombok.SneakyThrows;
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

    @FunctionalInterface
    public static interface Collector<T> {
        void collect(T t);
        default void terminate() {
            collect(null); // terminal
        }
    }

    /**
     * Creates a {@link Stream} from a producer that supports the visitor pattern.
     * <p>
     * A null element from the producer terminates the sequence.
     * <p>
     * Limitation: an element once produced must not be modified by the producer later,
     *      as 2 threads are racing for access to the element
     * <p>
     * Usage:
     * <pre>
     * StreamUtils.toStream(collector->{
     *      someProducer.visit(collector::collect);
     * });
     * </pre>
     * @see Collector
     */
    @SneakyThrows
    public <T> Stream<T> toStream(final Consumer<Collector<T>> consumer) {
        final var collector = new BlockingCollector<T>(
                new LinkedBlockingQueue<>(1),
                new AtomicBoolean(false));
        var producer = Thread.startVirtualThread(()->{
            consumer.accept(collector);
            collector.terminate();
        });

        return Stream.generate(collector::take)
                .takeWhile(t->{
                    var terminated = t==null;
                    if(terminated) {
                        try {
                            producer.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    return !terminated;
                });
    }

    // -- HELPER

    private record BlockingCollector<T>(
            LinkedBlockingQueue<T> q,
            AtomicBoolean terminated)
    implements Collector<T> {
        @SneakyThrows
        @Override
        public void collect(final T t) {
            if(t==null) {
                terminated.set(true);
                return;
            }
            q.put(t);
        }
        /**
         * Try to poll until q is empty or terminated.
         */
        @SneakyThrows
        public T take() {
            while(hasPotentiallyMore()) {
                var t = q.poll(10, TimeUnit.MILLISECONDS);
                if(t!=null) {
                    return t;
                }
            }
            return null;
        }

        // -- HELPER

        private boolean hasPotentiallyMore() {
            if(terminated.get()) {
                return !q.isEmpty();
            }
            return true;
        }

    }

}
