/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import org.jetbrains.kotlin.builtins.StandardNames.BACKING_FIELD
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.scopes.DelicateScopeAPI
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.util.PersistentMultimap
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

class FirLocalScope private constructor(
    val properties: PersistentMap<Name, FirVariableSymbol<*>>,
    val functions: PersistentMultimap<Name, FirNamedFunctionSymbol>,
    val classLikeSymbols: PersistentMap<Name, FirClassLikeSymbol<*>>,
    val useSiteSession: FirSession
) : FirContainingNamesAwareScope() {
    constructor(session: FirSession) : this(persistentMapOf(), PersistentMultimap(), persistentMapOf(), session)

    fun storeClassOrTypeAlias(classLikeDeclaration: FirClassLikeDeclaration, session: FirSession): FirLocalScope {
        val name = when (classLikeDeclaration) {
            is FirRegularClass -> classLikeDeclaration.name
            is FirTypeAlias -> classLikeDeclaration.name
            else -> shouldNotBeCalled()
        }
        return FirLocalScope(
            properties, functions, classLikeSymbols.put(name, classLikeDeclaration.symbol), session
        )
    }

    fun storeFunction(function: FirSimpleFunction, session: FirSession): FirLocalScope {
        return FirLocalScope(
            properties, functions.put(function.name, function.symbol), classLikeSymbols, session
        )
    }

    fun storeVariable(variable: FirVariable, session: FirSession): FirLocalScope {
        return FirLocalScope(
            properties.put(variable.name, variable.symbol), functions, classLikeSymbols, session
        )
    }

    fun storeBackingField(property: FirProperty, session: FirSession): FirLocalScope {
        val enhancedProperties = property.backingField?.symbol?.let {
            properties.put(BACKING_FIELD, it)
        }

        return FirLocalScope(
            enhancedProperties ?: properties,
            functions,
            classLikeSymbols,
            session
        )
    }

    override fun processFunctionsByName(name: Name, processor: (FirNamedFunctionSymbol) -> Unit) {
        for (function in functions[name]) {
            processor(function)
        }
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        val property = properties[name]
        if (property != null) {
            processor(property)
        }
    }

    override fun processClassifiersByNameWithSubstitution(name: Name, processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit) {
        val classLikeSymbol = classLikeSymbols[name]
        if (classLikeSymbol != null) {
            val substitution = classLikeSymbol.typeParameterSymbols.associateWith { it.toConeType() }
            processor(classLikeSymbol, substitutorByMap(substitution, useSiteSession, allowIdenticalSubstitution = true))
        }
    }

    override fun mayContainName(name: Name): Boolean {
        return properties.containsKey(name) || functions[name].isNotEmpty() || classLikeSymbols.containsKey(name)
    }

    override fun getCallableNames(): Set<Name> = properties.keys + functions.keys
    override fun getClassifierNames(): Set<Name> = classLikeSymbols.keys

    @DelicateScopeAPI
    override fun withReplacedSessionOrNull(newSession: FirSession, newScopeSession: ScopeSession): FirLocalScope? {
        return null
    }
}
