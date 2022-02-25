/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

@Suppress("unused") /* Receiver acts as scope, or key to that function */
internal fun IdeaKotlinFragmentBuildingContext.buildIdeaKotlinPlatform(platformType: KotlinPlatformType): IdeaKotlinPlatform {
    return when (platformType) {
        KotlinPlatformType.jvm, KotlinPlatformType.androidJvm -> IdeaKotlinPlatform.jvm
        KotlinPlatformType.js -> IdeaKotlinPlatform.js
        KotlinPlatformType.native -> IdeaKotlinPlatform.native
        KotlinPlatformType.wasm -> IdeaKotlinPlatform.wasm
        KotlinPlatformType.common -> throw IllegalArgumentException("Unexpected variant platform: ${KotlinPlatformType.common}")
    }
}
