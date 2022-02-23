/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinGradleFragment

interface IdeaKotlinFragmentDependencyResolver {
    fun resolve(fragment: KotlinGradleFragment): Set<IdeaKotlinFragmentDependency>

    object Empty : IdeaKotlinFragmentDependencyResolver {
        override fun resolve(fragment: KotlinGradleFragment): Set<IdeaKotlinFragmentDependency> = emptySet()
    }
}

fun IdeaKotlinFragmentDependencyResolver(
    resolvers: Iterable<IdeaKotlinFragmentDependencyResolver>
): IdeaKotlinFragmentDependencyResolver {
    val resolversList = resolvers.toList()
    if (resolversList.isEmpty()) return IdeaKotlinFragmentDependencyResolver.Empty
    return CompositeIdeaKotlinFragmentDependencyResolver(resolversList)
}

operator fun IdeaKotlinFragmentDependencyResolver.plus(
    other: IdeaKotlinFragmentDependencyResolver
): IdeaKotlinFragmentDependencyResolver {
    if (this is CompositeIdeaKotlinFragmentDependencyResolver && other is CompositeIdeaKotlinFragmentDependencyResolver) {
        return CompositeIdeaKotlinFragmentDependencyResolver(this.children + other.children)
    }

    if (this is CompositeIdeaKotlinFragmentDependencyResolver) {
        return CompositeIdeaKotlinFragmentDependencyResolver(this.children + other)
    }

    if (other is CompositeIdeaKotlinFragmentDependencyResolver) {
        return CompositeIdeaKotlinFragmentDependencyResolver(listOf(this) + other.children)
    }

    return CompositeIdeaKotlinFragmentDependencyResolver(listOf(this, other))
}

private class CompositeIdeaKotlinFragmentDependencyResolver(
    val children: List<IdeaKotlinFragmentDependencyResolver>
) : IdeaKotlinFragmentDependencyResolver {
    override fun resolve(fragment: KotlinGradleFragment): Set<IdeaKotlinFragmentDependency> {
        return children.flatMap { child -> child.resolve(fragment) }.toSet()
    }
}

fun IdeaKotlinFragmentDependencyResolver.filterFragments(
    filter: (KotlinGradleFragment) -> Boolean
): IdeaKotlinFragmentDependencyResolver {
    return FilteredIdeaKotlinFragmentDependencyResolver(this, filter)
}

private class FilteredIdeaKotlinFragmentDependencyResolver(
    private val resolver: IdeaKotlinFragmentDependencyResolver,
    private val filter: (KotlinGradleFragment) -> Boolean
) : IdeaKotlinFragmentDependencyResolver {
    override fun resolve(fragment: KotlinGradleFragment): Set<IdeaKotlinFragmentDependency> {
        return if (filter(fragment)) resolver.resolve(fragment) else emptySet()
    }
}
