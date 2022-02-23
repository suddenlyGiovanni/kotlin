/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution
import org.jetbrains.kotlin.gradle.plugin.mpp.getSourceSetCompiledMetadata
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.FragmentGranularMetadataResolverFactory
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinGradleFragment
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinGradleModule.Companion.moduleName
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinGradleVariant
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.toModuleDependency
import org.jetbrains.kotlin.gradle.plugin.sources.SourceSetMetadataStorageForIde
import org.jetbrains.kotlin.project.model.KotlinModuleIdentifier
import org.jetbrains.kotlin.project.model.LocalModuleIdentifier

internal class IdeaKotlinMetadataDependencyResolver(
    private val fragmentGranularMetadataResolverFactory: FragmentGranularMetadataResolverFactory = FragmentGranularMetadataResolverFactory()
) : IdeaKotlinFragmentDependencyResolver {

    override fun resolve(fragment: KotlinGradleFragment): Set<IdeaKotlinFragmentDependency> {
        if (fragment is KotlinGradleVariant) return emptySet() // <- use variant resolver!
        val fragmentGranularMetadataResolver = fragmentGranularMetadataResolverFactory.getOrCreate(fragment)
        return fragmentGranularMetadataResolver.resolutions.flatMap { resolution ->
            resolveMetadataDependencies(fragment, resolution)
        }.toSet()
    }
}

private fun resolveMetadataDependencies(
    fragment: KotlinGradleFragment, resolution: MetadataDependencyResolution
): List<IdeaKotlinFragmentDependency> {
    val moduleIdentifier = resolution.dependency.toModuleDependency().moduleIdentifier
    return when (val dependencyId = resolution.dependency.id) {
        is ProjectComponentIdentifier ->
            resolveFragmentSourceDependencies(resolution, dependencyId, moduleIdentifier)
        is ModuleComponentIdentifier ->
            resolveFragmentBinaryDependencies(fragment, resolution, dependencyId, moduleIdentifier)
        else -> emptyList()
    }
}

private fun resolveFragmentSourceDependencies(
    resolution: MetadataDependencyResolution,
    dependencyId: ProjectComponentIdentifier,
    kotlinModuleIdentifier: KotlinModuleIdentifier
): List<IdeaKotlinFragmentSourceDependency> {
    if (kotlinModuleIdentifier !is LocalModuleIdentifier) return emptyList()
    if (resolution !is MetadataDependencyResolution.ChooseVisibleSourceSets) return emptyList()
    return resolution.allVisibleSourceSetNames.map { fragmentName ->
        IdeaKotlinFragmentSourceDependencyImpl(
            buildId = dependencyId.build.name /* TODO? */,
            projectPath = dependencyId.projectPath,
            projectName = dependencyId.projectName,
            kotlinModuleName = kotlinModuleIdentifier.moduleName,
            kotlinFragmentName = fragmentName
        )
    }
}

private fun resolveFragmentBinaryDependencies(
    fragment: KotlinGradleFragment,
    resolution: MetadataDependencyResolution,
    dependencyId: ModuleComponentIdentifier,
    kotlinModuleIdentifier: KotlinModuleIdentifier,
): List<IdeaKotlinFragmentBinaryDependency> {
    return when (resolution) {
        is MetadataDependencyResolution.ChooseVisibleSourceSets ->
            resolveTransformedFragmentBinaryDependency(fragment, resolution, dependencyId, kotlinModuleIdentifier)
        is MetadataDependencyResolution.KeepOriginalDependency -> emptyList() // TODO
        is MetadataDependencyResolution.ExcludeAsUnrequested -> emptyList()
    }
}

private fun resolveTransformedFragmentBinaryDependency(
    fragment: KotlinGradleFragment,
    resolution: MetadataDependencyResolution.ChooseVisibleSourceSets,
    dependencyId: ModuleComponentIdentifier,
    kotlinModuleIdentifier: KotlinModuleIdentifier,
): List<IdeaKotlinFragmentBinaryDependency> {
    return resolution.allVisibleSourceSetNames
        .flatMap { fragmentName ->
            val binaryCoordinates = BinaryCoordinatesImpl(
                dependencyId.group, dependencyId.module, dependencyId.version, kotlinModuleIdentifier.moduleName, fragmentName
            )

            resolution.metadataProvider.getSourceSetCompiledMetadata(
                project = fragment.project,
                sourceSetName = fragmentName,
                outputDirectoryWhenMaterialised = SourceSetMetadataStorageForIde.sourceSetStorage(fragment.project, fragmentName),
                materializeFilesIfNecessary = true
            ).map { binaryFile ->
                IdeaKotlinFragmentResolvedBinaryDependencyImpl(
                    coordinates = binaryCoordinates,
                    binaryType = IdeaKotlinFragmentBinaryDependency.CLASSPATH_BINARY_TYPE,
                    binaryFile = binaryFile
                )
            }
        }
}
