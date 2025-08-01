/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.actualizer

import org.jetbrains.kotlin.backend.common.actualizer.checker.IrExpectActualChecker
import org.jetbrains.kotlin.backend.common.actualizer.checker.IrExpectActualCheckers
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.IrTypeSystemContext
import org.jetbrains.kotlin.ir.types.classOrFail
import org.jetbrains.kotlin.ir.util.SymbolRemapper
import org.jetbrains.kotlin.ir.util.classId

data class IrActualizedResult(
    val actualizedExpectDeclarations: List<IrDeclaration>,
    val expectActualMap: IrExpectActualMap
)

/**
 * IrActualizer is responsible for performing actualization.
 *
 * The main action is the replacement of references to expect declarations with corresponding actuals on the IR level.
 *
 * See `/docs/fir/k2_kmp.md`
 */
class IrActualizer(
    val ktDiagnosticReporter: IrDiagnosticReporter,
    val typeSystemContext: IrTypeSystemContext,
    val languageVersionSettings: LanguageVersionSettings,
    expectActualTracker: ExpectActualTracker?,
    val mainFragment: IrModuleFragment,
    val dependentFragments: List<IrModuleFragment>,
    extraActualClassExtractors: List<IrExtraActualDeclarationExtractor> = emptyList(),
    private val missingActualProvider: IrMissingActualDeclarationProvider?,
    actualizerMapContributor: IrActualizerMapContributor?,
    private val hmppSchemeEnabled: Boolean,
) {
    private val collector = ExpectActualCollector(
        mainFragment,
        dependentFragments,
        typeSystemContext,
        languageVersionSettings,
        ktDiagnosticReporter,
        expectActualTracker,
        extraActualClassExtractors,
        missingActualProvider,
        actualizerMapContributor
    )

    val classActualizationInfo: ClassActualizationInfo = collector.collectClassActualizationInfo()

    fun actualizeClassifiers() {
        val classSymbolRemapper = object : SymbolRemapper.Empty() {
            override fun getReferencedClass(symbol: IrClassSymbol): IrClassSymbol {
                if (!hmppSchemeEnabled && !symbol.owner.isExpect) return symbol
                if (symbol.owner.containsOptionalExpectation()) return symbol
                val classId = symbol.owner.classId ?: return symbol
                classActualizationInfo.actualTypeAliases[classId]?.let { return it.owner.expandedType.classOrFail }
                classActualizationInfo.actualClasses[classId]?.let { return it }
                // Can't happen normally, but possible on incorrect code.
                // In that case, it would later fail with error in matching inside [actualizeCallablesAndMergeModules]
                // Let's leave to expect class as is for that case, it is probably best effort to make errors reasonable.
                return symbol
            }
        }
        dependentFragments.forEach { it.transform(ActualizerVisitor(classSymbolRemapper, membersActualization = false), null) }
    }

    fun actualizeCallablesAndMergeModules(): IrExpectActualMap {
        // 1. Collect expect-actual links for members of classes found on step 1.
        val expectActualMap = collector.matchAllExpectDeclarations(classActualizationInfo)

        // 2. Copy and actualize function parameter default values from expect functions
        val symbolRemapper = ActualizerSymbolRemapper(expectActualMap)
        FunctionDefaultParametersActualizer(symbolRemapper, expectActualMap).actualize()

        // 3. Actualize expect calls in dependent fragments using info obtained in the previous steps
        val actualizerVisitor = ActualizerVisitor(symbolRemapper, membersActualization = true)
        dependentFragments.forEach { it.transform(actualizerVisitor, data = null) }

        // 4. Actualize property accessors actualized by java fields
        if (expectActualMap.propertyAccessorsActualizedByFields.isNotEmpty()) {
            val specialFakeOverrideSymbolsActualizedByFieldsTransformer =
                SpecialFakeOverrideSymbolsActualizedByFieldsTransformer(expectActualMap.propertyAccessorsActualizedByFields)
            dependentFragments.forEach {
                it.transform(specialFakeOverrideSymbolsActualizedByFieldsTransformer, null)
            }
        }

        // In lenient mode, calls in the main fragment can resolve to expect declarations because of missing actuals.
        // Therefore, we also need to actualize expect calls there.
        if (missingActualProvider != null) {
            mainFragment.transform(actualizerVisitor, null)
        }

        // 5. Move all declarations to mainFragment
        mergeIrFragments(mainFragment, dependentFragments)
        return expectActualMap
    }

    fun runChecksAndFinalize(expectActualMap: IrExpectActualMap): IrActualizedResult {
        //   Remove top-only expect declarations since they are not needed anymore and should not be presented in the final IrFragment
        //   Also, it doesn't remove unactualized expect declarations marked with @OptionalExpectation
        val removedExpectDeclarations = removeExpectDeclarations(dependentFragments, expectActualMap)

        IrExpectActualCheckers.check(
            context = IrExpectActualChecker.Context(
                expectActualMap,
                classActualizationInfo,
                typeSystemContext,
                languageVersionSettings,
                ktDiagnosticReporter
            )
        )
        return IrActualizedResult(removedExpectDeclarations, expectActualMap)
    }

    private fun removeExpectDeclarations(
        dependentFragments: List<IrModuleFragment>,
        expectActualMap: IrExpectActualMap,
    ): List<IrDeclaration> {
        val removedExpectDeclarations = mutableListOf<IrDeclaration>()
        for (fragment in dependentFragments) {
            for (file in fragment.files) {
                file.declarations.removeIf {
                    if (shouldRemoveExpectDeclaration(it, expectActualMap)) {
                        removedExpectDeclarations.add(it)
                        true
                    } else {
                        false
                    }
                }
            }
        }
        return removedExpectDeclarations
    }

    private fun shouldRemoveExpectDeclaration(irDeclaration: IrDeclaration, expectActualMap: IrExpectActualMap): Boolean {
        return when (irDeclaration) {
            is IrClass -> irDeclaration.isExpect && (!irDeclaration.containsOptionalExpectation() || expectActualMap.symbolMap.containsKey(irDeclaration.symbol))
            is IrProperty -> irDeclaration.isExpect
            is IrFunction -> irDeclaration.isExpect
            else -> false
        }
    }

    private fun mergeIrFragments(mainFragment: IrModuleFragment, dependentFragments: List<IrModuleFragment>) {
        // Reversing `dependentFragments` results in backward top-sorted order
        // It is crucial for aligning files order when serializing to klib
        val newFiles = dependentFragments.reversed().flatMap { it.files }
        for (file in newFiles) file.module = mainFragment
        mainFragment.files.addAll(newFiles)
    }
}

