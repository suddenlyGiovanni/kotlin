/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.internal.resolve.ModuleVersionResolveException
import org.jetbrains.kotlin.gradle.kpm.external.ExternalVariantApi
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.FragmentAttributes
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinGradleFragment
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinGradleVariant

class IdeaKotlinVariantBinaryDependencyResolver(
    private val viewBinaryType: String = IdeaKotlinFragmentBinaryDependency.CLASSPATH_BINARY_TYPE,
    private val viewAttributes: FragmentAttributes<KotlinGradleFragment> = FragmentAttributes { }
) : IdeaKotlinFragmentDependencyResolver {

    override fun resolve(fragment: KotlinGradleFragment): Set<IdeaKotlinFragmentBinaryDependency> {
        if (fragment !is KotlinGradleVariant) return emptySet()

        val artifacts = fragment.compileDependenciesConfiguration.incoming.artifactView { view ->
            viewAttributes.setAttributes(view.attributes, fragment)
            view.lenient(true)
            view.componentFilter { component -> component !is ProjectComponentIdentifier }
        }.artifacts

        val unresolvedDependencies = artifacts.failures
            .onEach { reason -> fragment.project.logger.error("Failed to resolve dependency", reason) }
            .map { reason ->
                val selector = (reason as? ModuleVersionResolveException)?.selector as? ModuleComponentSelector
                /* Can't figure out the dependency here :( */
                    ?: return@map IdeaKotlinFragmentUnresolvedBinaryDependencyImpl(
                        coordinates = null, cause = reason.message?.takeIf { it.isNotBlank() }
                    )

                IdeaKotlinFragmentUnresolvedBinaryDependencyImpl(
                    coordinates = BinaryCoordinatesImpl(selector.group, selector.module, selector.version),
                    cause = reason.message?.takeIf { it.isNotBlank() }
                )
            }.toSet()

        val resolvedDependencies = artifacts.artifacts.mapNotNull { artifact ->
            val identifier = artifact.variant.owner as? ModuleComponentIdentifier ?: return@mapNotNull null

            IdeaKotlinFragmentResolvedBinaryDependencyImpl(
                coordinates = BinaryCoordinatesImpl(identifier.group, identifier.module, identifier.version),
                binaryType = viewBinaryType,
                binaryFile = artifact.file
            )
        }.toSet()

        return resolvedDependencies + unresolvedDependencies
    }
}
