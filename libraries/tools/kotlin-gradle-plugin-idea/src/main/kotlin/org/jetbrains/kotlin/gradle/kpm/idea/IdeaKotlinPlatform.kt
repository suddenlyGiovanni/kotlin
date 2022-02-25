/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import java.io.Serializable

sealed interface IdeaKotlinPlatform : Serializable {
    val name: String

    companion object {
        val wasm: IdeaKotlinPlatform = IdeaKotlinPlatformImpl("wasm")
        val jvm: IdeaKotlinPlatform = IdeaKotlinPlatformImpl("jvm")
        val js: IdeaKotlinPlatform = IdeaKotlinPlatformImpl("js")
        val native: IdeaKotlinPlatform = IdeaKotlinPlatformImpl("native")
    }
}

private data class IdeaKotlinPlatformImpl(override val name: String) : IdeaKotlinPlatform {
    companion object {
        const val serialVersionUID = 0L
    }
}
