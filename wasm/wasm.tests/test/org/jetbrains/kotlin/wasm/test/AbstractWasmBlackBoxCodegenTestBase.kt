/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test

import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.handlers.FirInterpreterDumpHandler
import org.jetbrains.kotlin.test.backend.handlers.JsKlibInterpreterDumpHandler
import org.jetbrains.kotlin.test.backend.handlers.WasmIrInterpreterDumpHandler
import org.jetbrains.kotlin.test.builders.*
import org.jetbrains.kotlin.test.configuration.commonClassicFrontendHandlersForCodegenTest
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.DIAGNOSTICS
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.frontend.classic.handlers.ClassicDiagnosticsHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDiagnosticsHandler
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.AdditionalSourceProvider
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.LibraryProvider
import org.jetbrains.kotlin.test.services.sourceProviders.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider
import org.jetbrains.kotlin.wasm.test.converters.WasmPreSerializationLoweringFacade
import org.jetbrains.kotlin.wasm.test.handlers.WasmDtsHandler

abstract class AbstractWasmBlackBoxCodegenTestBase<R : ResultingArtifact.FrontendOutput<R>, I : ResultingArtifact.BackendInput<I>, A : ResultingArtifact.Binary<A>>(
    private val targetFrontend: FrontendKind<R>,
    targetBackend: TargetBackend,
    private val targetPlatform: TargetPlatform,
    private val pathToTestDir: String,
    private val testGroupOutputDirPrefix: String,
) : AbstractKotlinCompilerWithTargetBackendTest(targetBackend) {
    abstract val frontendFacade: Constructor<FrontendFacade<R>>
    abstract val frontendToBackendConverter: Constructor<Frontend2BackendConverter<R, I>>
    abstract val backendFacade: Constructor<BackendFacade<I, A>>
    abstract val afterBackendFacade: Constructor<AbstractTestFacade<A, BinaryArtifacts.Wasm>>
    abstract val wasmBoxTestRunner: Constructor<AnalysisHandler<BinaryArtifacts.Wasm>>
    abstract val wasmEnvironmentConfigurator: Constructor<EnvironmentConfigurator>
    open val additionalSourceProvider: Constructor<AdditionalSourceProvider>? = null

    protected fun TestConfigurationBuilder.commonConfigurationForWasmBlackBoxCodegenTest() {
        globalDefaults {
            frontend = targetFrontend
            targetPlatform = this@AbstractWasmBlackBoxCodegenTestBase.targetPlatform
            dependencyKind = DependencyKind.Binary
        }

        val pathToRootOutputDir = System.getProperty("kotlin.wasm.test.root.out.dir") ?: error("'kotlin.wasm.test.root.out.dir' is not set")
        defaultDirectives {
            +DiagnosticsDirectives.REPORT_ONLY_EXPLICITLY_DEFINED_DEBUG_INFO
            WasmEnvironmentConfigurationDirectives.PATH_TO_ROOT_OUTPUT_DIR with pathToRootOutputDir
            WasmEnvironmentConfigurationDirectives.PATH_TO_TEST_DIR with pathToTestDir
            WasmEnvironmentConfigurationDirectives.TEST_GROUP_OUTPUT_DIR_PREFIX with testGroupOutputDirPrefix
            LANGUAGE with "+JsAllowImplementingFunctionInterface"
        }

        useConfigurators(
            wasmEnvironmentConfigurator,
        )

        useAdditionalSourceProviders(
            ::WasmAdditionalSourceProvider,
            ::CoroutineHelpersSourceFilesProvider,
            ::AdditionalDiagnosticsSourceFilesProvider,
        )

        additionalSourceProvider?.let {
            useAdditionalSourceProviders(it)
        }

        useAdditionalService(::LibraryProvider)

        useAfterAnalysisCheckers(
            ::WasmFailingTestSuppressor,
            ::BlackBoxCodegenSuppressor,
        )

        facadeStep(frontendFacade)

        classicFrontendHandlersStep {
            commonClassicFrontendHandlersForCodegenTest()
            useHandlers(::ClassicDiagnosticsHandler)
        }

        firHandlersStep {
            useHandlers(::FirDiagnosticsHandler)
        }

        facadeStep(frontendToBackendConverter)
        irHandlersStep()

        facadeStep(::WasmPreSerializationLoweringFacade)
        loweredIrHandlersStep()

        facadeStep(backendFacade)
        klibArtifactsHandlersStep()
        facadeStep(afterBackendFacade)

        wasmArtifactsHandlersStep {
            useHandlers(wasmBoxTestRunner)
            useHandlers(::WasmDtsHandler)
        }
    }

    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        commonConfigurationForWasmBlackBoxCodegenTest()

        forTestsNotMatching(
            "compiler/testData/codegen/box/diagnostics/functions/tailRecursion/*" or
                    "compiler/testData/diagnostics/*"
        ) {
            defaultDirectives {
                DIAGNOSTICS with "-warnings"
            }
        }

        enableMetaInfoHandler()

        forTestsMatching("compiler/testData/codegen/box/involvesIrInterpreter/*") {
            enableMetaInfoHandler()
            configureFirHandlersStep {
                useHandlers(::FirInterpreterDumpHandler)
            }
            configureKlibArtifactsHandlersStep {
                useHandlers(::JsKlibInterpreterDumpHandler)
            }
            configureWasmArtifactsHandlersStep {
                useHandlers(::WasmIrInterpreterDumpHandler)
            }
        }

        forTestsMatching("compiler/testData/codegen/box/properties/backingField/*") {
            defaultDirectives {
                LANGUAGE with "+ExplicitBackingFields"
            }
        }
    }
}
