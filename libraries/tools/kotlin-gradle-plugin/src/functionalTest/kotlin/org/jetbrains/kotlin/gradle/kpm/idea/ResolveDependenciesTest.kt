/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.kpm.idea

import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import org.gradle.kotlin.dsl.repositories
import org.jetbrains.kotlin.gradle.android.android
import org.jetbrains.kotlin.gradle.kpm.AbstractKpmExtensionTest
import org.jetbrains.kotlin.gradle.kpm.external.ExternalVariantApi
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinLinuxX64Variant
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinMacosX64Variant
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.jvm
import org.jetbrains.kotlin.gradle.propertiesExtension
import org.junit.Test
import kotlin.test.fail

class ResolveDependenciesTest : AbstractKpmExtensionTest() {

    @Test
    fun `test - resolve stdlib`() {
        project.propertiesExtension.set("kotlin.stdlib.default.dependency", "false")
        project.plugins.apply(LibraryPlugin::class.java)
        project.extensions.findByType(LibraryExtension::class.java)?.compileSdkVersion(30)

        project.repositories {
            mavenLocal()
            mavenCentral()
        }

        kotlin.android()

        kotlin.mainAndTest {
            common.dependencies { implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0") }
            fragments.create("linux", KotlinLinuxX64Variant::class.java)
            jvm
        }

        project.evaluate()
        val model = kotlin.buildIdeaKotlinProjectModel()

        val mainModule = model.modules.find { it.moduleIdentifier.moduleClassifier == null } ?: fail("Missing main module")
        val jvmFragment = mainModule.fragments.find { it.name == "jvm" } ?: fail("Missing jvm fragment")
        println(jvmFragment.dependencies)
    }

    @Test
    fun `test mvikotlin`() {
        project.propertiesExtension.set("kotlin.stdlib.default.dependency", "false")
        project.plugins.apply(LibraryPlugin::class.java)
        project.extensions.findByType(LibraryExtension::class.java)?.compileSdkVersion(30)

        project.repositories {
            mavenLocal()
            mavenCentral()
        }

        kotlin.android()

        kotlin.mainAndTest {
            common.dependencies { implementation("com.arkivanov.mvikotlin:mvikotlin:3.0.0-beta01") }
            jvm
            val nativeMain = fragments.create("linuxMain")
            val linuxX64 = fragments.create("linuxX64", KotlinLinuxX64Variant::class.java)
            val macosX64 = fragments.create("macosX64", KotlinMacosX64Variant::class.java)
            linuxX64.refines(nativeMain)
            macosX64.refines(nativeMain)
        }

        project.evaluate()
        val model = kotlin.buildIdeaKotlinProjectModel()
        println(model)
    }

}
