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

import it.unimi.dsi.fastutil.ints.IntSet;
import org.gradle.api.artifacts.ModuleIdentifier;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.excludes.specs.ExcludeAllOf;
import org.gradle.internal.component.model.IvyArtifactName;

final class IndexedAllOf extends IndexedCompositeExclude implements ExcludeAllOf {
    IndexedAllOf(int index, IntSet components, IndexedExcludeFactory factory) {
        super(index, components, factory);
    }

    @Override
    boolean doExcludes(ModuleIdentifier module) {
        return components().allMatch(c -> c.excludes(module));
    }

    @Override
    public boolean excludesArtifact(ModuleIdentifier module, IvyArtifactName artifactName) {
        return components().allMatch(c -> c.excludesArtifact(module, artifactName));
    }

    @Override
    public boolean mayExcludeArtifacts() {
        return components().allMatch(c -> c.mayExcludeArtifacts());
    }

    @Override
    public String toString() {
        return "At " + index() + " {exclude all of " + getComponents() + "}";
    }
}
