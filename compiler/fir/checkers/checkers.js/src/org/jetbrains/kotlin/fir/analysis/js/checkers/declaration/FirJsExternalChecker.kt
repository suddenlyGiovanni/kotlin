/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.declaration

import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyBackingField
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.analysis.js.checkers.isNativeObject
import org.jetbrains.kotlin.fir.analysis.web.common.checkers.declaration.FirWebCommonExternalChecker
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.JsStandardClassIds
import org.jetbrains.kotlin.name.JsStandardClassIds.Annotations.JsNative

object FirJsExternalChecker : FirWebCommonExternalChecker(allowCompanionInInterface = true) {
    override fun isNativeOrEffectivelyExternal(symbol: FirBasedSymbol<*>, session: FirSession): Boolean {
        return symbol.isNativeObject(session)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun reportExternalEnum(declaration: FirDeclaration, ) {
        reporter.reportOn(declaration.source, FirJsErrors.ENUM_CLASS_IN_EXTERNAL_DECLARATION_WARNING)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun additionalCheck(declaration: FirDeclaration, ) {
        if (declaration is FirFunction && declaration.isInline) {
            reporter.reportOn(declaration.source, FirWebCommonErrors.INLINE_EXTERNAL_DECLARATION)
        }

        fun reportOnParametersAndReturnTypesIf(
            diagnosticFactory: KtDiagnosticFactory0,
            condition: (ConeKotlinType) -> Boolean,
        ) {
            if (
                declaration !is FirCallableDeclaration ||
                declaration is FirDefaultPropertyAccessor ||
                declaration is FirDefaultPropertyBackingField
            ) {
                return
            }

            fun checkTypeIsNotInlineClass(type: ConeKotlinType, elementToReport: KtSourceElement?) {
                if (condition(type)) {
                    reporter.reportOn(elementToReport, diagnosticFactory)
                }
            }

            if (declaration.returnTypeRef.source?.allowsReporting == true) {
                declaration.returnTypeRef.source?.let {
                    checkTypeIsNotInlineClass(declaration.returnTypeRef.coneType, it)
                }
            }

            if (declaration !is FirFunction) {
                return
            }

            for (parameter in declaration.valueParameters) {
                val ktParam = parameter.source
                if (ktParam?.allowsReporting != true) {
                    continue
                }

                val typeToCheck = parameter.varargElementType ?: parameter.returnTypeRef.coneType
                checkTypeIsNotInlineClass(typeToCheck, ktParam)
            }
        }

        val valueClassInExternalDiagnostic = when {
            LanguageFeature.JsAllowValueClassesInExternals.isEnabled() -> {
                FirJsErrors.INLINE_CLASS_IN_EXTERNAL_DECLARATION_WARNING
            }

            else -> {
                FirJsErrors.INLINE_CLASS_IN_EXTERNAL_DECLARATION
            }
        }

        reportOnParametersAndReturnTypesIf(valueClassInExternalDiagnostic) { it.isValueClass(context.session) }

        if (!LanguageFeature.JsEnableExtensionFunctionInExternals.isEnabled()) {
            reportOnParametersAndReturnTypesIf(
                FirJsErrors.EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION, ConeKotlinType::isExtensionFunctionType
            )
        }

        declaration.checkEnumEntry()
    }

    override fun isDefinedExternallyCallableId(callableId: CallableId?): Boolean {
        return callableId == JsStandardClassIds.Callables.JsDefinedExternally
    }

    override fun hasExternalLikeAnnotations(declaration: FirDeclaration, session: FirSession): Boolean {
        return declaration.hasAnnotation(JsNative, session)
    }

    private val KtSourceElement.allowsReporting
        get() = kind !is KtFakeSourceElementKind || kind == KtFakeSourceElementKind.PropertyFromParameter

    private val FirValueParameter.varargElementType
        get() = when {
            !isVararg -> null
            else -> returnTypeRef.coneType.typeArguments.firstOrNull()?.type
        }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun FirDeclaration.checkEnumEntry() {
        if (this !is FirEnumEntry) return
        initializer?.let {
            reporter.reportOn(it.source, FirJsErrors.EXTERNAL_ENUM_ENTRY_WITH_BODY)
        }
    }
}
