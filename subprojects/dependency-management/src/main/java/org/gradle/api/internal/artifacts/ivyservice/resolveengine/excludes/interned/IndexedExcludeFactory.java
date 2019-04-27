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

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.gradle.api.artifacts.ModuleIdentifier;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.excludes.specs.ExcludeFactory;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.excludes.specs.ExcludeSpec;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.excludes.factories.Optimizations;
import org.gradle.internal.component.model.IvyArtifactName;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

public class IndexedExcludeFactory implements ExcludeFactory {
    private static final int NOTHING_INDEX = 0;
    private static final int EVERYTHING_INDEX = 1;

    private final List<IndexedExcludeSpec> excludes;

    // interning structures
    private int index;
    private final ExcludeIndexer<String, IndexedExcludeSpec> groupCache;
    private final ExcludeIndexer<String, IndexedExcludeSpec> moduleCache;
    private final ExcludeIndexer<ModuleIdentifier, IndexedExcludeSpec> moduleIdCache;
    private final ExcludeIndexer<Set<ModuleIdentifier>, IndexedExcludeSpec> moduleSetCache;
    private final ExcludeIndexer<ModuleArtifactKey, IndexedExcludeSpec> artifactCache;
    private final ExcludeIndexer<IntSet, IndexedCompositeExclude> anyCache;
    private final ExcludeIndexer<IntSet, IndexedCompositeExclude> allCache;
    private final ExcludeIndexer<IvyPatternKey, IndexedExcludeSpec> ivyPatternsCache;

    public IndexedExcludeFactory(ExcludeFactory delegate) {
        this.excludes = initializeExcludes(delegate);
        this.groupCache = createCache((key, idx) -> new IndexedGroupExclude(idx, delegate.group(key)));
        this.moduleCache = createCache((key, idx) -> new IndexedModuleExclude(idx, delegate.module(key)));
        this.moduleIdCache = createCache((key, idx) -> new IndexedModuleIdExclude(idx, delegate.moduleId(key)));
        this.moduleSetCache = createCache((key, idx) -> new IndexedModuleIdSetExclude(idx, delegate.moduleSet(key)));
        this.artifactCache = createCache((key, idx) -> new IndexedModuleArtifactExclude(idx, delegate.artifact(key.moduleId, key.artifact)));
        this.anyCache = createCache((key, idx) -> new IndexedAnyOf(idx, key, IndexedExcludeFactory.this));
        this.allCache = createCache((key, idx) -> new IndexedAllOf(idx, key, IndexedExcludeFactory.this));
        this.ivyPatternsCache = createCache((key, idx) -> new IndexedIvyPatternExclude(idx, delegate.ivyPatternExclude(key.moduleId, key.artifact, key.matcher)));
    }

    private <T, E extends IndexedExcludeSpec> ExcludeIndexer<T, E> createCache(BiFunction<T, Integer, E> producer) {
        return new ExcludeIndexer<T, E>(producer);
    }

    IndexedExcludeSpec getAt(int index) {
        return excludes.get(index);
    }

    private ArrayList<IndexedExcludeSpec> initializeExcludes(ExcludeFactory delegate) {
        ArrayList<IndexedExcludeSpec> indexedExcludes = Lists.newArrayListWithExpectedSize(1024);
        indexedExcludes.add(new IndexedExcludeNothing(NOTHING_INDEX, delegate.nothing()));
        indexedExcludes.add(new IndexedExcludeEverything(EVERYTHING_INDEX, delegate.everything()));
        index = indexedExcludes.size();
        return indexedExcludes;
    }

    @Override
    public ExcludeSpec nothing() {
        return excludes.get(NOTHING_INDEX);
    }

    @Override
    public ExcludeSpec everything() {
        return excludes.get(EVERYTHING_INDEX);
    }

    @Override
    public ExcludeSpec group(String group) {
        return groupCache.get(group);
    }

    @Override
    public ExcludeSpec module(String module) {
        return moduleCache.get(module);
    }

    @Override
    public ExcludeSpec moduleId(ModuleIdentifier id) {
        return moduleIdCache.get(id);
    }

    @Override
    public ExcludeSpec artifact(ModuleIdentifier id, IvyArtifactName artifact) {
        return artifactCache.get(new ModuleArtifactKey(id, artifact));
    }

    @Override
    public ExcludeSpec moduleSet(Set<ModuleIdentifier> modules) {
        return moduleSetCache.get(modules);
    }

    private static int indexOf(ExcludeSpec spec) {
        return ((IndexedExcludeSpec) spec).index();
    }

    @Override
    public ExcludeSpec anyOf(ExcludeSpec one, ExcludeSpec two) {
        return Optimizations.optimizeAnyOf(one, two, (left, right) -> {
            IntSet key = new IntRBTreeSet();
            key.add(indexOf(left));
            key.add(indexOf(right));
            return anyCache.get(key);
        });
    }

    @Override
    public ExcludeSpec allOf(ExcludeSpec one, ExcludeSpec two) {
        return Optimizations.optimizeAllOf(this, one, two, (left, right) -> {
            IntSet key = new IntRBTreeSet();
            key.add(indexOf(left));
            key.add(indexOf(right));
            return allCache.get(key);
        });
    }

    @Override
    public ExcludeSpec anyOf(List<ExcludeSpec> specs) {
        IntSet key = new IntRBTreeSet();
        for (ExcludeSpec spec : specs) {
            key.add(indexOf(spec));
        }
        return anyCache.get(key);
    }

    @Override
    public ExcludeSpec allOf(List<ExcludeSpec> specs) {
        IntSet key = new IntRBTreeSet();
        for (ExcludeSpec spec : specs) {
            key.add(indexOf(spec));
        }
        return allCache.get(key);
    }

    @Override
    public ExcludeSpec ivyPatternExclude(ModuleIdentifier moduleId, IvyArtifactName artifact, String matcher) {
        return ivyPatternsCache.get(new IvyPatternKey(moduleId, artifact, matcher));
    }

    private static class ModuleArtifactKey {
        protected final ModuleIdentifier moduleId;
        protected final IvyArtifactName artifact;
        private final int hashCode;

        private ModuleArtifactKey(ModuleIdentifier moduleId, IvyArtifactName artifact) {
            this.moduleId = moduleId;
            this.artifact = artifact;
            this.hashCode = 31 * (moduleId == null ? 0 : moduleId.hashCode()) + (artifact == null ? 0 : artifact.hashCode());
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ModuleArtifactKey that = (ModuleArtifactKey) o;

            if (hashCode != that.hashCode) {
                return false;
            }
            if (moduleId != null ? !moduleId.equals(that.moduleId) : that.moduleId != null) {
                return false;
            }
            return artifact != null ? artifact.equals(that.artifact) : that.artifact == null;

        }

        @Override
        public int hashCode() {
            return hashCode;
        }
    }

    private static class IvyPatternKey extends ModuleArtifactKey {
        private final String matcher;
        private final int hashCode;

        private IvyPatternKey(ModuleIdentifier moduleId, IvyArtifactName artifact, String matcher) {
            super(moduleId, artifact);
            this.matcher = matcher;
            this.hashCode = 31 * super.hashCode() + matcher.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            if (!super.equals(o)) {
                return false;
            }
            IvyPatternKey that = (IvyPatternKey) o;
            return hashCode == that.hashCode &&
                Objects.equal(matcher, that.matcher);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
    }

    private class ExcludeIndexer<T, E extends IndexedExcludeSpec>  {
        private final BiFunction<T, Integer, E> producer;
        private final Map<T, E> backingMap = Maps.newConcurrentMap();

        private ExcludeIndexer(BiFunction<T, Integer, E> producer) {
            this.producer = producer;
        }

        public E get(T key) {
            return backingMap.computeIfAbsent(key, e -> {
                // optimistic synchronization
                // despite the fact we use a concurrent map,
                // we must make sure that we only compute the value once
                // so we synchronize on excludes then recheck if another
                // thread has computed the result in between.
                // Because there's a cache in front of this, it's unlikely
                // to get high contention
                E result;
                synchronized (excludes) {
                    E recheck = backingMap.get(key);
                    if (recheck != null) {
                        return recheck;
                    }
                    result = producer.apply(key, index++);
                    excludes.add(result);
                }
                return result;
            });
        }

    }
}
