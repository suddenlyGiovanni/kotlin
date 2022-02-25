/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.gradle.api.artifacts.ResolvableDependencies
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.internal.resolve.ModuleVersionResolveException
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.FragmentAttributes
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinGradleFragment
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinGradleVariant
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.disambiguateName

class IdeaKotlinVariantBinaryDependencyResolver(
    private val dependenciesProvider: DependenciesProvider = CompileDependenciesProvider,
    private val viewBinaryType: String = IdeaKotlinFragmentBinaryDependency.CLASSPATH_BINARY_TYPE,
    private val viewAttributes: FragmentAttributes<KotlinGradleFragment> = FragmentAttributes { }
) : IdeaKotlinFragmentDependencyResolver {

    interface DependenciesProvider {
        fun resolvableDependencies(fragment: KotlinGradleFragment): ResolvableDependencies?
    }

    object CompileDependenciesProvider : DependenciesProvider {
        override fun resolvableDependencies(fragment: KotlinGradleFragment): ResolvableDependencies? {
            if (fragment !is KotlinGradleVariant) return null
            return fragment.compileDependenciesConfiguration.incoming
        }
    }

    object DeclaredDependenciesProvider : DependenciesProvider {
        override fun resolvableDependencies(fragment: KotlinGradleFragment): ResolvableDependencies? {
            val results = mutableListOf<MetadataDependencyResolution.KeepOriginalDependency>()

            results.single().dependency.variants.map {
                it.
            }
            fragment.project.configurations.detachedConfiguration(
                *results.map { it }.toTypedArray()
            )

            // TODO: Dependency Consistency Scope
            val configuration = fragment.project.configurations
                .maybeCreate(fragment.disambiguateName("resolvableApiAndImplementationDependencies"))
            configuration.extendsFrom(fragment.transitiveApiConfiguration, fragment.transitiveImplementationConfiguration)
            return configuration.incoming
        }
    }

    override fun resolve(fragment: KotlinGradleFragment): Set<IdeaKotlinFragmentBinaryDependency> {
        val dependencies = dependenciesProvider.resolvableDependencies(fragment) ?: return emptySet()
        val artifacts = dependencies.artifactView { view ->
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
                    coordinates = IdeaKotlinBinaryCoordinatesImpl(selector.group, selector.module, selector.version),
                    cause = reason.message?.takeIf { it.isNotBlank() }
                )
            }.toSet()

        val resolvedDependencies = artifacts.artifacts.mapNotNull { artifact ->
            IdeaKotlinFragmentResolvedBinaryDependencyImpl(
                coordinates = artifact.variant.owner.ideaKotlinBinaryCoordinates,
                binaryType = viewBinaryType,
                binaryFile = artifact.file
            )
        }.toSet()

        return resolvedDependencies + unresolvedDependencies
    }
}

private val ComponentIdentifier.ideaKotlinBinaryCoordinates: IdeaKotlinBinaryCoordinates?
    get() = when (this) {
        is ModuleComponentIdentifier -> IdeaKotlinBinaryCoordinatesImpl(group, module, version)
        else -> null
    }
