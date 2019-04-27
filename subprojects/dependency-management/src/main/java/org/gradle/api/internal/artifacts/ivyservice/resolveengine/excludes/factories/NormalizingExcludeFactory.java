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
package org.gradle.api.internal.artifacts.ivyservice.resolveengine.excludes.factories;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.excludes.specs.CompositeExclude;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.excludes.specs.ExcludeEverything;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.excludes.specs.ExcludeFactory;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.excludes.specs.ExcludeNothing;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.excludes.specs.ExcludeSpec;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.excludes.specs.ModuleIdExclude;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This factory performs normalization of exclude rules. This is the smartest
 * of all factories and is responsible for doing some basic algebra computations.
 * It shouldn't be too slow, or the whole chain will pay the price.
 */
public class NormalizingExcludeFactory extends DelegatingExcludeFactory {
    public NormalizingExcludeFactory(ExcludeFactory delegate) {
        super(delegate);
    }

    @Override
    public ExcludeSpec anyOf(ExcludeSpec one, ExcludeSpec two) {
        return doUnion(ImmutableList.of(one, two));
    }

    @Override
    public ExcludeSpec allOf(ExcludeSpec one, ExcludeSpec two) {
        return doIntersect(ImmutableList.of(one, two));
    }

    @Override
    public ExcludeSpec anyOf(List<ExcludeSpec> specs) {
        return doUnion(specs);
    }

    @Override
    public ExcludeSpec allOf(List<ExcludeSpec> specs) {
        return doIntersect(specs);
    }

    private ExcludeSpec doUnion(List<ExcludeSpec> specs) {
        Set<ModuleIdExclude> simpleExcludes = Sets.newHashSetWithExpectedSize(specs.size());
        for (ExcludeSpec spec : specs) {
            if (spec instanceof ExcludeEverything) {
                return everything();
            }
            if (spec instanceof ModuleIdExclude) {
                simpleExcludes.add((ModuleIdExclude) spec);
            }
        }
        // merge all single module id into an id set
        if (simpleExcludes.size() > 1) {
            specs = specs.stream().filter(e -> !simpleExcludes.contains(e)).collect(Collectors.toList());
            specs.add(delegate.moduleSet(simpleExcludes.stream().map(ModuleIdExclude::getModuleId).collect(Collectors.toSet())));
        }
        specs = reduce(specs);
        return Optimizations.optimizeList(this, specs, delegate::anyOf);
    }

    // Optimizes (A ∪ B) ∩ A = A and (A ∩ B) ∪ A = A
    private List<ExcludeSpec> reduce(List<ExcludeSpec> specs) {
        Set<ExcludeSpec> asSet = ImmutableSet.copyOf(specs);
        return specs.stream()
            .flatMap(e -> {
                if (e instanceof CompositeExclude) {
                    CompositeExclude exc = (CompositeExclude) e;
                    if (exc.components().anyMatch(asSet::contains)) {
                        return Stream.of();
                    }
                }
                return Stream.of(e);
            })
            .collect(Collectors.toList());
    }

    private ExcludeSpec doIntersect(List<ExcludeSpec> specs) {
        if (containsExcludeNothing(specs)) {
            return nothing();
        }
        specs = reduce(specs);
        return Optimizations.optimizeList(this, specs, delegate::allOf);
    }

    private static boolean containsExcludeNothing(List<ExcludeSpec> specs) {
        return specs.stream().anyMatch(ExcludeNothing.class::isInstance);
    }

}
