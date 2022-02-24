/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.gradle.kpm.KotlinExternalModelContainer
import java.io.File
import java.io.Serializable

sealed interface IdeaKotlinFragmentDependency : Serializable {
    val external: KotlinExternalModelContainer
}

sealed interface IdeaKotlinFragmentSourceDependency : IdeaKotlinFragmentDependency {
    val buildId: String
    val projectPath: String
    val projectName: String
    val kotlinModuleName: String
    val kotlinFragmentName: String
}

sealed interface BinaryCoordinates : Serializable {
    val group: String
    val module: String
    val version: String
    val kotlinModuleName: String?
    val kotlinFragmentName: String?
}

sealed interface IdeaKotlinFragmentBinaryDependency : IdeaKotlinFragmentDependency {
    val coordinates: BinaryCoordinates?

    companion object {
        const val CLASSPATH_BINARY_TYPE = "org.jetbrains.binary.type.classpath"
        const val SOURCES_BINARY_TYPE = "org.jetbrains.binary.type.sources"
        const val DOCUMENTATION_BINARY_TYPE = "org.jetbrains.binary.type.documentation"
    }
}

sealed interface IdeaKotlinFragmentUnresolvedBinaryDependency : IdeaKotlinFragmentBinaryDependency {
    val cause: String?
}

sealed interface IdeaKotlinFragmentResolvedBinaryDependency : IdeaKotlinFragmentBinaryDependency {
    val binaryType: String
    val binaryFile: File
}

@InternalKotlinGradlePluginApi
data class IdeaKotlinFragmentSourceDependencyImpl(
    override val buildId: String, // TODO is `isCurrentBuild` used?
    override val projectPath: String,
    override val projectName: String,
    override val kotlinModuleName: String,
    override val kotlinFragmentName: String,
    override val external: KotlinExternalModelContainer = KotlinExternalModelContainer.Empty
) : IdeaKotlinFragmentSourceDependency {

    @InternalKotlinGradlePluginApi
    companion object {
        private const val serialVersionUID = 0L
    }
}

@InternalKotlinGradlePluginApi
data class BinaryCoordinatesImpl(
    override val group: String,
    override val module: String,
    override val version: String,
    override val kotlinModuleName: String? = null,
    override val kotlinFragmentName: String? = null
) : BinaryCoordinates {

    override fun toString(): String {
        return "$group:$module:$version" +
                (if (kotlinModuleName != null) ":$kotlinModuleName" else "") +
                (if (kotlinFragmentName != null) ":$kotlinFragmentName" else "")
    }

    companion object {
        private const val serialVersionUID = 0L
    }
}

@InternalKotlinGradlePluginApi
data class IdeaKotlinFragmentResolvedBinaryDependencyImpl(
    override val coordinates: BinaryCoordinates?,
    override val binaryType: String,
    override val binaryFile: File,
    override val external: KotlinExternalModelContainer = KotlinExternalModelContainer.Empty
) : IdeaKotlinFragmentResolvedBinaryDependency {

    override fun toString(): String {
        val readableBinaryType = when (binaryType) {
            IdeaKotlinFragmentBinaryDependency.CLASSPATH_BINARY_TYPE -> "classpath"
            IdeaKotlinFragmentBinaryDependency.DOCUMENTATION_BINARY_TYPE -> "documentation"
            IdeaKotlinFragmentBinaryDependency.SOURCES_BINARY_TYPE -> "sources"
            else -> binaryType
        }

        return "$readableBinaryType://$coordinates/${binaryFile.name}"
    }

    @InternalKotlinGradlePluginApi
    companion object {
        private const val serialVersionUID = 0L
    }
}

@InternalKotlinGradlePluginApi
data class IdeaKotlinFragmentUnresolvedBinaryDependencyImpl(
    override val cause: String?,
    override val coordinates: BinaryCoordinates?,
    override val external: KotlinExternalModelContainer = KotlinExternalModelContainer.Empty
) : IdeaKotlinFragmentUnresolvedBinaryDependency {

    @InternalKotlinGradlePluginApi
    companion object {
        private const val serialVersionUID = 0L
    }
}
