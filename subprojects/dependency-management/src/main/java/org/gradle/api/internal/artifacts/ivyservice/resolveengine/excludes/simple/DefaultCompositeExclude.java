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
package org.gradle.api.internal.artifacts.ivyservice.resolveengine.excludes.simple;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.excludes.specs.CompositeExclude;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.excludes.specs.ExcludeSpec;

import java.util.Iterator;
import java.util.stream.Stream;

abstract class DefaultCompositeExclude implements CompositeExclude {
    private final ImmutableSet<ExcludeSpec> components;
    private final int hashCode;

    DefaultCompositeExclude(ImmutableSet<ExcludeSpec> components) {
        this.components = components;
        this.hashCode = components.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DefaultCompositeExclude that = (DefaultCompositeExclude) o;
        return hashCode == that.hashCode && Objects.equal(components, that.components);
    }

    @Override
    public boolean equalsIgnoreArtifact(ExcludeSpec o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DefaultCompositeExclude that = (DefaultCompositeExclude) o;
        return equalsIgnoreArtifact(components, that.components);
    }

    private boolean equalsIgnoreArtifact(ImmutableSet<ExcludeSpec> components, ImmutableSet<ExcludeSpec> other) {
        if (components == other) {
            return true;
        }
        if (components.size() != other.size()) {
            return false;
        }
        return compareExcludingArtifacts(components, other);
    }

    private boolean compareExcludingArtifacts(ImmutableSet<ExcludeSpec> components, ImmutableSet<ExcludeSpec> other) {
        // The fast check iterator is there assuming that we have 2 collections with identical contents
        // in which case we can perform a faster check for sets, as if they were lists
        Iterator<ExcludeSpec> fastCheckIterator = other.iterator();
        for (ExcludeSpec component : components) {
            boolean found = false;
            if (fastCheckIterator != null && fastCheckIterator.next().equalsIgnoreArtifact(component)) {
                break;
            }
            // we're unlucky, sets are either different, or in a different order
            fastCheckIterator = null;
            for (ExcludeSpec o : other) {
                if (component.equalsIgnoreArtifact(o)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                // fast exit, when sets are actually different
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public final Stream<ExcludeSpec> components() {
        return components.stream();
    }

    @Override
    public ImmutableSet<ExcludeSpec> getComponents() {
        return components;
    }

    @Override
    public String toString() {
        return "{" + getDisplayName() +
            " " + components +
            '}';
    }

    protected abstract String getDisplayName();
}
