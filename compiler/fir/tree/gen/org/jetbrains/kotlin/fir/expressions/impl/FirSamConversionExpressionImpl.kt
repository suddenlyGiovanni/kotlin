/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.MutableOrEmptyList
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirSamConversionExpression
import org.jetbrains.kotlin.fir.expressions.UnresolvedExpressionTypeAccess
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.fir.visitors.transformInplace

@OptIn(UnresolvedExpressionTypeAccess::class)
internal class FirSamConversionExpressionImpl(
    override val source: KtSourceElement?,
    @property:UnresolvedExpressionTypeAccess
    override var coneTypeOrNull: ConeKotlinType?,
    override var annotations: MutableOrEmptyList<FirAnnotation>,
    override var expression: FirExpression,
    override val usesFunctionKindConversion: Boolean,
) : FirSamConversionExpression() {

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        expression.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirSamConversionExpressionImpl {
        transformAnnotations(transformer, data)
        expression = expression.transform(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirSamConversionExpressionImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeKotlinType?) {
        coneTypeOrNull = newConeTypeOrNull
    }

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        annotations = newAnnotations.toMutableOrEmpty()
    }
}
