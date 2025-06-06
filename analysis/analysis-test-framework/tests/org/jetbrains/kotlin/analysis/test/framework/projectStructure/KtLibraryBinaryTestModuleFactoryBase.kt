/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.projectStructure

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.StandaloneProjectFactory
import org.jetbrains.kotlin.analysis.test.framework.hasFallbackDependencies
import org.jetbrains.kotlin.analysis.test.framework.isSdkLibrary
import org.jetbrains.kotlin.analysis.test.framework.services.environmentManager
import org.jetbrains.kotlin.analysis.test.framework.services.libraries.compiledLibraryProvider
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.TestModuleKind
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.targetPlatform
import java.nio.file.Path

abstract class KtLibraryBinaryTestModuleFactoryBase : KtTestModuleFactory {
    protected abstract val testModuleKind: TestModuleKind

    protected abstract fun decompileToPsiFiles(binaryRoot: Path, testServices: TestServices, project: Project): List<PsiFile>

    override fun createModule(
        testModule: TestModule,
        contextModule: KtTestModule?,
        dependencyBinaryRoots: Collection<Path>,
        testServices: TestServices,
        project: Project,
    ): KtTestModule {
        val (binaryRoots, _) = testServices.compiledLibraryProvider.compileToLibrary(testModule, dependencyBinaryRoots)
        val decompiledFiles = binaryRoots.flatMap { decompileToPsiFiles(it, testServices, project) }

        val libraryModule = KaLibraryModuleImpl(
            testModule.name,
            testModule.targetPlatform(testServices),
            StandaloneProjectFactory.createSearchScopeByLibraryRoots(
                binaryRoots,
                emptyList(),
                testServices.environmentManager.getApplicationEnvironment(),
                project,
            ),
            project,
            binaryRoots = binaryRoots,
            librarySources = null,
            isSdk = testModule.isSdkLibrary,
        )

        if (testModule.hasFallbackDependencies) {
            libraryModule.directRegularDependencies += KaLibraryFallbackDependenciesModuleImpl(libraryModule)
        }

        return KtTestModule(
            testModuleKind,
            testModule,
            libraryModule,
            decompiledFiles,
        )
    }
}
