/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.internal.reflect;

import com.google.common.collect.Sets;
import org.gradle.api.Transformer;
import org.gradle.util.CollectionUtils;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Benchmark                                    (size)   Mode  Cnt         Score        Error  Units
 * CollectionUtilsBenchmark.collect_collection      10  thrpt   20  25044591.599 ± 470130.788  ops/s
 * CollectionUtilsBenchmark.collect_collection     100  thrpt   20   2874923.581 ±  40558.137  ops/s
 * CollectionUtilsBenchmark.collect_collection    1000  thrpt   20    295896.916 ±   1213.754  ops/s
 * CollectionUtilsBenchmark.collect_iterable        10  thrpt   20  17647067.697 ± 385483.786  ops/s
 * CollectionUtilsBenchmark.collect_iterable       100  thrpt   20   2058919.036 ±  28507.690  ops/s
 * CollectionUtilsBenchmark.collect_iterable      1000  thrpt   20    208554.590 ±   1459.872  ops/s
 * CollectionUtilsBenchmark.collect_set             10  thrpt   20   9243910.992 ± 104211.609  ops/s
 * CollectionUtilsBenchmark.collect_set            100  thrpt   20    692530.109 ±  19110.223  ops/s
 * CollectionUtilsBenchmark.collect_set           1000  thrpt   20     71080.103 ±   2151.557  ops/s
 **/
@Fork(2)
@State(Scope.Benchmark)
@Warmup(iterations = 10, time = 1, timeUnit = SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = SECONDS)
public class CollectionUtilsBenchmark {
    @Param({"10", "100", "1000"})
    int size;

    Iterable<Integer> iterable;
    Collection<Integer> collection;
    Set<Integer> set;

    @Setup
    public void setup() {
        List<Integer> values = new ArrayList<Integer>(size);
        for (int i = 0; i < size; i++) {
            values.add(i);
        }
        iterable = values;
        collection = values;
        set = Sets.newHashSet(values);
    }

    @Benchmark
    public Object collect_iterable() {
        return CollectionUtils.collect(iterable, new Transformer<Integer, Integer>() {
            @Override
            public Integer transform(Integer i) {
                return i;
            }
        });
    }

    @Benchmark
    public Object collect_collection() {
        return CollectionUtils.collect(collection, new Transformer<Integer, Integer>() {
            @Override
            public Integer transform(Integer i) {
                return i;
            }
        });
    }

    @Benchmark
    public Object collect_set() {
        return CollectionUtils.collect(set, new Transformer<Integer, Integer>() {
            @Override
            public Integer transform(Integer i) {
                return i;
            }
        });
    }
}
