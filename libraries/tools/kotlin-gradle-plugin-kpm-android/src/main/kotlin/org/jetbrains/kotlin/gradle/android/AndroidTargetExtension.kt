/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")
@file:OptIn(ExternalVariantApi::class)

package org.jetbrains.kotlin.gradle.android

import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.internal.publishing.AndroidArtifacts
import com.android.build.gradle.internal.utils.isKotlinAndroidPluginApplied
import org.gradle.api.internal.component.ArtifactType
import org.gradle.kotlin.dsl.findByType
import org.jetbrains.kotlin.gradle.kpm.external.ExternalVariantApi
import org.jetbrains.kotlin.gradle.kpm.external.external
import org.jetbrains.kotlin.gradle.kpm.external.ideaKotlinProjectModelBuilder
import org.jetbrains.kotlin.gradle.kpm.external.project
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKotlinFragmentBinaryDependency.Companion.CLASSPATH_BINARY_TYPE
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKotlinFragmentDependencyResolver
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKotlinVariantBinaryDependencyResolver
import org.jetbrains.kotlin.gradle.kpm.idea.filterFragments
import org.jetbrains.kotlin.gradle.kpm.idea.plus
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinVariant
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.FragmentAttributes
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinGradleVariant
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinPm20ProjectExtension
import org.jetbrains.kotlin.project.model.utils.findRefiningFragments
import org.jetbrains.kotlin.project.model.utils.variantsContainingFragment

fun KotlinPm20ProjectExtension.android() {
    project.extensions.findByType<AppExtension>()?.applicationVariants?.all { androidVariant ->
        main { createKotlinAndroidVariant(androidVariant) }
        test { createKotlinAndroidVariant(androidVariant.unitTestVariant ?: return@test) }
        instrumentedTest { createKotlinAndroidVariant(androidVariant.testVariant ?: return@instrumentedTest) }
    }

    project.extensions.findByType<LibraryExtension>()?.libraryVariants?.all { androidVariant ->
        main { createKotlinAndroidVariant(androidVariant) }
        test { createKotlinAndroidVariant(androidVariant.unitTestVariant ?: return@test) }
        instrumentedTest { createKotlinAndroidVariant(androidVariant.testVariant ?: return@instrumentedTest) }
    }

    setupIdeaKotlinFragmentDependencyResolver()
}

private fun KotlinPm20ProjectExtension.setupIdeaKotlinFragmentDependencyResolver() {
    ideaKotlinProjectModelBuilder.registerDependencyResolver(
        (IdeaKotlinVariantBinaryDependencyResolver(
            viewBinaryType = "manifest",
            viewAttributes = FragmentAttributes {
                attribute(AndroidArtifacts.ARTIFACT_TYPE, AndroidArtifacts.ArtifactType.MANIFEST.type)
            }
        ) + IdeaKotlinVariantBinaryDependencyResolver(
            viewBinaryType = "resources",
            viewAttributes = FragmentAttributes {
                attribute(AndroidArtifacts.ARTIFACT_TYPE, AndroidArtifacts.ArtifactType.ANDROID_RES.type)
            }
        ) + IdeaKotlinVariantBinaryDependencyResolver(
            viewBinaryType = "keep-rules",
            viewAttributes = FragmentAttributes {
                attribute(AndroidArtifacts.ARTIFACT_TYPE, AndroidArtifacts.ArtifactType.KEEP_RULES.type)
            }
        ) + IdeaKotlinVariantBinaryDependencyResolver(
            viewBinaryType = "android-symbol",
            viewAttributes = FragmentAttributes {
                attribute(AndroidArtifacts.ARTIFACT_TYPE, AndroidArtifacts.ArtifactType.COMPILE_SYMBOL_LIST.type)
            }
        )).filterFragments { fragment ->
            if (fragment is KotlinGradleVariant) {
                return@filterFragments androidDslKey in fragment.external
            }

            val variants = fragment.containingModule.variantsContainingFragment(fragment).toList()
            if (variants.isEmpty()) return@filterFragments false
            variants.all { variant -> variant is KotlinGradleVariant && androidDslKey in variant.external }
        }
    )

    ideaKotlinProjectModelBuilder.registerDependencyResolver(
        IdeaKotlinVariantBinaryDependencyResolver(
            IdeaKotlinVariantBinaryDependencyResolver.DeclaredDependenciesProvider,
            viewBinaryType = CLASSPATH_BINARY_TYPE,
            viewAttributes = FragmentAttributes {
                attribute(AndroidArtifacts.ARTIFACT_TYPE, AndroidArtifacts.ArtifactType.CLASSES_JAR.type)
            }
        ).filterFragments { fragment -> fragment !is KotlinGradleVariant && androidDslKey in fragment.external }
    )
}
