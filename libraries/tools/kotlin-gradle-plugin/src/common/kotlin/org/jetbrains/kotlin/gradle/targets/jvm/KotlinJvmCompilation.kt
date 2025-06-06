/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage.AfterFinaliseDsl
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCompilationImpl
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.utils.CompletableFuture
import org.jetbrains.kotlin.gradle.utils.Future
import org.jetbrains.kotlin.gradle.utils.javaSourceSets
import org.jetbrains.kotlin.gradle.utils.lenient
import org.jetbrains.kotlin.gradle.utils.named
import javax.inject.Inject

@Suppress("TYPEALIAS_EXPANSION_DEPRECATION", "TYPEALIAS_EXPANSION_DEPRECATION_ERROR", "DEPRECATION")
open class KotlinJvmCompilation @Inject internal constructor(
    compilation: KotlinCompilationImpl,
    internal val defaultJavaSourceSet: SourceSet,
) : DeprecatedAbstractKotlinCompilationToRunnableFiles<KotlinAnyOptionsDeprecated>(compilation),
    DeprecatedKotlinCompilationWithResources<KotlinAnyOptionsDeprecated> {

    final override val target: KotlinJvmTarget = compilation.target as KotlinJvmTarget

    @Suppress("DEPRECATION")
    @Deprecated(
        "To configure compilation compiler options use 'compileTaskProvider':\ncompilation.compileTaskProvider.configure{\n" +
                "    compilerOptions {}\n}"
    )
    override val compilerOptions: DeprecatedHasCompilerOptions<KotlinJvmCompilerOptions> =
        compilation.compilerOptions.castCompilerOptionsType()

    @Deprecated(
        "Replaced with compileTaskProvider. Scheduled for removal in Kotlin 2.3.",
        replaceWith = ReplaceWith("compileTaskProvider"),
        level = DeprecationLevel.ERROR
    )
    @Suppress("UNCHECKED_CAST", "DEPRECATION_ERROR")
    override val compileKotlinTaskProvider: TaskProvider<out org.jetbrains.kotlin.gradle.tasks.KotlinCompile>
        get() = compilation.compileKotlinTaskProvider as TaskProvider<out org.jetbrains.kotlin.gradle.tasks.KotlinCompile>

    @Suppress("DEPRECATION_ERROR")
    @Deprecated(
        "Accessing task instance directly is deprecated. Scheduled for removal in Kotlin 2.3.",
        replaceWith = ReplaceWith("compileTaskProvider"),
        level = DeprecationLevel.ERROR
    )
    override val compileKotlinTask: org.jetbrains.kotlin.gradle.tasks.KotlinCompile
        get() = compilation.compileKotlinTask as org.jetbrains.kotlin.gradle.tasks.KotlinCompile

    @Suppress("UNCHECKED_CAST")
    override val compileTaskProvider: TaskProvider<out KotlinCompilationTask<KotlinJvmCompilerOptions>>
        get() = compilation.compileTaskProvider as TaskProvider<KotlinCompilationTask<KotlinJvmCompilerOptions>>

    /**
     * **Note**: requesting this too early (right after target creation and before any target configuration) may falsely return `null`
     * value, but later target will be configured to run with Java enabled. If possible, please use [compileJavaTaskProviderSafe].
     */
    val compileJavaTaskProvider: TaskProvider<out JavaCompile>?
        get() {
            val project = target.project
            val javaSourceSet = if (target.withJavaEnabled) {
                val javaSourceSets = project.javaSourceSets
                javaSourceSets.getByName(compilationName)
            } else defaultJavaSourceSet
            return project.tasks.named<JavaCompile>(javaSourceSet.compileJavaTaskName)
        }

    /**
     * Alternative to [compileJavaTaskProvider] to safely receive [JavaCompile] task provider  when [KotlinJvmTarget.withJavaEnabled]
     * will be enabled after call to this method.
     */
    internal val compileJavaTaskProviderSafe: Provider<JavaCompile> = target.project.providers
        .provider { javaSourceSet.lenient.getOrNull() }
        .flatMap { javaSourceSet ->
            checkNotNull(javaSourceSet)
            project.tasks.named<JavaCompile>(javaSourceSet.compileJavaTaskName)
        }

    internal val defaultCompileJavaProvider: TaskProvider<out JavaCompile>
        get() = project.tasks.named<JavaCompile>(defaultJavaSourceSet.compileJavaTaskName)

    internal val javaSourceSet: Future<SourceSet?> get() = javaSourceSetImpl
    private val javaSourceSetImpl: CompletableFuture<SourceSet> = CompletableFuture<SourceSet>().also { future ->
        /**
         * If no SourceSet was set until 'AfterFinaliseDsl', then user really did never call into 'withJava'.
         * Hence, we can complete the Future with Java SourceSet created by default.
         */
        target.project.launchInStage(AfterFinaliseDsl) {
            if (!future.isCompleted) {
                future.complete(defaultJavaSourceSet)
            }
        }
    }

    @Deprecated("Conditionally creating Java source sets is deprecated. Check 'defaultJavaSourceSet' instead. Scheduled for removal in Kotlin 2.4.")
    internal fun maybeCreateJavaSourceSet(): SourceSet {
        check(target.withJavaEnabled)
        val sourceSet = target.project.javaSourceSets.maybeCreate(compilationName)
        javaSourceSetImpl.complete(sourceSet)
        return sourceSet
    }

    override val processResourcesTaskName: String
        get() = compilation.processResourcesTaskName ?: error("Missing 'processResourcesTaskName'")
}
