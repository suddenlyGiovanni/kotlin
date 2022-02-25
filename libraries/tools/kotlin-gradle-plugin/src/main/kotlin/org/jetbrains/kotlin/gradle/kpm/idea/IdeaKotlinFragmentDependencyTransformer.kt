/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinGradleFragment

interface IdeaKotlinFragmentDependencyTransformer {
    fun transform(
        fragment: KotlinGradleFragment, dependencies: Set<IdeaKotlinFragmentDependency>
    ): Set<IdeaKotlinFragmentDependency>

    object Empty : IdeaKotlinFragmentDependencyTransformer {
        override fun transform(
            fragment: KotlinGradleFragment, dependencies: Set<IdeaKotlinFragmentDependency>
        ): Set<IdeaKotlinFragmentDependency> = dependencies
    }
}

fun IdeaKotlinFragmentDependencyTransformer(
    transformers: List<IdeaKotlinFragmentDependencyTransformer>
): IdeaKotlinFragmentDependencyTransformer = CompositeIdeaKotlinFragmentDependencyTransformer(transformers)

operator fun IdeaKotlinFragmentDependencyTransformer.plus(other: IdeaKotlinFragmentDependencyTransformer):
        IdeaKotlinFragmentDependencyTransformer {
    if (this is CompositeIdeaKotlinFragmentDependencyTransformer && other is CompositeIdeaKotlinFragmentDependencyTransformer) {
        return CompositeIdeaKotlinFragmentDependencyTransformer(this.transformers + other.transformers)
    }

    if (this is CompositeIdeaKotlinFragmentDependencyTransformer) {
        return CompositeIdeaKotlinFragmentDependencyTransformer(this.transformers + other)
    }

    if (other is CompositeIdeaKotlinFragmentDependencyTransformer) {
        return CompositeIdeaKotlinFragmentDependencyTransformer(listOf(this) + other.transformers)
    }

    return CompositeIdeaKotlinFragmentDependencyTransformer(listOf(this, other))
}

private class CompositeIdeaKotlinFragmentDependencyTransformer(
    val transformers: List<IdeaKotlinFragmentDependencyTransformer>
) : IdeaKotlinFragmentDependencyTransformer {
    override fun transform(
        fragment: KotlinGradleFragment, dependencies: Set<IdeaKotlinFragmentDependency>
    ): Set<IdeaKotlinFragmentDependency> {
        return transformers.fold(dependencies) { currentDependencies, transformer -> transformer.transform(fragment, currentDependencies) }
    }

}
