/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.gradle.api.initialization.resolve.RepositoriesMode
import org.intellij.lang.annotations.Language
import java.nio.file.Path
import kotlin.io.path.absolutePathString

@Language("Groovy")
internal val DEFAULT_GROOVY_SETTINGS_FILE =
    """
    pluginManagement {
        repositories {
            mavenLocal()
            mavenCentral()
            google()
            gradlePluginPortal()
        }

        plugins {
            id "org.jetbrains.kotlin.jvm" version "${'$'}kotlin_version"
            id "org.jetbrains.kotlin.kapt" version "${'$'}kotlin_version"
            id "org.jetbrains.kotlin.android" version "${'$'}kotlin_version"
            id "org.jetbrains.kotlin.js" version "${'$'}kotlin_version"
            id "org.jetbrains.kotlin.native.cocoapods" version "${'$'}kotlin_version"
            id "org.jetbrains.kotlin.multiplatform" version "${'$'}kotlin_version"
            id "org.jetbrains.kotlin.plugin.allopen" version "${'$'}kotlin_version"
            id "org.jetbrains.kotlin.plugin.spring" version "${'$'}kotlin_version"
            id "org.jetbrains.kotlin.plugin.jpa" version "${'$'}kotlin_version"
            id "org.jetbrains.kotlin.plugin.noarg" version "${'$'}kotlin_version"
            id "org.jetbrains.kotlin.plugin.lombok" version "${'$'}kotlin_version"
            id "org.jetbrains.kotlin.plugin.power-assert" version "${'$'}kotlin_version"
            id "org.jetbrains.kotlin.plugin.sam.with.receiver" version "${'$'}kotlin_version"
            id "org.jetbrains.kotlin.plugin.serialization" version "${'$'}kotlin_version"
            id "org.jetbrains.kotlin.plugin.assignment" version "${'$'}kotlin_version"
            id "org.jetbrains.kotlin.test.fixes.android" version "${'$'}test_fixes_version"
            id "org.jetbrains.kotlin.gradle-subplugin-example" version "${'$'}kotlin_version"
            id "org.jetbrains.kotlin.plugin.atomicfu" version "${'$'}kotlin_version"
            id "org.jetbrains.kotlin.plugin.compose" version "${'$'}kotlin_version"
            
            id "org.jetbrains.kotlin.test.gradle-warnings-detector" version "${'$'}test_fixes_version"
            id "org.jetbrains.kotlin.test.kotlin-compiler-args-properties" version "${'$'}test_fixes_version"
            
            if (System.properties["android_tools_version"]) {
                def android_tools_version = System.properties["android_tools_version"]
                id("com.android.application") version android_tools_version
                id("com.android.library") version android_tools_version
                id("com.android.test") version android_tools_version
                id("com.android.dynamic-feature") version android_tools_version
                id("com.android.asset-pack") version android_tools_version
                id("com.android.asset-pack-bundle") version android_tools_version
                id("com.android.lint") version android_tools_version
                id("com.android.instantapp") version android_tools_version
                id("com.android.feature") version android_tools_version
                id("com.android.kotlin.multiplatform.library") version android_tools_version
            }
        }
    }

    plugins {
        id("org.jetbrains.kotlin.test.gradle-warnings-detector")
    }
    """.trimIndent()


internal fun getGroovyDependencyManagementBlock(
    gradleRepositoriesMode: RepositoriesMode,
    additionalDependencyRepositories: Set<String>,
    localRepo: Path? = null,
): String =
    //language=Groovy
    """    
    |dependencyResolutionManagement {
    |    ${getGroovyRepositoryBlock(additionalDependencyRepositories, localRepo)}
    |    repositoriesMode.set(${mapRepositoryModeToString(gradleRepositoriesMode)})
    |}
    """.trimMargin()

internal fun getGroovyRepositoryBlock(
    additionalDependencyRepositories: Set<String>,
    localRepo: Path? = null,
): String =
    //language=Groovy
    """
    |
    |    repositories {
    |        mavenLocal()
    |        mavenCentral()
    |        google()
    |        ivy {
    |            url = "https://download.jetbrains.com/kotlin/native/builds/releases"
    |            patternLayout {
    |                artifact("[revision]/[classifier]/[artifact]-[classifier]-[revision].[ext]")
    |            }
    |            metadataSources {
    |                artifact()
    |            }
    |        }
    |        ivy {
    |            url = "https://download.jetbrains.com/kotlin/native/builds/dev"
    |            patternLayout {
    |                artifact("[revision]/[classifier]/[artifact]-[classifier]-[revision].[ext]")
    |            }
    |            metadataSources {
    |                artifact()
    |            }
    |        }
    |        ivy {
    |            url = "https://github.com/yarnpkg/yarn/releases/download"
    |            patternLayout {
    |                artifact("v[revision]/[artifact](-v[revision]).[ext]")
    |            }
    |            metadataSources { 
    |                artifact() 
    |            }
    |            content { 
    |                includeModule("com.yarnpkg", "yarn") 
    |            }
    |        }
    |        ivy {
    |            url = "https://nodejs.org/dist"
    |            patternLayout {
    |                artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]")
    |            }
    |            metadataSources { 
    |                artifact() 
    |            }
    |            content { 
    |                includeModule("org.nodejs", "node") 
    |            }
    |        }
    |        ivy {
    |            url = uri("https://github.com/WebAssembly/binaryen/releases/download")
    |            patternLayout {
    |                artifact("version_[revision]/binaryen-version_[revision]-[classifier].[ext]")
    |            }
    |            metadataSources { 
    |                artifact() 
    |            }
    |            content { 
    |                includeModule("com.github.webassembly", "binaryen") 
    |            }
    |        }
    |        ivy {
    |            url = uri("https://storage.googleapis.com/chromium-v8/official/canary")
    |            patternLayout {
    |                artifact("[artifact]-[revision].[ext]")
    |            }
    |            metadataSources { 
    |                artifact() 
    |            }
    |            content { 
    |                includeModule("google.d8", "v8") 
    |            }
    |        }
    |        maven {
    |            url = "https://packages.jetbrains.team/maven/p/ij/intellij-dependencies/"
    |        }
    |        ${additionalDependencyRepositories.map { repo -> "maven{ url = \"$repo\" }" }.joinToString("\n")}
    |        ${localRepo?.absolutePathString()?.let { repo -> "maven{ url = \"${repo.replace("\\", "\\\\")}\" }" } ?: ""}
    |    }
    """.trimMargin()

@Language("kts")
internal val DEFAULT_KOTLIN_SETTINGS_FILE =
    """
    pluginManagement {
        repositories {
            mavenLocal()
            mavenCentral()
            google()
            gradlePluginPortal()
        }

        val kotlin_version: String by settings
        val test_fixes_version: String by settings
        plugins {
            id("org.jetbrains.kotlin.jvm") version kotlin_version
            id("org.jetbrains.kotlin.kapt") version kotlin_version
            id("org.jetbrains.kotlin.android") version kotlin_version
            id("org.jetbrains.kotlin.js") version kotlin_version
            id("org.jetbrains.kotlin.native.cocoapods") version kotlin_version
            id("org.jetbrains.kotlin.multiplatform") version kotlin_version
            id("org.jetbrains.kotlin.plugin.allopen") version kotlin_version
            id("org.jetbrains.kotlin.plugin.spring") version kotlin_version
            id("org.jetbrains.kotlin.plugin.jpa") version kotlin_version
            id("org.jetbrains.kotlin.plugin.noarg") version kotlin_version
            id("org.jetbrains.kotlin.plugin.lombok") version kotlin_version
            id("org.jetbrains.kotlin.plugin.power-assert") version kotlin_version
            id("org.jetbrains.kotlin.plugin.sam.with.receiver") version kotlin_version
            id("org.jetbrains.kotlin.plugin.serialization") version kotlin_version
            id("org.jetbrains.kotlin.plugin.assignment") version kotlin_version
            id("org.jetbrains.kotlin.test.fixes.android") version test_fixes_version
            id("org.jetbrains.kotlin.gradle-subplugin-example") version kotlin_version
            id("org.jetbrains.kotlin.plugin.atomicfu") version kotlin_version
            id("org.jetbrains.kotlin.plugin.compose") version kotlin_version

            id("org.jetbrains.kotlin.test.gradle-warnings-detector") version test_fixes_version
            id("org.jetbrains.kotlin.test.kotlin-compiler-args-properties") version test_fixes_version

            id("org.gradle.kotlin.kotlin-dsl") version org.gradle.kotlin.dsl.support.expectedKotlinDslPluginsVersion
            
            val android_tools_version = System.getProperty("android_tools_version")
            if (android_tools_version != null) {
                id("com.android.application") version android_tools_version
                id("com.android.library") version android_tools_version
                id("com.android.test") version android_tools_version
                id("com.android.dynamic-feature") version android_tools_version
                id("com.android.asset-pack") version android_tools_version
                id("com.android.asset-pack-bundle") version android_tools_version
                id("com.android.lint") version android_tools_version
                id("com.android.instantapp") version android_tools_version
                id("com.android.feature") version android_tools_version
                id("com.android.kotlin.multiplatform.library") version android_tools_version
            }
        }
    }

    plugins {
        id("org.jetbrains.kotlin.test.gradle-warnings-detector")
    }
    """.trimIndent()

internal fun getKotlinDependencyManagementBlock(
    gradleRepositoriesMode: RepositoriesMode,
    additionalDependencyRepositories: Set<String>,
    localRepo: Path? = null,
): String =
    //language=kotlin
    """    
    |dependencyResolutionManagement {
    |    ${getKotlinRepositoryBlock(additionalDependencyRepositories, localRepo)}
    |    repositoriesMode.set(${mapRepositoryModeToString(gradleRepositoriesMode)})
    |}
    """.trimMargin()

internal fun getKotlinRepositoryBlock(
    additionalDependencyRepositories: Set<String>,
    localRepo: Path? = null,
): String =
    //language=kotlin
    """
    |
    |    repositories {
    |        mavenLocal()
    |        mavenCentral()
    |        google()
    |        ivy {
    |            url = uri("https://download.jetbrains.com/kotlin/native/builds/releases")
    |            patternLayout {
    |                artifact("[revision]/[classifier]/[artifact]-[classifier]-[revision].[ext]")
    |            }
    |            metadataSources {
    |                artifact()
    |            }
    |        }
    |        ivy {
    |            url = uri("https://download.jetbrains.com/kotlin/native/builds/dev")
    |            patternLayout {
    |                artifact("[revision]/[classifier]/[artifact]-[classifier]-[revision].[ext]")
    |            }
    |            metadataSources {
    |                artifact()
    |            }
    |        }
    |        ivy {
    |            url = uri("https://github.com/yarnpkg/yarn/releases/download")
    |            patternLayout {
    |                artifact("v[revision]/[artifact](-v[revision]).[ext]")
    |            }
    |            metadataSources { 
    |                artifact() 
    |            }
    |            content { 
    |                includeModule("com.yarnpkg", "yarn") 
    |            }
    |        }
    |        ivy {
    |            url = uri("https://nodejs.org/dist")
    |            patternLayout {
    |                artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]")
    |            }
    |            metadataSources { 
    |                artifact() 
    |            }
    |            content { 
    |                includeModule("org.nodejs", "node") 
    |            }
    |        }
    |        ivy {
    |            url = uri("https://github.com/WebAssembly/binaryen/releases/download")
    |            patternLayout {
    |                artifact("version_[revision]/binaryen-version_[revision]-[classifier].[ext]")
    |            }
    |            metadataSources { 
    |                artifact() 
    |            }
    |            content { 
    |                includeModule("com.github.webassembly", "binaryen") 
    |            }
    |        }
    |        ivy {
    |            url = uri("https://storage.googleapis.com/chromium-v8/official/canary")
    |            patternLayout {
    |                artifact("[artifact]-[revision].[ext]")
    |            }
    |            metadataSources { 
    |                artifact() 
    |            }
    |            content { 
    |                includeModule("google.d8", "v8") 
    |            }
    |        }
    |        maven {
    |            url = uri("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies/")
    |        }
    |        ${additionalDependencyRepositories.map { repo -> "maven{ url = uri(\"$repo\") }" }.joinToString("\n")}
    |        ${localRepo?.absolutePathString()?.let { repo -> "maven{ url = uri(\"${repo.replace("\\", "\\\\")}\") }" } ?: ""}
    |    }
    """.trimMargin()

private fun mapRepositoryModeToString(gradleRepositoriesMode: RepositoriesMode): String {
    return when (gradleRepositoriesMode) {
        RepositoriesMode.PREFER_PROJECT -> "RepositoriesMode.PREFER_PROJECT"
        RepositoriesMode.PREFER_SETTINGS -> "RepositoriesMode.PREFER_SETTINGS"
        RepositoriesMode.FAIL_ON_PROJECT_REPOS -> "RepositoriesMode.FAIL_ON_PROJECT_REPOS"
    }
}
