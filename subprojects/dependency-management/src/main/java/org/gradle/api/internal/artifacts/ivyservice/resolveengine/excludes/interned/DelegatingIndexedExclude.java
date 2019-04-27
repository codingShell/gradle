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

import org.gradle.api.artifacts.ModuleIdentifier;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.excludes.specs.ExcludeSpec;
import org.gradle.internal.component.model.IvyArtifactName;

class DelegatingIndexedExclude implements IndexedExcludeSpec {
    private final int index;
    private final ExcludeSpec delegate;
    private final int hashCode;

    DelegatingIndexedExclude(int index, ExcludeSpec delegate) {
        this.index = index;
        this.delegate = delegate;
        this.hashCode = 31 * index + delegate.hashCode();
    }

    @Override
    public boolean excludes(ModuleIdentifier module) {
        return delegate.excludes(module);
    }

    @Override
    public boolean excludesArtifact(ModuleIdentifier module, IvyArtifactName artifactName) {
        return delegate.excludesArtifact(module, artifactName);
    }

    @Override
    public boolean mayExcludeArtifacts() {
        return delegate.mayExcludeArtifacts();
    }

    @Override
    public boolean equalsIgnoreArtifact(ExcludeSpec o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DelegatingIndexedExclude that = (DelegatingIndexedExclude) o;
        return that.index == index || delegate.equalsIgnoreArtifact(that.delegate);
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
        DelegatingIndexedExclude that = (DelegatingIndexedExclude) o;
        return index == that.index;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return "At " + index + ", " + delegate;
    }
}
