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

import org.gradle.api.internal.artifacts.ivyservice.resolveengine.excludes.specs.ExcludeSpec;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.excludes.specs.IvyPatternMatcherExcludeRuleSpec;
import org.gradle.internal.component.model.IvyArtifactName;

final class IndexedIvyPatternExclude extends DelegatingIndexedExclude implements IvyPatternMatcherExcludeRuleSpec {
    private final IvyPatternMatcherExcludeRuleSpec delegate;

    IndexedIvyPatternExclude(int index, ExcludeSpec delegate) {
        super(index, delegate);
        this.delegate = (IvyPatternMatcherExcludeRuleSpec) delegate;
    }

    @Override
    public IvyArtifactName getArtifact() {
        return delegate.getArtifact();
    }

}
