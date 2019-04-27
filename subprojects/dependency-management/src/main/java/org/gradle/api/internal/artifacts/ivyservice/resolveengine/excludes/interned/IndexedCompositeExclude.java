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
package org.gradle.api.internal.artifacts.ivyservice.resolveengine.excludes.interned;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.gradle.api.artifacts.ModuleIdentifier;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.excludes.specs.CompositeExclude;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.excludes.specs.ExcludeSpec;
import org.gradle.internal.Cast;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;

abstract class IndexedCompositeExclude implements CompositeExclude, IndexedExcludeSpec {
    private final Map<ModuleIdentifier, Boolean> queryCache = Maps.newConcurrentMap();

    private final int index;
    private final IntSet componentsIndex;
    private final IndexedExcludeFactory factory;
    private final int cardinality;
    private final ExcludeCollection components;
    private final int hashCode;

    IndexedCompositeExclude(int index, IntSet componentsIndex, IndexedExcludeFactory factory) {
        this.index = index;
        this.componentsIndex = componentsIndex;
        this.factory = factory;
        this.components = new ExcludeCollection();
        this.cardinality = componentsIndex.size();
        this.hashCode = computeHashCode();
    }

    private int computeHashCode() {
        int hashCode = index;
        IntIterator it = componentsIndex.iterator();
        while (it.hasNext()) {
            hashCode = 31 * hashCode + it.nextInt();
        }
        return hashCode;
    }

    IndexedExcludeSpec getAt(int index) {
        return factory.getAt(index);
    }

    @Override
    public Stream<ExcludeSpec> components() {
        return Cast.uncheckedCast(components.stream());
    }

    @Override
    public Collection<ExcludeSpec> getComponents() {
        return Cast.uncheckedCast(components);
    }

    @Override
    public int index() {
        return index;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        IndexedCompositeExclude that = (IndexedCompositeExclude) o;
        return index == that.index;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equalsIgnoreArtifact(ExcludeSpec o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        IndexedCompositeExclude that = (IndexedCompositeExclude) o;
        return equalsIgnoreArtifact(components, that.components);
    }

    private boolean equalsIgnoreArtifact(ExcludeCollection components, ExcludeCollection other) {
        if (components == other) {
            return true;
        }
        if (components.size() != other.size()) {
            return false;
        }
        return compareExcludingArtifacts(components, other);
    }

    private boolean compareExcludingArtifacts(ExcludeCollection components, ExcludeCollection other) {
        Iterator<ExcludeSpec> fastCheckIterator = other.iterator();
        for (ExcludeSpec component : components) {
            ExcludeSpec next = fastCheckIterator.next();
            if (!next.equalsIgnoreArtifact(component)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean contains(ExcludeSpec spec) {
        IndexedExcludeSpec indexed = (IndexedExcludeSpec) spec;
        return componentsIndex.contains(indexed.index());
    }

    @Override
    public final boolean excludes(ModuleIdentifier module) {
        return queryCache.computeIfAbsent(module, this::doExcludes);
    }

    abstract boolean doExcludes(ModuleIdentifier module);

    private class ExcludeCollection implements Collection<ExcludeSpec> {

        @Override
        public int size() {
            return cardinality;
        }

        @Override
        public boolean isEmpty() {
            return cardinality == 0;
        }

        @Override
        public boolean contains(Object o) {
            if (o instanceof IndexedExcludeSpec) {
                return componentsIndex.contains(((IndexedExcludeSpec) o).index());
            }
            return false;
        }

        @Override
        public Iterator<ExcludeSpec> iterator() {
            return new ComponentIterator();
        }

        @Override
        public Object[] toArray() {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean add(ExcludeSpec excludeSpec) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            for (Object o : c) {
                if (!contains(o)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean addAll(Collection<? extends ExcludeSpec> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
            return '[' + Joiner.on(", ").join(components) + ']';
        }
    }

    private class ComponentIterator implements Iterator<ExcludeSpec> {
        private final IntIterator index = componentsIndex.iterator();

        @Override
        public boolean hasNext() {
            return index.hasNext();
        }

        @Override
        public ExcludeSpec next() {
            return getAt(index.nextInt());
        }
    }
}
