/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.api.projectStructure.copyOrigin
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getResolutionFacade
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirCustomScriptDefinitionTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.ContextCollector
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.fir.declarations.FirResolvedImport
import org.jetbrains.kotlin.fir.declarations.FirTowerDataContext
import org.jetbrains.kotlin.fir.renderer.FirDeclarationRendererWithAttributes
import org.jetbrains.kotlin.fir.renderer.FirRenderer
import org.jetbrains.kotlin.fir.renderer.FirResolvePhaseRenderer
import org.jetbrains.kotlin.fir.resolve.dfa.RealVariable
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.scopes.impl.*
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.renderReadableWithFqNames
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractContextCollectorTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        performTestByMainFile(mainFile, mainModule, testServices, testPrefixes = emptyList(), useBodyElement = false)
        performTestByMainFile(mainFile, mainModule, testServices, testPrefixes = listOf("body"), useBodyElement = true)

        val fakeFile = createFileCopy(mainFile)

        performTestByMainFile(fakeFile, mainModule, testServices, testPrefixes = listOf("copy"), useBodyElement = false)
        performTestByMainFile(fakeFile, mainModule, testServices, testPrefixes = listOf("body.copy", "copy"), useBodyElement = true)
    }

    private fun createFileCopy(file: KtFile): KtFile {
        val fakeFile = file.copy() as KtFile

        assert(fakeFile.copyOrigin == file)
        assert(!fakeFile.isPhysical)
        assert(!fakeFile.viewProvider.isEventSystemEnabled)

        return fakeFile
    }

    private fun performTestByMainFile(
        mainFile: KtFile,
        mainModule: KtTestModule,
        testServices: TestServices,
        testPrefixes: List<String>,
        useBodyElement: Boolean
    ) {
        val resolutionFacade = mainModule.ktModule.getResolutionFacade(mainFile.project)
        val firFile = mainFile.getOrBuildFirFile(resolutionFacade)
        val targetElement = testServices.expressionMarkerProvider
            .getBottommostSelectedElementOfType(mainFile, KtElement::class)

        val elementContext = ContextCollector.process(resolutionFacade, firFile, targetElement, useBodyElement)
            ?: error("Context not found for element $targetElement")

        val firRenderer = FirRenderer(
            resolvePhaseRenderer = FirResolvePhaseRenderer(),
            declarationRenderer = FirDeclarationRendererWithPartialBodyResolveState()
        )

        val actualText = buildString {
            ElementContextRenderer.render(elementContext, this)
            appendLine()
            append(firRenderer.renderElementAsString(firFile, trim = true))
        }

        testServices.assertions.assertEqualsToTestOutputFile(actualText, testPrefixes = testPrefixes)
    }
}

private class FirDeclarationRendererWithPartialBodyResolveState : FirDeclarationRendererWithAttributes() {
    override fun attributeTypesToIds(): List<Pair<String, Int>> {
        return super.attributeTypesToIds().filter { it.first == "PartialBodyAnalysisStateKey" }
    }
}

internal object ElementContextRenderer {
    fun render(context: ContextCollector.Context, builder: StringBuilder) = with(builder) {
        renderTowerDataContext(context.towerDataContext)
        renderSmartCasts(context.smartCasts)
    }

    private fun StringBuilder.renderTowerDataContext(towerDataContext: FirTowerDataContext) {
        appendBlock("Tower Data Context:") {
            for ((index, towerDataElement) in towerDataContext.towerDataElements.withIndex()) {
                appendBlock("Element $index") {
                    for (scope in towerDataElement.scope?.flatten().orEmpty()) {
                        appendBlock("Scope: " + scope.javaClass.simpleName) {
                            renderScope(scope)
                        }
                    }

                    towerDataElement.implicitReceiver?.let { implicitReceiver ->
                        appendBlock("Implicit receiver:") {
                            appendSymbol(implicitReceiver.boundSymbol as FirBasedSymbol<*>).appendLine()

                            appendBlock {
                                append("Type: ").appendType(implicitReceiver.type).appendLine()
                            }
                        }
                    }

                    towerDataElement.contextReceiverGroup?.takeIf { it.isNotEmpty() }?.let { contextReceiverValues ->
                        appendBlock("Context receivers:") {
                            for (contextReceiverValue in contextReceiverValues) {
                                appendSymbol(contextReceiverValue.boundSymbol).appendLine()

                                appendBlock {
                                    append("Type: ").appendType(contextReceiverValue.type).appendLine()
                                    contextReceiverValue.labelName?.let { labelName ->
                                        append("Label: ").appendLine(labelName)
                                    }
                                }
                            }
                        }
                    }

                    towerDataElement.contextParameterGroup?.takeIf { it.isNotEmpty() }?.let { contextParameterValues ->
                        appendBlock("Context parameters:") {
                            for (contextParameterValue in contextParameterValues) {
                                appendSymbol(contextParameterValue.boundSymbol).appendLine()
                                appendBlock {
                                    append("Type: ").appendType(contextParameterValue.type).appendLine()
                                }
                            }
                        }
                    }

                    towerDataElement.staticScopeOwnerSymbol?.let { staticScopeOwnerSymbol ->
                        append("Static scope owner symbol: ").appendSymbol(staticScopeOwnerSymbol).appendLine()
                    }
                }
            }
        }
    }

    private fun StringBuilder.renderScope(scope: FirScope) {
        when (scope) {
            is FirDefaultSimpleImportingScope, is FirDefaultStarImportingScope -> {
                Unit
                // Skip to avoid fixing default imports in an unrelated test
            }
            is FirPackageMemberScope -> {
                // Skip as the scope can be huge
            }
            is FirAbstractSimpleImportingScope -> {
                for (import in scope.simpleImports.flatMap { it.value }) {
                    renderImport(import)
                }
            }
            is FirAbstractStarImportingScope -> {
                for (import in scope.starImports) {
                    renderImport(import)
                }
            }
            is FirContainingNamesAwareScope -> {
                val classifierNames = scope.getClassifierNames().sorted()
                val callableNames = scope.getCallableNames().sorted()

                fun <T : FirBasedSymbol<*>> appendDeclarations(title: String, names: List<Name>, collector: (Name, (T) -> Unit) -> Unit) {
                    collect(names, collector).takeIf { it.isNotEmpty() }?.let { classifiers ->
                        appendBlock(title) {
                            classifiers.forEach { appendSymbol(it).appendLine() }
                        }
                    }
                }

                appendDeclarations("Classifiers:", classifierNames, scope::processClassifiersByName)
                appendDeclarations("Functions", callableNames, scope::processFunctionsByName)
                appendDeclarations("Properties:", callableNames, scope::processPropertiesByName)
            }
            else -> {
                throw IllegalArgumentException("Unexpected scope type: " + scope.javaClass.name)
            }
        }
    }

    private fun <T> collect(names: List<Name>, collector: (Name, (T) -> Unit) -> Unit): List<T> {
        return buildList {
            for (name in names) {
                collector(name) { add(it) }
            }
        }
    }

    private fun StringBuilder.renderImport(import: FirResolvedImport) {
        val name = import.importedName ?: SpecialNames.NO_NAME_PROVIDED

        appendBlock("Import name:" + name.asString()) {
            import.importedFqName?.let { importedFqName ->
                append("Qualified name: ").append(importedFqName.asString()).appendLine()
            }

            append("Is all under: ").append(import.isAllUnder).appendLine()

            import.aliasName?.let { aliasName ->
                append("Alias: ").append(aliasName.asString()).appendLine()
            }
        }
    }

    private fun StringBuilder.renderSmartCasts(smartCasts: Map<RealVariable, Set<ConeKotlinType>>) {
        if (smartCasts.isEmpty()) {
            return
        }

        appendBlock("Smart Casts:") {
            for ((realVariable, types) in smartCasts) {
                appendSymbol(realVariable.symbol).appendLine()

                appendBlock("Types:") {
                    for (type in types) {
                        appendType(type).appendLine()
                    }
                }

            }
        }
    }

    private fun StringBuilder.appendBlock(title: String? = null, block: StringBuilder.() -> Unit): StringBuilder {
        if (title != null) {
            appendLine(title)
        }

        val nestedText = StringBuilder().apply(block).toString().trim()

        if (nestedText.isNotBlank()) {
            for (line in nestedText.lineSequence()) {
                appendIndent().appendLine(line)
            }
        }

        return this
    }

    private fun StringBuilder.appendSymbol(symbol: FirBasedSymbol<*>): StringBuilder {
        val renderer = FirRenderer(
            annotationRenderer = null,
            bodyRenderer = null,
            classMemberRenderer = null,
            contractRenderer = null
        )

        val type = symbol.javaClass.simpleName
        val text = renderer.renderElementAsString(symbol.fir, trim = true)
        return append(type).append(' ').append(text)
    }

    private fun StringBuilder.appendType(type: ConeKotlinType): StringBuilder {
        return append(type.renderReadableWithFqNames())
    }

    private fun StringBuilder.appendIndent(): StringBuilder {
        append("    ")
        return this
    }
}

private fun FirScope.flatten(): List<FirScope> {
    return when (this) {
        is FirCompositeScope -> scopes.flatMap { it.flatten() }
        is FirNameAwareCompositeScope -> scopes.flatMap { it.flatten() }
        else -> listOf(this)
    }
}

abstract class AbstractContextCollectorSourceTest : AbstractContextCollectorTest() {
    override val configurator: AnalysisApiTestConfigurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)
}

abstract class AbstractContextCollectorScriptTest : AbstractContextCollectorTest() {
    override val configurator: AnalysisApiTestConfigurator =
        AnalysisApiFirCustomScriptDefinitionTestConfigurator(analyseInDependentSession = false)
}

