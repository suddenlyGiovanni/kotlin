/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.builtins.StandardNames.BUILT_INS_PACKAGE_FQ_NAMES
import org.jetbrains.kotlin.builtins.StandardNames.DATA_CLASS_COPY
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.analysis.checkers.isVisibleInClass
import org.jetbrains.kotlin.fir.backend.generators.isExternalParent
import org.jetbrains.kotlin.fir.backend.utils.ConversionTypeOrigin
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.utils.contextParametersForFunctionOrContainingProperty
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.declarations.utils.nameOrSpecialName
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.descriptors.FirBuiltInsPackageFragment
import org.jetbrains.kotlin.fir.descriptors.FirModuleDescriptor
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazyClass
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazyConstructor
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazyProperty
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazySimpleFunction
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.calls.FirSimpleSyntheticPropertySymbol
import org.jetbrains.kotlin.fir.resolve.getContainingClass
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.types.classLikeLookupTagIfAny
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin.Companion.FILLED_FOR_UNBOUND_SYMBOL
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.expressions.IrSyntheticBodyKind
import org.jetbrains.kotlin.ir.irFlag
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.createThisReceiverParameter
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.load.kotlin.FacadeClassSource
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerAbiStability
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.serialization.deserialization.descriptors.PreReleaseInfo
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled
import org.jetbrains.kotlin.utils.exceptions.ExceptionAttachmentBuilder
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.requireWithAttachment
import org.jetbrains.kotlin.utils.threadLocal
import java.util.concurrent.ConcurrentHashMap

class Fir2IrDeclarationStorage(
    private val c: Fir2IrComponents,
    private val sourceModuleDescriptor: FirModuleDescriptor,
    commonMemberStorage: Fir2IrCommonMemberStorage
) : Fir2IrComponents by c {

    private val fragmentCache: ConcurrentHashMap<FqName, ExternalPackageFragments> = ConcurrentHashMap()
    private val moduleDescriptorCache: ConcurrentHashMap<FirModuleData, FirModuleDescriptor> = ConcurrentHashMap()

    private class ExternalPackageFragments(
        val fragmentsForDependencies: ConcurrentHashMap<FirModuleData, IrExternalPackageFragment>,
        val builtinFragmentsForDependencies: ConcurrentHashMap<FirModuleData, IrExternalPackageFragment>,
        val fragmentForPrecompiledBinaries: IrExternalPackageFragment
    )

    private val fileCache: ConcurrentHashMap<FirFile, IrFile> = ConcurrentHashMap()

    private val scriptCache: ConcurrentHashMap<FirScript, IrScript> = ConcurrentHashMap()

    private val replSnippetCache: ConcurrentHashMap<FirReplSnippet, IrReplSnippet> = ConcurrentHashMap()

    class DataClassGeneratedFunctionsStorage {
        var hashCodeSymbol: IrSimpleFunctionSymbol? = null
            set(value) {
                field = value
            }
        var toStringSymbol: IrSimpleFunctionSymbol? = null
        var equalsSymbol: IrSimpleFunctionSymbol? = null
    }

    private val functionCache: ConcurrentHashMap<FirFunction, IrSimpleFunctionSymbol> = commonMemberStorage.functionCache
    private val dataClassGeneratedFunctionsCache: ConcurrentHashMap<FirClass, DataClassGeneratedFunctionsStorage> =
        commonMemberStorage.dataClassGeneratedFunctionsCache

    private val constructorCache: ConcurrentHashMap<FirConstructor, IrConstructorSymbol> = commonMemberStorage.constructorCache

    private val initializerCache: ConcurrentHashMap<FirAnonymousInitializer, IrAnonymousInitializer> = ConcurrentHashMap()

    class PropertyCacheStorage(
        val normal: ConcurrentHashMap<FirProperty, IrPropertySymbol>,
        val synthetic: ConcurrentHashMap<SyntheticPropertyKey, IrPropertySymbol>
    ) {
        /**
         * Fir synthetic properties are session-dependent, so it can't be used as a cache key
         * That's why, we are using original java function as a key in that case.
         */
        data class SyntheticPropertyKey(
            val originalFunction: FirSimpleFunction,
            val dispatchReceiverLookupTag: ConeClassLikeLookupTag?,
        )

        private val FirSyntheticProperty.cacheKey: SyntheticPropertyKey
            get() {
                val originalFunction = symbol.getterSymbol!!.delegateFunctionSymbol.fir
                val dispatchReceiverLookupTag = runIf(symbol !is FirSimpleSyntheticPropertySymbol) {
                    dispatchReceiverType?.classLikeLookupTagIfAny
                }
                return SyntheticPropertyKey(originalFunction, dispatchReceiverLookupTag)
            }

        operator fun set(fir: FirProperty, value: IrPropertySymbol) {
            when (fir) {
                is FirSyntheticProperty -> synthetic[fir.cacheKey] = value
                else -> normal[fir] = value
            }
        }

        operator fun get(fir: FirProperty): IrPropertySymbol? {
            return when (fir) {
                is FirSyntheticProperty -> synthetic[fir.cacheKey]
                else -> normal[fir]
            }
        }
    }

    private val propertyCache = PropertyCacheStorage(commonMemberStorage.propertyCache, commonMemberStorage.syntheticPropertyCache)
    private val getterForPropertyCache: ConcurrentHashMap<IrSymbol, IrSimpleFunctionSymbol> =
        commonMemberStorage.getterForPropertyCache
    private val setterForPropertyCache: ConcurrentHashMap<IrSymbol, IrSimpleFunctionSymbol> =
        commonMemberStorage.setterForPropertyCache
    private val backingFieldForPropertyCache: ConcurrentHashMap<IrPropertySymbol, IrFieldSymbol> =
        commonMemberStorage.backingFieldForPropertyCache
    private val propertyForBackingFieldCache: ConcurrentHashMap<IrFieldSymbol, IrPropertySymbol> =
        commonMemberStorage.propertyForBackingFieldCache
    private val delegateVariableForPropertyCache: ConcurrentHashMap<IrLocalDelegatedPropertySymbol, IrVariableSymbol> =
        commonMemberStorage.delegateVariableForPropertyCache

    /**
     * This function is quite messy and doesn't have a good contract of what exactly is traversed.
     * The basic idea is to traverse the symbols which can be reasonably referenced from other modules.
     *
     * Be careful when using it, and avoid it, except really needed.
     */
    @Suppress("unused")
    @DelicateDeclarationStorageApi
    fun forEachCachedDeclarationSymbol(block: (IrSymbol) -> Unit) {
        functionCache.values.forEachSkipFakeOverrides(block)
        constructorCache.values.forEach(block)
        propertyCache.normal.values.forEachSkipFakeOverrides(block)
        propertyCache.synthetic.values.forEachSkipFakeOverrides(block)
        getterForPropertyCache.values.forEachSkipFakeOverrides(block)
        setterForPropertyCache.values.forEachSkipFakeOverrides(block)
        backingFieldForPropertyCache.values.forEach(block)
        propertyForBackingFieldCache.values.forEach(block)
        delegateVariableForPropertyCache.values.forEach(block)
    }

    private inline fun <S : IrSymbol> Collection<S>.forEachSkipFakeOverrides(block: (S) -> Unit) {
        for (symbol in this) {
            if (symbol is IrFakeOverrideSymbolBase<*, *, *>) continue
            block(symbol)
        }
    }

    /*
     * FIR declarations for substitution and intersection overrides, and also for delegated members are session dependent,
     *   which means that in an MPP project we can have two different functions for the same substitution overrides
     *  (in common and platform modules)
     *
     * So this cache is needed to have only one IR declaration for both overrides
     *
     * The key here is a pair of the original function (first not f/o) and lookup tag of class for which this fake override was created
     * THe value is IR function, build for this fake override during fir2ir translation of the module that contains parent class of this function
     */
    private val irForFirSessionDependantDeclarationMap: MutableMap<FakeOverrideIdentifier, IrSymbol> =
        commonMemberStorage.irForFirSessionDependantDeclarationMap

    data class FakeOverrideIdentifier(
        val originalSymbol: FirCallableSymbol<*>,
        val dispatchReceiverLookupTag: ConeClassLikeLookupTag,
        val parentIsExpect: Boolean,
    ) {
        companion object {
            context(c: Fir2IrComponents)
            operator fun invoke(
                originalSymbol: FirCallableSymbol<*>,
                dispatchReceiverLookupTag: ConeClassLikeLookupTag
            ): FakeOverrideIdentifier {
                return FakeOverrideIdentifier(
                    originalSymbol,
                    dispatchReceiverLookupTag,
                    dispatchReceiverLookupTag.toRegularClassSymbol(c.session)?.isExpect == true
                )
            }
        }
    }

    private val delegatedReverseCache: ConcurrentHashMap<IrSymbol, FirDeclaration> = ConcurrentHashMap()

    private val fieldForDelegatedSupertypeCache: ConcurrentHashMap<FirField, IrFieldSymbol> = ConcurrentHashMap()
    private val delegatedClassesMap: MutableMap<IrClassSymbol, MutableMap<IrClassSymbol, IrFieldSymbol>> = commonMemberStorage.delegatedClassesInfo
    private val firClassesWithInheritanceByDelegation: MutableSet<FirClass> = commonMemberStorage.firClassesWithInheritanceByDelegation

    private val localStorage: Fir2IrLocalCallableStorage by threadLocal {
        Fir2IrLocalCallableStorage(commonMemberStorage.localCallableCache)
    }

    // TODO: move to common storage
    private val propertyForFieldCache: ConcurrentHashMap<FirField, IrPropertySymbol> = ConcurrentHashMap()

    // ------------------------------------ package fragments ------------------------------------

    fun getDependenciesModuleDescriptor(moduleData: FirModuleData): FirModuleDescriptor {
        return moduleDescriptorCache.getOrPut(moduleData) {
            FirModuleDescriptor.createDependencyModuleDescriptor(
                moduleData,
                sourceModuleDescriptor.builtIns
            )
        }
    }

    fun getIrExternalPackageFragment(
        fqName: FqName,
        moduleData: FirModuleData,
        firOrigin: FirDeclarationOrigin = FirDeclarationOrigin.Library
    ): IrExternalPackageFragment {
        return getIrExternalOrBuiltInsPackageFragment(fqName, moduleData, firOrigin, false)
    }

    private fun getIrExternalOrBuiltInsPackageFragment(
        fqName: FqName,
        moduleData: FirModuleData,
        firOrigin: FirDeclarationOrigin,
        allowBuiltins: Boolean
    ): IrExternalPackageFragment {
        val isBuiltIn = allowBuiltins && fqName in BUILT_INS_PACKAGE_FQ_NAMES
        val fragments = fragmentCache.getOrPut(fqName) {
            val fragmentForPrecompiledBinaries = callablesGenerator.createExternalPackageFragment(fqName, sourceModuleDescriptor)
            ExternalPackageFragments(ConcurrentHashMap(), ConcurrentHashMap(), fragmentForPrecompiledBinaries)
        }
        // Make sure that external package fragments have a different module descriptor. The module descriptors are compared
        // to determine if objects need regeneration because they are from different modules.
        // But keep the original module descriptor for the fragments coming from parts compiled on the previous incremental step
        return when (firOrigin) {
            FirDeclarationOrigin.Precompiled -> fragments.fragmentForPrecompiledBinaries
            else -> {
                val moduleDescriptor = getDependenciesModuleDescriptor(moduleData)
                if (isBuiltIn) {
                    fragments.builtinFragmentsForDependencies.getOrPut(moduleData) {
                        callablesGenerator.createExternalPackageFragment(FirBuiltInsPackageFragment(fqName, moduleDescriptor))
                    }
                } else {
                    fragments.fragmentsForDependencies.getOrPut(moduleData) {
                        callablesGenerator.createExternalPackageFragment(fqName, moduleDescriptor)
                    }
                }
            }
        }
    }

    // ------------------------------------ files ------------------------------------

    fun registerFile(firFile: FirFile, irFile: IrFile) {
        fileCache[firFile] = irFile
    }

    fun getIrFile(firFile: FirFile): IrFile {
        return fileCache[firFile]!!
    }

    private class NonCachedSourceFacadeContainerSource(
        override val className: JvmClassName,
        override val facadeClassName: JvmClassName?,
    ) : DeserializedContainerSource, FacadeClassSource {
        override val incompatibility get() = null
        override val preReleaseInfo: PreReleaseInfo get() = PreReleaseInfo.DEFAULT_VISIBLE
        override val abiStability get() = DeserializedContainerAbiStability.STABLE
        override val presentableString get() = className.internalName
        override val jvmClassName: JvmClassName? get() = null

        override fun getContainingFile(): SourceFile = SourceFile.NO_SOURCE_FILE
    }

    // ------------------------------------ functions ------------------------------------

    fun getCachedIrFunctionSymbol(
        function: FirFunction,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag? = null,
    ): IrSimpleFunctionSymbol? {
        return if (function is FirSimpleFunction) getCachedIrFunctionSymbol(function, fakeOverrideOwnerLookupTag)
        else localStorage.getLocalFunctionSymbol(function)
    }

    fun getCachedIrFunctionSymbol(
        function: FirSimpleFunction,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag? = null,
    ): IrSimpleFunctionSymbol? {
        if (function.visibility == Visibilities.Local) {
            return localStorage.getLocalFunctionSymbol(function)
        }
        runIf(function.origin.generatedAnyMethod) {
            val containingClass = function.getContainingClass()!!
            val cache = dataClassGeneratedFunctionsCache.computeIfAbsent(containingClass) { DataClassGeneratedFunctionsStorage() }
            val cachedFunction = when (function.nameOrSpecialName) {
                OperatorNameConventions.EQUALS -> cache.equalsSymbol
                OperatorNameConventions.HASH_CODE -> cache.hashCodeSymbol
                OperatorNameConventions.TO_STRING -> cache.toStringSymbol
                else -> return@runIf // componentN functions are the same for all sessions
            }
            return cachedFunction?.let(symbolsMappingForLazyClasses::remapFunctionSymbol)
        }
        val cachedIrCallable = getCachedIrCallableSymbol(
            function,
            fakeOverrideOwnerLookupTag,
            functionCache::get
        )
        return cachedIrCallable?.let(symbolsMappingForLazyClasses::remapFunctionSymbol)
    }

    /**
     * @param allowLazyDeclarationsCreation should be passed only during fake-override generation
     */
    fun createAndCacheIrFunction(
        function: FirFunction,
        irParent: IrDeclarationParent?,
        predefinedOrigin: IrDeclarationOrigin? = null,
        isLocal: Boolean = false,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag? = null,
        allowLazyDeclarationsCreation: Boolean = false
    ): IrSimpleFunction {
        val symbol = getIrFunctionSymbol(function.symbol, fakeOverrideOwnerLookupTag, isLocal) as IrSimpleFunctionSymbol
        return callablesGenerator.createIrFunction(
            function,
            irParent,
            symbol,
            predefinedOrigin,
            isLocal = isLocal,
            fakeOverrideOwnerLookupTag = fakeOverrideOwnerLookupTag,
            allowLazyDeclarationsCreation
        )
    }

    internal fun createFunctionSymbol(): IrSimpleFunctionSymbol {
        return IrSimpleFunctionSymbolImpl()
    }

    private fun createMemberFunctionSymbol(
        function: FirFunction,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag? = null,
        parentIsExternal: Boolean
    ): IrSimpleFunctionSymbol {
        if (
            parentIsExternal ||
            function !is FirSimpleFunction ||
            !function.isFakeOverrideOrDelegated(fakeOverrideOwnerLookupTag)
        ) {
            return createFunctionSymbol()
        }
        val containingClassSymbol = findContainingIrClassSymbol(function, fakeOverrideOwnerLookupTag)
        val originalFirFunction = function.unwrapFakeOverridesOrDelegated()
        val originalSymbol = getIrFunctionSymbol(originalFirFunction.symbol) as IrSimpleFunctionSymbol
        return IrFunctionFakeOverrideSymbol(originalSymbol, containingClassSymbol, idSignature = null)
    }

    private fun findContainingIrClassSymbol(
        callable: FirCallableDeclaration,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag?,
    ): IrClassSymbol {
        val containingClassLookupTag = when {
            fakeOverrideOwnerLookupTag != null -> fakeOverrideOwnerLookupTag
            callable.isSubstitutionOrIntersectionOverride -> callable.containingClassLookupTag()
            callable.isDelegated -> callable.containingClassLookupTag()
            else -> shouldNotBeCalled()
        }
        requireNotNull(containingClassLookupTag) { "Containing class not found for ${callable.render()}" }
        return classifierStorage.getIrClassSymbol(containingClassLookupTag)
            ?: error("IR class for $containingClassLookupTag not found")
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun cacheIrFunctionSymbol(
        function: FirFunction,
        irFunctionSymbol: IrSimpleFunctionSymbol,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag?,
    ) {
        when {
            function.visibility == Visibilities.Local || function is FirAnonymousFunction -> {
                localStorage.putLocalFunction(function, irFunctionSymbol)
            }

            function.isFakeOverrideOrDelegated(fakeOverrideOwnerLookupTag) -> {
                val originalFunction = function.unwrapFakeOverridesOrDelegated()
                val key = FakeOverrideIdentifier(
                    originalFunction.symbol,
                    fakeOverrideOwnerLookupTag ?: function.containingClassLookupTag()!!,
                )
                irForFirSessionDependantDeclarationMap[key] = irFunctionSymbol
            }

            function.origin.generatedAnyMethod -> {
                val name = function.nameOrSpecialName
                /*
                 * During regular compilation `equals`, `hashCode` and `toString` are generated separately using DataClassMemberGenerator.
                 *   `componentN` and `copy` are generated on a FIR level, so they are created here like any other regular function.
                 *
                 * In AA API mode the source-based data class may come as a dependency, which means that even for generated method we
                 *   will create a regular Fir2IrLazyFunction
                 *
                 * Additionally in case of pulling declarations from other snippets, they are
                 */
                require(
                    OperatorNameConventions.isComponentN(name) || name == DATA_CLASS_COPY ||
                            configuration.allowNonCachedDeclarations ||
                            (irFunctionSymbol.owner.parent as? Fir2IrLazyClass)?.origin == IrDeclarationOrigin.REPL_FROM_OTHER_SNIPPET
                ) {
                    buildString {
                        appendLine("Only componentN functions should be cached this way, but got: ${function.render()}")
                        appendLine("fakeOverrideOwnerLookupTag: $fakeOverrideOwnerLookupTag")
                        appendLine("Dispatch receiver: ${function.dispatchReceiverType}")
                    }
                }
                functionCache[function] = irFunctionSymbol
            }

            else -> {
                functionCache[function] = irFunctionSymbol
            }
        }
    }

    fun <T : IrFunction> T.putParametersInScope(function: FirFunction): T {
        val contextParameters = function.contextParametersForFunctionOrContainingProperty()

        for ((firParameter, irParameter) in contextParameters.zip(this.parameters.filter { it.kind == IrParameterKind.Context })) {
            if (!firParameter.isLegacyContextReceiver()) {
                localStorage.putParameter(firParameter, irParameter.symbol)
            }
        }

        for ((firParameter, irParameter) in function.valueParameters.zip(parameters.filter { it.kind == IrParameterKind.Regular })) {
            localStorage.putParameter(firParameter, irParameter.symbol)
        }
        return this
    }

    internal fun cacheGeneratedFunction(firFunction: FirSimpleFunction, irFunction: IrSimpleFunction) {
        val containingClass = firFunction.getContainingClass()!!
        val cache = dataClassGeneratedFunctionsCache.computeIfAbsent(containingClass) { DataClassGeneratedFunctionsStorage() }
        val irSymbol = irFunction.symbol
        when (val name = firFunction.nameOrSpecialName) {
            OperatorNameConventions.EQUALS -> cache.equalsSymbol = irSymbol
            OperatorNameConventions.HASH_CODE -> cache.hashCodeSymbol = irSymbol
            OperatorNameConventions.TO_STRING -> cache.toStringSymbol = irSymbol
            else -> error(
                buildString {
                    appendLine("Only componentN functions should be cached this way, but got: ${firFunction.render()}")
                    appendLine("Dispatch receiver: ${firFunction.dispatchReceiverType}")
                }
            )
        }
    }

    // ------------------------------------ constructors ------------------------------------

    fun getCachedIrConstructorSymbol(constructor: FirConstructor): IrConstructorSymbol? {
        return constructorCache[constructor]
    }

    fun createAndCacheIrConstructor(
        constructor: FirConstructor,
        irParent: () -> IrClass,
        predefinedOrigin: IrDeclarationOrigin? = null,
        isLocal: Boolean = false,
    ): IrConstructor {
        val symbol = getIrConstructorSymbol(constructor.symbol, potentiallyExternal = !isLocal)
        return callablesGenerator.createIrConstructor(
            constructor,
            irParent(),
            symbol,
            predefinedOrigin,
            allowLazyDeclarationsCreation = false
        )
    }

    private fun cacheIrConstructorSymbol(constructor: FirConstructor, irConstructorSymbol: IrConstructorSymbol) {
        constructorCache[constructor] = irConstructorSymbol
    }

    fun getIrConstructorSymbol(firConstructorSymbol: FirConstructorSymbol, potentiallyExternal: Boolean = true): IrConstructorSymbol {
        val constructor = firConstructorSymbol.fir
        getCachedIrConstructorSymbol(constructor)?.let { return it }

        // caching of created constructor is not called here, because `callablesGenerator` calls `cacheIrConstructor` by itself
        val symbol = IrConstructorSymbolImpl()
        if (potentiallyExternal) {
            val irParent = findIrParent(constructor, fakeOverrideOwnerLookupTag = null)
            if (irParent.isExternalParent()) {
                callablesGenerator.createIrConstructor(
                    constructor,
                    irParent as IrClass,
                    symbol,
                    constructor.computeExternalOrigin(),
                    allowLazyDeclarationsCreation = true
                ).also {
                    check(it is Fir2IrLazyConstructor)
                }
            }
        }
        cacheIrConstructorSymbol(constructor, symbol)

        return symbol
    }

    // ------------------------------------ properties ------------------------------------

    /**
     *    There is a difference in how FIR and IR treat synthetic properties (properties built upon java getter + optional java setter)
     *    For FIR they are really synthetic and exist only during call resolution, so FIR creates a new instance of FirSyntheticProperty
     *    each time it resolves some call to such property
     *    In IR synthetic properties are fair properties that are present in IR Java classes
     *
     *    This leads to the situation when synthetic property does not have a stable key (because FIR instance is new each time) and the only
     *    source of truth is a symbol table. To fix it (and avoid using symbol table as a storage), a pair of original getter and setter is
     *    used as a key for storage IR for synthetic properties. And to avoid introducing special cache of FirSyntheticPropertyKey -> IrProperty
     *    additional mapping level is introduced
     *
     *    - FirSyntheticPropertyKey is mapped to the first FIR synthetic property which was processed by FIR2IR
     *    - this property is mapped to IrProperty using regular propertyCache
     *
     * IMPORTANT: this whole story requires to call [prepareProperty] or [preparePropertySymbol] in the beginning of any public method
     *   which accepts arbitary FirProperty or FirPropertySymbol
     */
    @Suppress("KDocUnresolvedReference")
    private data class FirSyntheticPropertyKey(
        val originalForGetter: FirSimpleFunction,
        val originalForSetter: FirSimpleFunction?,
    ) {
        constructor(property: FirSyntheticProperty) : this(property.getter.delegate, property.setter?.delegate)
    }

    private val originalForSyntheticProperty: ConcurrentHashMap<FirSyntheticPropertyKey, FirSyntheticProperty> = ConcurrentHashMap()

    private fun prepareProperty(property: FirProperty): FirProperty {
        return when {
            property is FirSyntheticProperty && property.symbol is FirSimpleSyntheticPropertySymbol -> {
                originalForSyntheticProperty.getOrPut(FirSyntheticPropertyKey(property)) { property }
            }
            else -> property
        }
    }

    fun createAndCacheIrProperty(
        property: FirProperty,
        irParent: IrDeclarationParent?,
        predefinedOrigin: IrDeclarationOrigin? = null,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag? = null,
        allowLazyDeclarationsCreation: Boolean = false
    ): IrProperty {
        @Suppress("NAME_SHADOWING")
        val property = prepareProperty(property)

        val symbols = getIrPropertySymbols(property.symbol, fakeOverrideOwnerLookupTag)

        return callablesGenerator.createIrProperty(
            property, irParent, symbols, predefinedOrigin, fakeOverrideOwnerLookupTag, allowLazyDeclarationsCreation
        )
    }

    private fun createPropertySymbols(
        property: FirProperty,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag?,
        parentIsExternal: Boolean
    ): PropertySymbols {
        if (
            !parentIsExternal &&
            property.isFakeOverrideOrDelegated(fakeOverrideOwnerLookupTag)
        ) {
            return createFakeOverridePropertySymbols(property, fakeOverrideOwnerLookupTag)
        }

        val isJavaOrigin = property.origin is FirDeclarationOrigin.Java
        val propertySymbol = IrPropertySymbolImpl()
        val getterSymbol = runIf(!isJavaOrigin) { createFunctionSymbol() }
        val setterSymbol = runIf(!isJavaOrigin && property.isVar) { createFunctionSymbol() }

        val backingFieldSymbol = runIf(property.delegate != null || extensions.hasBackingField(property, session)) {
            createFieldSymbol()
        }

        return PropertySymbols(propertySymbol, getterSymbol, setterSymbol, backingFieldSymbol)
    }

    private fun createFakeOverridePropertySymbols(
        property: FirProperty,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag?,
    ): PropertySymbols {
        val originalFirProperty = property.unwrapFakeOverridesOrDelegated()
        val originalSymbols = getIrPropertySymbols(originalFirProperty.symbol)
        require(property.isStubPropertyForPureField != true) {
            "What are we doing here? ${property.render()}"
        }

        val containingClassSymbol = findContainingIrClassSymbol(property, fakeOverrideOwnerLookupTag)
        val propertySymbol = IrPropertyFakeOverrideSymbol(originalSymbols.propertySymbol, containingClassSymbol, idSignature = null)
        val getterSymbol = originalSymbols.getterSymbol?.let {
            IrFunctionFakeOverrideSymbol(it, containingClassSymbol, idSignature = null)
        }

        val setterSymbol = runIf(property.isVar) {
            val setterIsVisible = property.setter?.let { setter ->
                fakeOverrideOwnerLookupTag?.toClassSymbol(session)?.fir?.let { containingClass ->
                    setter.isVisibleInClass(containingClass)
                }
            } ?: true
            runIf(setterIsVisible) {
                IrFunctionFakeOverrideSymbol(originalSymbols.setterSymbol!!, containingClassSymbol, idSignature = null)
            }
        }
        return PropertySymbols(propertySymbol, getterSymbol, setterSymbol, backingFieldSymbol = null)
    }

    private fun cacheIrPropertySymbols(
        property: FirProperty,
        symbols: PropertySymbols,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag?,
    ) {
        val irPropertySymbol = symbols.propertySymbol
        symbols.backingFieldSymbol?.let {
            backingFieldForPropertyCache[irPropertySymbol] = it
            propertyForBackingFieldCache[it] = irPropertySymbol
        }
        symbols.getterSymbol?.let {
            getterForPropertyCache[irPropertySymbol] = it
        }
        symbols.setterSymbol?.let {
            setterForPropertyCache[irPropertySymbol] = it
        }
        if (property.isFakeOverrideOrDelegated(fakeOverrideOwnerLookupTag)) {
            val originalProperty = property.unwrapFakeOverridesOrDelegated()
            val key = FakeOverrideIdentifier(
                originalProperty.symbol,
                fakeOverrideOwnerLookupTag ?: property.containingClassLookupTag()!!,
            )
            irForFirSessionDependantDeclarationMap[key] = irPropertySymbol
        } else {
            propertyCache[property] = irPropertySymbol
        }
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    fun getIrPropertySymbol(
        firPropertySymbol: FirPropertySymbol,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag? = null,
    ): IrSymbol {
        val property = prepareProperty(firPropertySymbol.fir)
        if (property.isLocal) {
            return localStorage.getDelegatedProperty(property) ?: getIrVariableSymbol(property)
        }
        getCachedIrPropertySymbol(property, fakeOverrideOwnerLookupTag)?.let { return it }
        return getIrPropertySymbols(firPropertySymbol, fakeOverrideOwnerLookupTag).propertySymbol
    }

    private fun getIrPropertySymbols(
        firPropertySymbol: FirPropertySymbol,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag? = null,
    ): PropertySymbols {
        val property = prepareProperty(firPropertySymbol.fir)
        getCachedIrPropertySymbols(property, fakeOverrideOwnerLookupTag)?.let { return it }
        return createAndCacheIrPropertySymbols(property, fakeOverrideOwnerLookupTag)
    }

    private fun createAndCacheIrPropertySymbols(
        property: FirProperty,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag?,
    ): PropertySymbols {
        val irParent = findIrParent(property, fakeOverrideOwnerLookupTag)
        if (irParent?.isExternalParent() == true) {
            val symbols = createPropertySymbols(property, fakeOverrideOwnerLookupTag, parentIsExternal = true)
            val firForLazyProperty = calculateFirForLazyDeclaration(
                property, fakeOverrideOwnerLookupTag, lazyFakeOverrideGenerator::createFirPropertyFakeOverrideIfNeeded
            )

            callablesGenerator.createIrProperty(
                firForLazyProperty,
                irParent,
                symbols,
                predefinedOrigin = firForLazyProperty.computeExternalOrigin(),
                allowLazyDeclarationsCreation = true
            ).also {
                check(it is Fir2IrLazyProperty)
            }

            cacheIrPropertySymbols(property, symbols, fakeOverrideOwnerLookupTag)
            return symbols
        }

        val symbols = createPropertySymbols(property, fakeOverrideOwnerLookupTag, parentIsExternal = false)
        cacheIrPropertySymbols(property, symbols, fakeOverrideOwnerLookupTag)

        return symbols
    }

    fun getCachedIrPropertySymbol(
        property: FirProperty,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag?
    ): IrPropertySymbol? {
        @Suppress("NAME_SHADOWING")
        val property = prepareProperty(property)
        val symbol = getCachedIrCallableSymbol(
            property,
            fakeOverrideOwnerLookupTag,
            propertyCache::get
        ) ?: return null
        return symbolsMappingForLazyClasses.remapPropertySymbol(symbol)
    }

    private fun getCachedIrPropertySymbols(
        property: FirProperty,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag?
    ): PropertySymbols? {
        val propertySymbol = getCachedIrPropertySymbol(property, fakeOverrideOwnerLookupTag) ?: return null
        return PropertySymbols(
            propertySymbol,
            findGetterOfProperty(propertySymbol)!!,
            findSetterOfProperty(propertySymbol),
            findBackingFieldOfProperty(propertySymbol)
        )
    }

    fun findGetterOfProperty(propertySymbol: IrPropertySymbol): IrSimpleFunctionSymbol? {
        return getterForPropertyCache[propertySymbol]?.let(symbolsMappingForLazyClasses::remapFunctionSymbol)
    }

    fun findSetterOfProperty(propertySymbol: IrPropertySymbol): IrSimpleFunctionSymbol? {
        return setterForPropertyCache[propertySymbol]?.let(symbolsMappingForLazyClasses::remapFunctionSymbol)
    }

    fun findBackingFieldOfProperty(propertySymbol: IrPropertySymbol): IrFieldSymbol? {
        return backingFieldForPropertyCache[propertySymbol]
    }

    // ------------------------------------ fields ------------------------------------

    /**
     * @return [IrFieldSymbol] if [firFieldSymbol] is a field for supertype delegating
     * @return [IrPropertySymbol] if [firFieldSymbol] is a java field or fake-override of it
     */
    fun getIrSymbolForField(
        firFieldSymbol: FirFieldSymbol,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag?
    ): IrSymbol {
        val field = firFieldSymbol.fir
        val originalField = field.unwrapFakeOverrides()

        @Suppress("NAME_SHADOWING")
        val fakeOverrideOwnerLookupTag = fakeOverrideOwnerLookupTag ?: runIf(field !== originalField) {
            field.dispatchReceiverClassLookupTagOrNull()
        }
        return when {
            originalField.origin == FirDeclarationOrigin.Synthetic.DelegateField ->
                getIrSymbolForSupertypeDelegateField(originalField, fakeOverrideOwnerLookupTag)

            originalField.isJavaOrEnhancement -> getIrPropertySymbolForJavaField(field, fakeOverrideOwnerLookupTag)

            else -> errorWithAttachment("Unknown field kind. Only java fields and fields for supertype delegation are supported: ${field.render()}") {
                withFirEntry("field", field)
                withFirEntry("originalField", originalField)
            }
        }
    }

    private fun getIrSymbolForSupertypeDelegateField(
        field: FirField,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag?
    ): IrFieldSymbol {
        requireWithAttachment(
            fakeOverrideOwnerLookupTag == null || field.dispatchReceiverClassLookupTagOrNull() == fakeOverrideOwnerLookupTag,
            { "Field for supertype delegate accessed with incorrect fakeOverrideOwnerLookupTag" }
        ) {
            withEntry("fakeOverrideOwnerLookupTag", fakeOverrideOwnerLookupTag.toString())
            withFirEntry("field", field)
        }
        return fieldForDelegatedSupertypeCache.getOrPut(field) { createFieldSymbol() }
    }

    private fun getIrPropertySymbolForJavaField(
        field: FirField,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag?
    ): IrPropertySymbol {
        getCachedIrSymbolForJavaField(field, fakeOverrideOwnerLookupTag)?.let { return it }

        val symbols = createAndCachePropertySymbolsForJavaField(field, fakeOverrideOwnerLookupTag)
        return symbols.propertySymbol
    }

    private fun createAndCachePropertySymbolsForJavaField(
        field: FirField,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag?
    ): PropertySymbols {
        val irParent = findIrParent(field, fakeOverrideOwnerLookupTag)
        requireNotNull(irParent) { "No IR parent found for field ${field.render()}" }

        val buildAttachment: ExceptionAttachmentBuilder.() -> Unit = {
            withFirEntry("field", field)
            withEntry("fakeOverrideOwnerLookupTag", fakeOverrideOwnerLookupTag.toString())
            withEntry("irParent", irParent.render())
        }

        val isFakeOverride = field.isFakeOverride(fakeOverrideOwnerLookupTag)
        val staticFakeOverride = isFakeOverride && field.isStatic
        val parentIsExternal = irParent.isExternalParent()

        if (staticFakeOverride && !parentIsExternal) {
            errorWithAttachment(
                "Fake-overrides for static field in non-external classes are not allowed: ${field.render()}",
                buildAttachment = buildAttachment
            )
        }

        val symbols = if (isFakeOverride && !staticFakeOverride && !parentIsExternal) {
            /**
             * At this point `field` may be an unwrapped substitution override, so we need to take the most
             *   original field to create f/o symbol
             * This is needed for cases of complex `Java -> Kotlin -> Java -> Kotlin` hierarchies,
             *   see the test compiler/testData/codegen/box/fakeOverride/fieldInJKJKHierarchy.kt
             */
            createFakeOverridePropertySymbolsForJavaField(field.unwrapFakeOverrides(), fakeOverrideOwnerLookupTag)
        } else {
            requireWithAttachment(
                irParent.isExternalParent(),
                { "Non f/o field with non-external parent: ${field.render()}" },
                buildAttachment = buildAttachment
            )

            val fieldSymbol = createFieldSymbol()
            val propertySymbol = IrPropertySymbolImpl()
            val origin = field.computeExternalOrigin()

            val firForLazyField = calculateFirForLazyDeclaration(
                field, fakeOverrideOwnerLookupTag, lazyFakeOverrideGenerator::createFirFieldFakeOverrideIfNeeded
            )

            lazyDeclarationsGenerator.createIrPropertyForPureField(firForLazyField, fieldSymbol, propertySymbol, irParent, origin)
            PropertySymbols(
                propertySymbol,
                getterSymbol = null,
                setterSymbol = null,
                backingFieldSymbol = fieldSymbol
            )
        }
        cacheIrPropertySymbolsForPureField(field, symbols, fakeOverrideOwnerLookupTag, isFakeOverride)
        return symbols
    }

    private fun cacheIrPropertySymbolsForPureField(
        field: FirField,
        symbols: PropertySymbols,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag?,
        isFakeOverride: Boolean
    ) {
        val irPropertySymbol = symbols.propertySymbol
        symbols.backingFieldSymbol?.let {
            backingFieldForPropertyCache[irPropertySymbol] = it
            propertyForBackingFieldCache[it] = irPropertySymbol
        }
        if (isFakeOverride) {
            val originalField = field.unwrapFakeOverrides()
            val key = FakeOverrideIdentifier(
                originalField.symbol,
                fakeOverrideOwnerLookupTag ?: field.containingClassLookupTag()!!,
            )
            irForFirSessionDependantDeclarationMap[key] = irPropertySymbol
        } else {
            propertyForFieldCache[field] = irPropertySymbol
        }
    }

    private fun createFakeOverridePropertySymbolsForJavaField(
        field: FirField,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag?
    ): PropertySymbols {
        val originalPropertySymbol = getIrPropertySymbolForJavaField(field, fakeOverrideOwnerLookupTag = null)
        val originalFieldSymbol = findBackingFieldOfProperty(originalPropertySymbol)
        requireWithAttachment(
            originalFieldSymbol != null,
            { "No original backing field found for f/o field" }
        ) {
            withFirEntry("field", field)
            withEntry("fakeOverrideOwnerLookupTag", fakeOverrideOwnerLookupTag.toString())
        }

        val containingClassSymbol = findContainingIrClassSymbol(field, fakeOverrideOwnerLookupTag)
        val propertySymbol = IrPropertyFakeOverrideSymbol(originalPropertySymbol, containingClassSymbol, idSignature = null)
        val fieldSymbol = IrFieldFakeOverrideSymbol(
            originalFieldSymbol,
            containingClassSymbol,
            idSignature = null,
            correspondingPropertySymbol = propertySymbol
        )
        return PropertySymbols(
            propertySymbol,
            getterSymbol = null,
            setterSymbol = null,
            backingFieldSymbol = fieldSymbol
        )
    }

    private fun getCachedIrSymbolForJavaField(field: FirField, fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag?): IrPropertySymbol? {
        return getCachedIrCallableSymbol(
            declaration = field,
            fakeOverrideOwnerLookupTag,
            cacheGetter = propertyForFieldCache::get
        )
    }

    private fun createFieldSymbol(): IrFieldSymbol {
        return IrFieldSymbolImpl()
    }

    // ------------------------------------ backing and delegate fields ------------------------------------

    fun getIrBackingFieldSymbol(firBackingFieldSymbol: FirBackingFieldSymbol): IrSymbol {
        return getIrPropertyForwardedSymbol(firBackingFieldSymbol.fir.propertySymbol.fir)
    }

    fun getIrDelegateFieldSymbol(delegateFieldSymbol: FirDelegateFieldSymbol): IrSymbol {
        return getIrPropertyForwardedSymbol(delegateFieldSymbol.fir)
    }

    private fun getIrPropertyForwardedSymbol(fir: FirProperty): IrSymbol {
        if (fir.isLocal) {
            // local property cannot be referenced before declaration, so it's safe to take an owner from the symbol
            @OptIn(UnsafeDuringIrConstructionAPI::class)
            val delegatedProperty = localStorage.getDelegatedProperty(fir)?.owner
            return delegatedProperty?.delegate?.symbol ?: getIrVariableSymbol(fir)
        }
        @OptIn(UnsafeDuringIrConstructionAPI::class)
        propertyCache[fir]?.ownerIfBound()?.let { return it.backingField!!.symbol }
        val irParent = findIrParent(fir, fakeOverrideOwnerLookupTag = null)
        val parentOrigin = (irParent as? IrDeclaration)?.origin ?: IrDeclarationOrigin.DEFINED
        return createAndCacheIrProperty(fir, irParent, predefinedOrigin = parentOrigin).backingField!!.symbol
    }

    fun getCachedIrFieldSymbolForSupertypeDelegateField(field: FirField): IrFieldSymbol? {
        return fieldForDelegatedSupertypeCache[field]
    }

    fun recordSupertypeDelegateFieldMappedToBackingField(field: FirField, irFieldSymbol: IrFieldSymbol) {
        fieldForDelegatedSupertypeCache[field] = irFieldSymbol
    }

    fun recordSupertypeDelegationInformation(containingFirClass: FirClass, irClass: IrClass, superType: IrType, irFieldSymbol: IrFieldSymbol) {
        val delegateMapForClass = delegatedClassesMap.getOrPut(irClass.symbol) { mutableMapOf() }
        val delegatedSuperClass = superType.classOrNull
        if (delegatedSuperClass == null) {
            if (c.configuration.skipBodies) return
            error("No symbol for type $superType")
        }
        require(delegatedSuperClass !in delegateMapForClass) { "Delegate info for supertype $superType already stored" }
        delegateMapForClass[delegatedSuperClass] = irFieldSymbol
        firClassesWithInheritanceByDelegation += containingFirClass
    }

    internal fun createSupertypeDelegateIrField(field: FirField, irClass: IrClass): IrField {
        val symbol = createFieldSymbol()

        val irField = callablesGenerator.createIrField(
            field, irParent = irClass, symbol,
            type = field.initializer?.resolvedType ?: field.returnTypeRef.coneType,
            origin = IrDeclarationOrigin.DELEGATE
        )
        fieldForDelegatedSupertypeCache[field] = symbol
        return irField
    }

    // ------------------------------------ parameters ------------------------------------

    fun createAndCacheParameter(
        valueParameter: FirValueParameter,
        useStubForDefaultValueStub: Boolean = true,
        typeOrigin: ConversionTypeOrigin = ConversionTypeOrigin.DEFAULT,
        skipDefaultParameter: Boolean = false,
        // Use this parameter if you want to insert the actual default value instead of the stub (overrides useStubForDefaultValueStub parameter).
        // This parameter is intended to be used for default values of annotation parameters where they are needed and
        // may produce incorrect results for values that may be encountered outside annotations.
        // Does not do anything if valueParameter.defaultValue is already FirExpressionStub.
        forcedDefaultValueConversion: Boolean = false,
    ): IrValueParameter {
        return callablesGenerator.createIrParameter(
            valueParameter,
            useStubForDefaultValueStub,
            typeOrigin,
            skipDefaultParameter,
            forcedDefaultValueConversion
        ).also {
            localStorage.putParameter(valueParameter, it.symbol)
        }
    }

    // ------------------------------------ local delegated properties ------------------------------------

    fun findGetterOfProperty(propertySymbol: IrLocalDelegatedPropertySymbol): IrSimpleFunctionSymbol {
        return getterForPropertyCache.getValue(propertySymbol)
    }

    fun findSetterOfProperty(propertySymbol: IrLocalDelegatedPropertySymbol): IrSimpleFunctionSymbol? {
        return setterForPropertyCache[propertySymbol]
    }

    fun findDelegateVariableOfProperty(propertySymbol: IrLocalDelegatedPropertySymbol): IrVariableSymbol {
        return delegateVariableForPropertyCache.getValue(propertySymbol)
    }

    fun createAndCacheIrLocalDelegatedProperty(
        property: FirProperty,
        irParent: IrDeclarationParent
    ): IrLocalDelegatedProperty {
        val symbols = createLocalDelegatedPropertySymbols(property)
        val irProperty = callablesGenerator.createIrLocalDelegatedProperty(property, irParent, symbols)
        val symbol = irProperty.symbol
        delegateVariableForPropertyCache[symbol] = irProperty.delegate.symbol
        getterForPropertyCache[symbol] = irProperty.getter.symbol
        irProperty.setter?.let { setterForPropertyCache[symbol] = it.symbol }
        localStorage.putDelegatedProperty(property, symbol)
        return irProperty
    }

    private fun createLocalDelegatedPropertySymbols(property: FirProperty): LocalDelegatedPropertySymbols {
        val propertySymbol = IrLocalDelegatedPropertySymbolImpl()
        val getterSymbol = createFunctionSymbol()
        val setterSymbol = runIf(property.isVar) {
            createFunctionSymbol()
        }
        return LocalDelegatedPropertySymbols(propertySymbol, getterSymbol, setterSymbol)
    }

    // ------------------------------------ variables ------------------------------------

    fun createAndCacheIrVariable(
        variable: FirVariable,
        irParent: IrDeclarationParent,
        givenOrigin: IrDeclarationOrigin? = null
    ): IrVariable {
        return callablesGenerator.createIrVariable(variable, irParent, givenOrigin).also {
            localStorage.putVariable(variable, it.symbol)
        }
    }

    fun getIrValueSymbol(firVariableSymbol: FirVariableSymbol<*>): IrSymbol {
        return when (val firDeclaration = firVariableSymbol.fir) {
            is FirEnumEntry -> classifierStorage.getIrEnumEntrySymbol(firDeclaration)

            is FirValueParameter -> localStorage.getParameter(firDeclaration)
                ?: getIrVariableSymbol(firDeclaration) // catch parameter is FirValueParameter in FIR but IrVariable in IR

            else -> getIrVariableSymbol(firDeclaration)
        }
    }

    private fun getIrVariableSymbol(firVariable: FirVariable): IrVariableSymbol {
        return localStorage.getVariable(firVariable)
            ?: error("Cannot find variable ${firVariable.render()} in local storage")
    }

    // ------------------------------------ anonymous initializers ------------------------------------

    fun createIrAnonymousInitializer(
        anonymousInitializer: FirAnonymousInitializer,
        containingIrClass: IrClass,
    ): IrAnonymousInitializer {
        val irInitializer = callablesGenerator.createIrAnonymousInitializer(anonymousInitializer, containingIrClass)
        val alreadyContained = initializerCache.put(anonymousInitializer, irInitializer)
        require(alreadyContained == null) {
            "IR for anonymous initializer already exits: ${anonymousInitializer.render()}"
        }
        return irInitializer
    }

    fun getIrAnonymousInitializer(anonymousInitializer: FirAnonymousInitializer): IrAnonymousInitializer {
        return initializerCache.getValue(anonymousInitializer)
    }

    // ------------------------------------ callables ------------------------------------

    fun originalDeclarationForDelegated(irDeclaration: IrDeclaration): FirDeclaration? {
        return delegatedReverseCache[irDeclaration.symbol]
    }

    private fun FirCallableDeclaration.computeExternalOrigin(): IrDeclarationOrigin {
        val containingClass = containingClassLookupTag()?.toRegularClassSymbol(session)
        return when (containingClass?.isJavaOrEnhancement) {
            true -> IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB
            else -> IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB
        }
    }

    fun getIrFunctionSymbol(
        firFunctionSymbol: FirFunctionSymbol<*>,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag? = null,
        isLocal: Boolean = false
    ): IrFunctionSymbol {
        val function = firFunctionSymbol.fir

        if (function is FirConstructor) {
            return getIrConstructorSymbol(function.symbol)
        }

        getCachedIrFunctionSymbol(function, fakeOverrideOwnerLookupTag)?.let { return it }
        if (function is FirSimpleFunction && !isLocal) {
            val irParent = findIrParent(function, fakeOverrideOwnerLookupTag)
            if (irParent?.isExternalParent() == true) {
                val symbol = createMemberFunctionSymbol(function, fakeOverrideOwnerLookupTag, parentIsExternal = true)
                val firForLazyFunction = calculateFirForLazyDeclaration(
                    function, fakeOverrideOwnerLookupTag, lazyFakeOverrideGenerator::createFirFunctionFakeOverrideIfNeeded
                )
                // Return value is not used here, because creation of IR declaration binds it to the corresponding symbol
                // And all we want here is to bind symbol for lazy declaration
                callablesGenerator.createIrFunction(
                    firForLazyFunction,
                    irParent,
                    symbol,
                    predefinedOrigin = firForLazyFunction.computeExternalOrigin(),
                    isLocal = false,
                    fakeOverrideOwnerLookupTag,
                    allowLazyDeclarationsCreation = true
                ).also {
                    check(it is Fir2IrLazySimpleFunction)
                }
                cacheIrFunctionSymbol(function, symbol, fakeOverrideOwnerLookupTag)
                return symbol
            }
        }

        val symbol = createMemberFunctionSymbol(function, fakeOverrideOwnerLookupTag, parentIsExternal = false)
        cacheIrFunctionSymbol(function, symbol, fakeOverrideOwnerLookupTag)
        return symbol
    }

    private inline fun <reified FC : FirCallableDeclaration, reified IS : IrSymbol> getCachedIrCallableSymbol(
        declaration: FC,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag?,
        cacheGetter: (FC) -> IS?
    ): IS? {
        /*
         * There should be two types of declarations:
         * 1. Real declarations. They are stored in simple FirDeclaration -> IrDeclaration [cache]
         * 2. Fake overrides. They are stored in [irFakeOverridesForRealFirFakeOverrideMap], where the key is the original real declaration and
         *      specific dispatch receiver of particular fake override. This cache is needed, because we can have two different FIR
         *      f/o for common and platform modules (because they are session dependent), but we should create IR declaration for them
         *      only once. So [irFakeOverridesForFirFakeOverrideMap] is shared between fir2ir conversion for different MPP modules
         *      (see KT-58229)
         *
         * Unfortunately, in the current implementation, there is a special case.
         * If the fake override exists in FIR (i.e., it is an intersection or substitution override), and it comes from dependency module,
         * corresponding LazyIrFunction or LazyIrProperty can be created, ignoring the fact that it is a fake override.
         * In that case, it can sometimes be put to the wrong cache, as a normal declaration.
         *
         * To workaround this, we look up such declarations in both caches.
         */
        val isFakeOverride = declaration.isFakeOverrideOrDelegated(fakeOverrideOwnerLookupTag)
        if (isFakeOverride) {
            val key = FakeOverrideIdentifier(
                declaration.unwrapFakeOverridesOrDelegated().symbol,
                fakeOverrideOwnerLookupTag ?: declaration.containingClassLookupTag()!!,
            )
            irForFirSessionDependantDeclarationMap[key]?.let { return it as IS }
        } else {
            cacheGetter(declaration)?.let { return it }
        }

        return null
    }

    private inline fun <T : FirCallableDeclaration> calculateFirForLazyDeclaration(
        originalDeclaration: T,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag?,
        createFakeOverrideIfNeeded: (T, ConeClassLikeLookupTag) -> T?
    ): T {
        if (fakeOverrideOwnerLookupTag == null) return originalDeclaration
        return createFakeOverrideIfNeeded(originalDeclaration, fakeOverrideOwnerLookupTag) ?: originalDeclaration
    }

    // ------------------------------------ binding unbound symbols ------------------------------------

    /**
     * This function iterates over all non f/o callable symbols created in declaration storage and binds all unbound symbols
     *
     * Usually all symbols are bound after fir2ir conversion is over, but it's not true for `allowNonCachedDeclarations`, when
     *   we convert to IR only part of sources from code fragments.
     *
     * ```
     * // Original code
     * fun foo(x: Int) {} // (1)
     *
     * fun bar() {
     *     1.let { // (2)
     *         <context of code fragment>
     *     }
     * }
     *
     * // Code fragment
     * foo(this@let)
     *
     * Here in the body of the code fragment we reference function (1) and lambda (2), which leads to creation of their symbols,
     *   but not to generation of their IR. And since the original code won't be processed by fir2ir, we need to manually create
     *   IR for all symbols from it, to avoid publication of unbound symbols after fir2ri conversion is over
     *
     * Note that in the code fragment we may capture even local functions and lambdas, which are stored not in global caches,
     *   but in `localStorage`, which is getting cleared after leaving from corresponding scope. So to generate IR for them we need
     *   to call this function not only after fir2ir conversion, but also after leaving each local scope (see `leaveScope` function)
     */
    @LeakedDeclarationCaches
    internal fun fillUnboundSymbols() {
        fillUnboundSymbols(functionCache)
        fillUnboundSymbols(propertyCache.normal)
        fillUnboundSymbols(propertyCache.synthetic.mapKeys { it.key.originalFunction })
    }

    @LeakedDeclarationCaches
    private fun fillUnboundSymbols(cache: Map<out FirCallableDeclaration, IrSymbol>) {
        for ((firDeclaration, irSymbol) in cache) {
            if (irSymbol.isBound) continue
            // To generate a declaration, we should assure its signature resolve is over here (KT-70856)
            firDeclaration.lazyResolveToPhase(FirResolvePhase.ANNOTATION_ARGUMENTS)
            generateDeclaration(firDeclaration.symbol)
        }
    }

    private fun generateDeclaration(originalSymbol: FirBasedSymbol<*>) {
        val irParent = findIrParent(
            originalSymbol.packageFqName(),
            originalSymbol.getContainingClassSymbol()?.toLookupTag(),
            originalSymbol,
            originalSymbol.origin
        )
        when (originalSymbol) {
            is FirPropertySymbol -> createAndCacheIrProperty(
                originalSymbol.fir,
                irParent,
                fakeOverrideOwnerLookupTag = null
            )

            is FirNamedFunctionSymbol -> createAndCacheIrFunction(
                originalSymbol.fir,
                irParent,
                predefinedOrigin = FILLED_FOR_UNBOUND_SYMBOL,
                fakeOverrideOwnerLookupTag = null
            )

            else -> error("Unexpected declaration: $originalSymbol")
        }
    }

    // ------------------------------------ scripts ------------------------------------

    fun getCachedIrScript(script: FirScript): IrScript? {
        return scriptCache[script]
    }

    fun createIrScript(script: FirScript): IrScript {
        getCachedIrScript(script)?.let { error("IrScript already created: ${script.render()}") }
        val symbol = IrScriptSymbolImpl()
        return callablesGenerator.createIrScript(script, symbol).also {
            scriptCache[script] = it
        }
    }

    // ------------------------------------ REPL snippets ------------------------------------

    fun getCachedIrReplSnippet(snippet: FirReplSnippet): IrReplSnippet? {
        return replSnippetCache[snippet]
    }

    fun createIrReplSnippet(snippet: FirReplSnippet): IrReplSnippet {
        getCachedIrReplSnippet(snippet)?.let { error("IrReplSnippet already created: ${snippet.render()}") }
        val symbol = IrReplSnippetSymbolImpl()
        return callablesGenerator.createIrReplSnippet(snippet, symbol).also {
            replSnippetCache[snippet] = it
        }
    }

    // ------------------------------------ scoping ------------------------------------

    fun enterScope(symbol: IrSymbol) {
        if (symbol is IrSimpleFunctionSymbol ||
            symbol is IrConstructorSymbol ||
            symbol is IrAnonymousInitializerSymbol ||
            symbol is IrPropertySymbol ||
            symbol is IrEnumEntrySymbol ||
            symbol is IrScriptSymbol ||
            symbol is IrReplSnippetSymbol
        ) {
            localStorage.enterCallable()
        }
    }

    fun leaveScope(symbol: IrSymbol) {
        if (symbol is IrSimpleFunctionSymbol ||
            symbol is IrConstructorSymbol ||
            symbol is IrAnonymousInitializerSymbol ||
            symbol is IrPropertySymbol ||
            symbol is IrEnumEntrySymbol ||
            symbol is IrScriptSymbol ||
            symbol is IrReplSnippetSymbol
        ) {
            @OptIn(LeakedDeclarationCaches::class)
            if (configuration.allowNonCachedDeclarations) {
                // See KDoc to `fillUnboundSymbols` function
                fillUnboundSymbols(localStorage.lastCache.localFunctions)
                extensions.preserveLocalScope(symbol, localStorage.lastCache)
            }
            localStorage.leaveCallable()
        }
    }

    inline fun withScope(symbol: IrSymbol, crossinline block: () -> Unit) {
        enterScope(symbol)
        block()
        leaveScope(symbol)
    }

    // ------------------------------------ utilities ------------------------------------

    internal fun findIrParent(
        packageFqName: FqName,
        parentLookupTag: ConeClassLikeLookupTag?,
        firBasedSymbol: FirBasedSymbol<*>,
        firOrigin: FirDeclarationOrigin
    ): IrDeclarationParent? {
        if (parentLookupTag != null) {
            // At this point all source classes should be already created and bound to symbols
            @OptIn(UnsafeDuringIrConstructionAPI::class)
            return classifierStorage.getIrClassSymbol(parentLookupTag)?.owner
        }


        // TODO: All classes from BUILT_INS_PACKAGE_FQ_NAMES are considered built-ins now,
        // which is not exact and can lead to some problems
        val parentPackage = getIrExternalOrBuiltInsPackageFragment(
            packageFqName, firBasedSymbol.moduleData, firOrigin,
            allowBuiltins = firBasedSymbol !is FirCallableSymbol<*>
        )

        /**
         * In `allowNonCachedDeclarations` mode there is a situation possible when we get source declaration
         *   from session which is different from one which we convert right now. So we need to take an original firProvider
         *   for this declaration to correctly find containig file and properly generate NonCachedSourceFileFacadeClass if needed
         */
        val firProvider = if (configuration.allowNonCachedDeclarations) {
            when {
                firBasedSymbol.moduleData == session.moduleData -> c.firProvider
                else -> firBasedSymbol.moduleData.session.firProvider
            }
        } else {
            c.firProvider
        }

        val containerFile = when (firBasedSymbol) {
            is FirCallableSymbol -> firProvider.getFirCallableContainerFile(firBasedSymbol)
            is FirClassLikeSymbol -> firProvider.getFirClassifierContainerFileIfAny(firBasedSymbol)
            else -> error("Unknown symbol: $firBasedSymbol")
        }

        if (containerFile != null) {
            val existingFile = fileCache[containerFile]
            if (existingFile != null) {
                return existingFile
            }

            // Sudden declarations do not go through IR lowering process,
            // so the parent file isn't replaced with a facade class, as in 'FileClassLowering'.
            if (configuration.allowNonCachedDeclarations && firBasedSymbol is FirCallableSymbol<*>) {
                val psiFile = containerFile.psi?.containingFile
                if (psiFile is KtFile) {
                    val fileClassInfo = JvmFileClassUtil.getFileClassInfoNoResolve(psiFile)
                    val className = JvmClassName.byFqNameWithoutInnerClasses(fileClassInfo.fileClassFqName)

                    val facadeClassName: JvmClassName?
                    val declarationOrigin: IrDeclarationOrigin

                    if (fileClassInfo.withJvmMultifileClass) {
                        facadeClassName = JvmClassName.byFqNameWithoutInnerClasses(fileClassInfo.facadeClassFqName)
                        declarationOrigin = IrDeclarationOrigin.JVM_MULTIFILE_CLASS
                    } else {
                        facadeClassName = null
                        declarationOrigin = IrDeclarationOrigin.FILE_CLASS
                    }

                    val facadeShortName = className.fqNameForClassNameWithoutDollars.shortName()
                    val containerSource = NonCachedSourceFacadeContainerSource(className, facadeClassName)
                    return IrFactoryImpl.createClass(
                        startOffset = UNDEFINED_OFFSET,
                        endOffset = UNDEFINED_OFFSET,
                        origin = declarationOrigin,
                        symbol = IrClassSymbolImpl(),
                        name = facadeShortName,
                        kind = ClassKind.CLASS,
                        visibility = DescriptorVisibilities.PUBLIC,
                        modality = Modality.FINAL,
                        source = containerSource
                    ).apply {
                        parent = parentPackage
                        createThisReceiverParameter()
                        this.isNonCachedSourceFileFacade = true
                    }
                }
            }
        }

        return parentPackage
    }

    internal fun findIrParent(
        callableDeclaration: FirCallableDeclaration,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag?,
    ): IrDeclarationParent? {
        val firBasedSymbol = callableDeclaration.symbol
        val callableId = firBasedSymbol.callableId
        val callableOrigin = callableDeclaration.origin
        val parentLookupTag = fakeOverrideOwnerLookupTag ?: callableDeclaration.containingClassLookupTag()
        return findIrParent(
            callableId!!.packageName,
            parentLookupTag,
            firBasedSymbol,
            callableOrigin
        )
    }

    companion object {
        internal val ENUM_SYNTHETIC_NAMES = mapOf(
            Name.identifier("values") to IrSyntheticBodyKind.ENUM_VALUES,
            Name.identifier("valueOf") to IrSyntheticBodyKind.ENUM_VALUEOF,
            Name.identifier("entries") to IrSyntheticBodyKind.ENUM_ENTRIES,
            Name.special("<get-entries>") to IrSyntheticBodyKind.ENUM_ENTRIES
        )
    }
}

private fun FirCallableDeclaration.isFakeOverrideOrDelegated(fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag?): Boolean {
    if (isCopyCreatedInScope) return true
    return isFakeOverrideImpl(fakeOverrideOwnerLookupTag)
}

private fun FirCallableDeclaration.isFakeOverride(fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag?): Boolean {
    if (isSubstitutionOrIntersectionOverride) return true
    return isFakeOverrideImpl(fakeOverrideOwnerLookupTag)
}

private fun FirCallableDeclaration.isFakeOverrideImpl(fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag?): Boolean {
    if (fakeOverrideOwnerLookupTag == null) return false
    // this condition is true for all places when we are trying to create "fake" fake overrides in IR
    // "fake" fake overrides are f/o which are presented in IR but have no corresponding FIR f/o
    return fakeOverrideOwnerLookupTag != containingClassLookupTag()
}

private object IsStubPropertyForPureFieldKey : FirDeclarationDataKey()

internal var FirProperty.isStubPropertyForPureField: Boolean? by FirDeclarationDataRegistry.data(IsStubPropertyForPureFieldKey)

internal var IrClass.isNonCachedSourceFileFacade: Boolean by irFlag(copyByDefault = false)

/**
 * Opt-in to this annotation indicates that some code uses annotated function but it actually shouldn't
 * See KT-61513
 */
@RequiresOptIn
annotation class LeakedDeclarationCaches

/**
 * This function is introduced as preparation to publishing unbound symbols in fir2ir
 * There is a probability that it won't be non needed in future, but for now it allows
 *   to easily track all places left when we need to extract owner from symbol
 */
@UnsafeDuringIrConstructionAPI
internal fun <D : IrDeclaration> IrBindableSymbol<*, D>.ownerIfBound(): D? {
    return runIf(isBound) { owner }
}

data class PropertySymbols(
    val propertySymbol: IrPropertySymbol,
    val getterSymbol: IrSimpleFunctionSymbol?,
    val setterSymbol: IrSimpleFunctionSymbol?,
    val backingFieldSymbol: IrFieldSymbol?,
)

data class LocalDelegatedPropertySymbols(
    val propertySymbol: IrLocalDelegatedPropertySymbol,
    val getterSymbol: IrSimpleFunctionSymbol,
    val setterSymbol: IrSimpleFunctionSymbol?,
)
