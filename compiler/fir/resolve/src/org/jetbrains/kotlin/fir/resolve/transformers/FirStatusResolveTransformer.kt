/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.componentFunctionSymbol
import org.jetbrains.kotlin.fir.declarations.utils.isInlineOrValue
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.LocalClassesNavigationInfo
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.util.PrivateForInline
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

class FirStatusResolveProcessor(
    session: FirSession,
    scopeSession: ScopeSession
) : FirTransformerBasedResolveProcessor(session, scopeSession, FirResolvePhase.STATUS) {
    override val transformer: FirStatusResolveTransformer = run {
        val statusComputationSession = StatusComputationSession(session, scopeSession)
        FirStatusResolveTransformer(statusComputationSession)
    }
}

fun <F : FirClassLikeDeclaration> F.runStatusResolveForLocalClass(
    session: FirSession,
    scopeSession: ScopeSession,
    localClassesNavigationInfo: LocalClassesNavigationInfo,
): F {
    @OptIn(FirImplementationDetail::class)
    val statusComputationSession = session.jumpingPhaseComputationSessionForLocalClassesProvider.statusPhaseSession(
        session,
        scopeSession,
        localClassesNavigationInfo.parentForClass,
    )

    val transformer = FirStatusResolveTransformer(statusComputationSession)
    return this.transform(transformer, null)
}

open class FirStatusResolveTransformer(
    statusComputationSession: StatusComputationSession,
) : AbstractFirStatusResolveTransformer(statusComputationSession) {
    override fun FirDeclaration.needResolveMembers(): Boolean {
        if (this is FirRegularClass) {
            return statusComputationSession[this] != StatusComputationSession.StatusComputationStatus.Computed
        }
        return true
    }

    override fun FirDeclaration.needResolveNestedClassifiers(): Boolean {
        return true
    }

    override fun transformClassContent(
        firClass: FirClass,
        data: FirResolvedDeclarationStatus?
    ): FirStatement {
        val computationStatus = statusComputationSession.startComputing(firClass)
        statusComputationSession.forceResolveStatusesOfSupertypes(firClass)
        /*
         * Status of class may be already calculated if that class was in supertypes of one of the previous classes
         */
        if (computationStatus != StatusComputationSession.StatusComputationStatus.Computed) {
            transformClassStatus(firClass)
            transformValueClassRepresentation(firClass)
        }

        return transformClass(firClass, data).also {
            statusComputationSession.endComputing(firClass)
        }
    }
}

open class FirDesignatedStatusResolveTransformer(
    private val designation: DesignationState,
    statusComputationSession: StatusComputationSession,
) : AbstractFirStatusResolveTransformer(statusComputationSession) {
    override fun FirDeclaration.needResolveMembers(): Boolean {
        return designation.classLocated
    }

    override fun FirDeclaration.needResolveNestedClassifiers(): Boolean {
        return !designation.classLocated
    }

    override fun transformClassContent(
        firClass: FirClass,
        data: FirResolvedDeclarationStatus?
    ): FirStatement = whileAnalysing(session, firClass) {
        if (designation.shouldSkipClass(firClass)) return firClass
        firClass.symbol.lazyResolveToPhase(FirResolvePhase.TYPES)
        val classLocated = designation.classLocated
        /*
         * In designated status resolve we should resolve status only of target class and it's members
         */
        if (classLocated) {
            assert(firClass == designation.targetClass)
            val computationStatus = statusComputationSession.startComputing(firClass)
            statusComputationSession.forceResolveStatusesOfSupertypes(firClass)
            if (computationStatus != StatusComputationSession.StatusComputationStatus.Computed) {
                firClass.transformStatus(this, statusResolver.resolveStatus(firClass, containingClass, isLocal = false))
            }
        } else {
            if (firClass.status !is FirResolvedDeclarationStatus) {
                firClass.transformStatus(this, statusResolver.resolveStatus(firClass, containingClass, isLocal = false))
                statusComputationSession.computeOnlyClassStatus(firClass)
            }
        }
        return transformClass(firClass, data).also {
            if (classLocated) statusComputationSession.endComputing(firClass)
        }
    }
}

open class StatusComputationSession(
    val useSiteSession: FirSession,
    val useSiteScopeSession: ScopeSession,
    val designationMapForLocalClasses: Map<FirClassLikeDeclaration, FirClassLikeDeclaration?> = emptyMap(),
) {
    private val statusMap = hashMapOf<FirClass, StatusComputationStatus>()
        .withDefault { StatusComputationStatus.NotComputed }

    operator fun get(klass: FirClass): StatusComputationStatus = statusMap.getValue(klass)

    fun startComputing(klass: FirClass): StatusComputationStatus {
        return statusMap.getOrPut(klass) { StatusComputationStatus.Computing }
    }

    fun endComputing(klass: FirClass) {
        statusMap[klass] = StatusComputationStatus.Computed
    }

    fun computeOnlyClassStatus(klass: FirClass) {
        val existedStatus = statusMap.getValue(klass)
        if (existedStatus < StatusComputationStatus.ComputedOnlyClassStatus) {
            statusMap[klass] = StatusComputationStatus.ComputedOnlyClassStatus
        }
    }

    enum class StatusComputationStatus(val requiresComputation: Boolean) {
        NotComputed(true),
        Computing(false),
        ComputedOnlyClassStatus(true),
        Computed(false)
    }

    open fun forceResolveStatusesOfSupertypes(regularClass: FirClass) {
        for (superTypeRef in regularClass.superTypeRefs + additionalSuperTypes(regularClass)) {
            for (classifierSymbol in superTypeToSymbols(superTypeRef)) {
                forceResolveStatusOfCorrespondingClass(classifierSymbol)
            }
        }
    }

    protected open fun additionalSuperTypes(regularClass: FirClass): List<FirTypeRef> = emptyList()

    /**
     * @return symbols which should be resolved to [FirResolvePhase.STATUS] phase
     */
    protected open fun superTypeToSymbols(typeRef: FirTypeRef): Collection<FirClassifierSymbol<*>> {
        return listOfNotNull(typeRef.coneType.toSymbol(useSiteSession))
    }

    private fun forceResolveStatusOfCorrespondingClass(superClassSymbol: FirClassifierSymbol<*>) {
        when (superClassSymbol) {
            is FirRegularClassSymbol -> forceResolveStatusesOfClass(superClassSymbol.fir)
            is FirTypeAliasSymbol -> {
                for (classifierSymbol in superTypeToSymbols(superClassSymbol.fir.expandedTypeRef)) {
                    forceResolveStatusOfCorrespondingClass(classifierSymbol)
                }
            }
            is FirTypeParameterSymbol, is FirAnonymousObjectSymbol -> {}
        }
    }

    private fun forceResolveStatusesOfClass(regularClass: FirRegularClass) {
        if (regularClass.origin != FirDeclarationOrigin.Source) {
            /*
             * If regular class has no corresponding file then it is platform or binary class,
             *   so we need to resolve supertypes of this class because they could
             *   come from kotlin sources (e.g. for java classes or cases of classpath substitution)
             */
            val statusComputationStatus = this[regularClass]
            if (!statusComputationStatus.requiresComputation) return

            this.startComputing(regularClass)
            forceResolveStatusesOfSupertypes(regularClass)
            this.endComputing(regularClass)

            return
        }

        val statusComputationStatus = this[regularClass]
        if (!statusComputationStatus.requiresComputation) return
        if (!resolveClassForSuperType(regularClass)) return
        this.endComputing(regularClass)
    }

    protected open fun resolveClassForSuperType(regularClass: FirRegularClass): Boolean {
        val designation = DesignationState.create(regularClass.symbol, designationMapForLocalClasses, includeFile = false) ?: return false

        val transformer = FirDesignatedStatusResolveTransformer(
            designation,
            this
        )

        designation.firstDeclaration.transformSingle(transformer, null)
        return true
    }
}

abstract class AbstractFirStatusResolveTransformer(
    val statusComputationSession: StatusComputationSession
) : FirAbstractTreeTransformer<FirResolvedDeclarationStatus?>(phase = FirResolvePhase.STATUS) {
    override val session: FirSession get() = statusComputationSession.useSiteSession

    @PrivateForInline
    val classes: MutableList<FirClass> = mutableListOf()
    val statusResolver: FirStatusResolver = FirStatusResolver(session, statusComputationSession.useSiteScopeSession)

    @OptIn(PrivateForInline::class)
    val containingClass: FirClass? get() = classes.lastOrNull()

    protected abstract fun FirDeclaration.needResolveMembers(): Boolean
    protected abstract fun FirDeclaration.needResolveNestedClassifiers(): Boolean

    override fun transformFile(file: FirFile, data: FirResolvedDeclarationStatus?): FirFile {
        withFileAnalysisExceptionWrapping(file) {
            transformDeclarationContent(file, data)
        }
        return file
    }

    override fun transformDeclarationStatus(
        declarationStatus: FirDeclarationStatus,
        data: FirResolvedDeclarationStatus?
    ): FirDeclarationStatus {
        return (data ?: declarationStatus)
    }

    @OptIn(PrivateForInline::class)
    inline fun storeClass(
        klass: FirClass,
        computeResult: () -> FirDeclaration
    ): FirDeclaration {
        classes += klass
        val result = computeResult()
        classes.removeAt(classes.lastIndex)
        return result
    }

    override fun transformDeclaration(
        declaration: FirDeclaration,
        data: FirResolvedDeclarationStatus?
    ): FirDeclaration = whileAnalysing(session, declaration) {
        return when (declaration) {
            is FirCallableDeclaration -> {
                if (declaration is FirFunction) {
                    for (valueParameter in declaration.valueParameters) {
                        transformValueParameter(valueParameter, data)
                    }
                }
                declaration
            }
            else -> {
                transformElement(declaration, data)
            }
        }
    }

    override fun transformDanglingModifierList(
        danglingModifierList: FirDanglingModifierList,
        data: FirResolvedDeclarationStatus?,
    ): FirDanglingModifierList = danglingModifierList

    override fun transformTypeAlias(
        typeAlias: FirTypeAlias,
        data: FirResolvedDeclarationStatus?
    ): FirStatement = whileAnalysing(session, typeAlias) {
        typeAlias.typeParameters.forEach { it.transformSingle(this, data) }
        typeAlias.transformStatus(this, statusResolver.resolveStatus(typeAlias, containingClass, isLocal = false))
        return transformDeclaration(typeAlias, data) as FirTypeAlias
    }

    override fun transformRegularClass(
        regularClass: FirRegularClass,
        data: FirResolvedDeclarationStatus?
    ): FirStatement = whileAnalysing(session, regularClass) {
        transformClassContent(regularClass, data)
    }

    abstract fun transformClassContent(
        firClass: FirClass,
        data: FirResolvedDeclarationStatus?
    ): FirStatement

    override fun transformAnonymousObject(
        anonymousObject: FirAnonymousObject,
        data: FirResolvedDeclarationStatus?
    ): FirStatement = whileAnalysing(session, anonymousObject) {
        transformClassContent(anonymousObject, data)
    }

    open fun transformDeclarationContent(
        declaration: FirDeclaration,
        data: FirResolvedDeclarationStatus?
    ): FirDeclaration {

        val declarations = when (declaration) {
            is FirRegularClass -> declaration.declarations
            is FirAnonymousObject -> declaration.declarations
            is FirFile -> declaration.declarations
            else -> errorWithAttachment("Unsupported declaration: ${declaration::class.java}") {
                withFirEntry("declaration", declaration)
            }
        }

        if (declaration.needResolveMembers()) {
            declarations.forEach {
                if (it !is FirClassLikeDeclaration) {
                    it.transformSingle(this, data)
                }
            }
        }
        if (declaration.needResolveNestedClassifiers()) {
            declarations.forEach {
                if (it is FirClassLikeDeclaration) {
                    it.transformSingle(this, data)
                }
            }
        }
        return declaration
    }

    override fun transformClass(
        klass: FirClass,
        data: FirResolvedDeclarationStatus?
    ): FirStatement = whileAnalysing(session, klass) {
        return storeClass(klass) {
            klass.typeParameters.forEach { it.transformSingle(this, data) }
            transformDeclarationContent(klass, data)
        } as FirStatement
    }

    fun transformValueClassRepresentation(firClass: FirClass) {
        if (firClass is FirRegularClass && firClass.isInlineOrValue) {
            firClass.valueClassRepresentation = computeValueClassRepresentation(firClass, session)
        }
    }

    fun transformClassStatus(firClass: FirClass) {
        firClass.transformStatus(this, statusResolver.resolveStatus(firClass, containingClass, isLocal = false))
    }

    override fun transformReplSnippet(
        replSnippet: FirReplSnippet,
        data: FirResolvedDeclarationStatus?,
    ): FirReplSnippet {
        // Processing snippet declarations as local ones
        return replSnippet
    }

    private fun transformPropertyAccessor(
        propertyAccessor: FirPropertyAccessor,
        containingProperty: FirProperty,
        overriddenStatuses: List<FirResolvedDeclarationStatus> = emptyList(),
    ): Unit = whileAnalysing(session, propertyAccessor) {
        propertyAccessor.transformStatus(
            this,
            statusResolver.resolveStatus(
                propertyAccessor,
                containingClass,
                containingProperty,
                isLocal = false,
                overriddenStatuses,
            )
        )

        propertyAccessor.transformValueParameters(this, null)
    }

    override fun transformConstructor(
        constructor: FirConstructor,
        data: FirResolvedDeclarationStatus?
    ): FirStatement = whileAnalysing(session, constructor) {
        constructor.transformStatus(this, statusResolver.resolveStatus(constructor, containingClass, isLocal = false))
        return transformDeclaration(constructor, data) as FirStatement
    }

    override fun transformErrorPrimaryConstructor(
        errorPrimaryConstructor: FirErrorPrimaryConstructor,
        data: FirResolvedDeclarationStatus?,
    ): FirStatement = transformConstructor(errorPrimaryConstructor, data)

    override fun transformSimpleFunction(
        simpleFunction: FirSimpleFunction,
        data: FirResolvedDeclarationStatus?,
    ): FirStatement = whileAnalysing(session, simpleFunction) {
        val overriddenFunctions = statusResolver.getOverriddenFunctions(simpleFunction, containingClass)
        transformSimpleFunction(simpleFunction, overriddenFunctions, data)
        return simpleFunction
    }

    fun transformSimpleFunction(
        simpleFunction: FirSimpleFunction,
        overriddenFunctions: List<FirSimpleFunction>,
        data: FirResolvedDeclarationStatus? = null,
    ) {
        val resolvedStatus = statusResolver.resolveStatus(
            simpleFunction,
            containingClass,
            isLocal = false,
            overriddenFunctions.map { it.status as FirResolvedDeclarationStatus },
        )

        simpleFunction.transformStatus(this, resolvedStatus)
        transformDeclaration(simpleFunction, data) as FirStatement
    }

    override fun transformProperty(
        property: FirProperty,
        data: FirResolvedDeclarationStatus?
    ): FirStatement = whileAnalysing(session, property) {
        val overridden = statusResolver.getOverriddenProperties(property, containingClass)
        transformProperty(property, overridden)
        return property
    }

    fun transformProperty(property: FirProperty, overriddenProperties: List<FirProperty>) {
        val overriddenStatuses = overriddenProperties.map { it.status as FirResolvedDeclarationStatus }
        val overriddenSetters = overriddenProperties.mapNotNull {
            val setter = it.setter ?: return@mapNotNull null
            setter.status as FirResolvedDeclarationStatus
        }

        property.transformStatus(
            this,
            statusResolver.resolveStatus(property, containingClass, false, overriddenStatuses)
        )

        property.getter?.let { transformPropertyAccessor(it, property) }
        property.setter?.let { transformPropertyAccessor(it, property, overriddenSetters) }

        property.backingField?.let {
            it.transformStatus(
                this,
                statusResolver.resolveStatus(it, containingClass, property, isLocal = false)
            )
        }

        property.componentFunctionSymbol?.let { componentFunction ->
            if (componentFunction.fir.status.visibility == Visibilities.Unknown) {
                componentFunction.fir.replaceStatus(componentFunction.fir.status.copy(visibility = property.visibility))
            }
        }
    }

    override fun transformField(
        field: FirField,
        data: FirResolvedDeclarationStatus?
    ): FirStatement = whileAnalysing(session, field) {
        field.transformStatus(this, statusResolver.resolveStatus(field, containingClass, isLocal = false))
        return transformDeclaration(field, data) as FirField
    }

    override fun transformPropertyAccessor(propertyAccessor: FirPropertyAccessor, data: FirResolvedDeclarationStatus?): FirStatement {
        return propertyAccessor.also { transformProperty(it.propertySymbol.fir, data) }
    }

    override fun transformEnumEntry(
        enumEntry: FirEnumEntry,
        data: FirResolvedDeclarationStatus?
    ): FirStatement = whileAnalysing(session, enumEntry) {
        enumEntry.transformStatus(this, statusResolver.resolveStatus(enumEntry, containingClass, isLocal = false))
        return transformDeclaration(enumEntry, data) as FirEnumEntry
    }

    override fun transformValueParameter(
        valueParameter: FirValueParameter,
        data: FirResolvedDeclarationStatus?
    ): FirStatement {
        return transformDeclaration(valueParameter, data) as FirStatement
    }

    override fun transformTypeParameter(
        typeParameter: FirTypeParameter,
        data: FirResolvedDeclarationStatus?
    ): FirTypeParameterRef {
        return transformDeclaration(typeParameter, data) as FirTypeParameter
    }

    override fun transformBlock(block: FirBlock, data: FirResolvedDeclarationStatus?): FirStatement {
        return block
    }
}
