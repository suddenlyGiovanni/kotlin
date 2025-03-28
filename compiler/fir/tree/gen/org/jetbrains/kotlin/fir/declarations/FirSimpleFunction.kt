/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.contracts.FirContractDescription
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

/**
 * Generated from: [org.jetbrains.kotlin.fir.tree.generator.FirTree.simpleFunction]
 */
abstract class FirSimpleFunction : FirFunction(), FirContractDescriptionOwner, FirTypeParametersOwner {
    abstract override val source: KtSourceElement?
    abstract override val moduleData: FirModuleData
    abstract override val origin: FirDeclarationOrigin
    abstract override val attributes: FirDeclarationAttributes
    abstract override val status: FirDeclarationStatus
    abstract override val returnTypeRef: FirTypeRef
    abstract override val receiverParameter: FirReceiverParameter?
    abstract override val deprecationsProvider: DeprecationsProvider
    abstract override val containerSource: DeserializedContainerSource?
    abstract override val dispatchReceiverType: ConeSimpleKotlinType?
    abstract override val contextParameters: List<FirValueParameter>
    abstract override val controlFlowGraphReference: FirControlFlowGraphReference?
    abstract override val valueParameters: List<FirValueParameter>
    abstract override val body: FirBlock?
    abstract override val contractDescription: FirContractDescription?
    abstract val name: Name
    abstract override val symbol: FirNamedFunctionSymbol
    abstract override val annotations: List<FirAnnotation>
    abstract override val typeParameters: List<FirTypeParameter>

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitSimpleFunction(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : FirElement, D> transform(transformer: FirTransformer<D>, data: D): E =
        transformer.transformSimpleFunction(this, data) as E

    abstract override fun replaceStatus(newStatus: FirDeclarationStatus)

    abstract override fun replaceReturnTypeRef(newReturnTypeRef: FirTypeRef)

    abstract override fun replaceReceiverParameter(newReceiverParameter: FirReceiverParameter?)

    abstract override fun replaceDeprecationsProvider(newDeprecationsProvider: DeprecationsProvider)

    abstract override fun replaceContextParameters(newContextParameters: List<FirValueParameter>)

    abstract override fun replaceControlFlowGraphReference(newControlFlowGraphReference: FirControlFlowGraphReference?)

    abstract override fun replaceValueParameters(newValueParameters: List<FirValueParameter>)

    abstract override fun replaceBody(newBody: FirBlock?)

    abstract override fun replaceContractDescription(newContractDescription: FirContractDescription?)

    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract override fun <D> transformStatus(transformer: FirTransformer<D>, data: D): FirSimpleFunction

    abstract override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirSimpleFunction

    abstract override fun <D> transformReceiverParameter(transformer: FirTransformer<D>, data: D): FirSimpleFunction

    abstract override fun <D> transformContextParameters(transformer: FirTransformer<D>, data: D): FirSimpleFunction

    abstract override fun <D> transformValueParameters(transformer: FirTransformer<D>, data: D): FirSimpleFunction

    abstract override fun <D> transformBody(transformer: FirTransformer<D>, data: D): FirSimpleFunction

    abstract override fun <D> transformContractDescription(transformer: FirTransformer<D>, data: D): FirSimpleFunction

    abstract override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirSimpleFunction

    abstract override fun <D> transformTypeParameters(transformer: FirTransformer<D>, data: D): FirSimpleFunction
}
