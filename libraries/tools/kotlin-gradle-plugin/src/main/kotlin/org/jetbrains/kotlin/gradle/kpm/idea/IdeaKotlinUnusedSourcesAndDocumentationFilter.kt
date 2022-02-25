/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinGradleFragment

internal object IdeaKotlinUnusedSourcesAndDocumentationFilter : IdeaKotlinFragmentDependencyTransformer {
    override fun transform(
        fragment: KotlinGradleFragment, dependencies: Set<IdeaKotlinFragmentDependency>
    ): Set<IdeaKotlinFragmentDependency> {
        val sourcesAndDocumentationDependencies = dependencies
            .filterIsInstance<IdeaKotlinFragmentResolvedBinaryDependency>()
            .filter { dependency -> dependency.isSourcesType || dependency.isDocumentationType }
            .toSet()

        val classpathCoordinates = dependencies.filterIsInstance<IdeaKotlinFragmentResolvedBinaryDependency>()
            .filter { dependency -> dependency.isClasspathType }
            .mapNotNull { it.coordinates }
            .toSet()

        val unusedSourcesAndDocumentationDependencies = sourcesAndDocumentationDependencies
            .filter { it.coordinates !in classpathCoordinates }
            .toSet()

        return dependencies - unusedSourcesAndDocumentationDependencies
    }
}
