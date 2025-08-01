/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.builtins.functions.AllowedToUsedOnlyInK1
import org.jetbrains.kotlin.types.TypeCheckerState.LowerCapturedTypePolicy.*
import org.jetbrains.kotlin.types.TypeCheckerState.SupertypesPolicy
import org.jetbrains.kotlin.types.model.*
import org.jetbrains.kotlin.util.context
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.kotlin.utils.SmartSet
import java.util.*

/**
 * Context that defines how type-checker operates, stores type-checker state,
 * created by [TypeCheckerProviderContext.newTypeCheckerState] in most cases
 *
 * Stateful and shouldn't be reused
 *
 * Once some type-checker operation is performed using a [TypeCheckerProviderContext], for example a [AbstractTypeChecker.isSubtypeOf],
 * new instance of particular [TypeCheckerState] should be created, with properly specified type system context
 *
 */
open class TypeCheckerState(
    val isErrorTypeEqualsToAnything: Boolean,
    val isStubTypeEqualsToAnything: Boolean,
    /**
     * Hacky flag related to overrides binding against Java [KT-74049](https://youtrack.jetbrains.com/issue/KT-74049).
     * Normally should be false. Its true state makes true the predicate that is normally false: `T & Any = T .. T?`.
     * This is required sometimes to avoid nullability warnings [KT-58933](https://youtrack.jetbrains.com/issue/KT-58933)
     * or even to compile the code [KT-73760](https://youtrack.jetbrains.com/issue/KT-73760) when T! and T & Any from Java are intersected.
     */
    val isDnnTypesEqualToFlexible: Boolean,
    val allowedTypeVariable: Boolean,
    val typeSystemContext: TypeSystemContext,
    val kotlinTypePreparator: AbstractTypePreparator,
    val kotlinTypeRefiner: AbstractTypeRefiner
) {
    @OptIn(TypeRefinement::class)
    fun refineType(type: KotlinTypeMarker): KotlinTypeMarker {
        return kotlinTypeRefiner.refineType(type)
    }

    fun prepareType(type: KotlinTypeMarker): KotlinTypeMarker {
        return kotlinTypePreparator.prepareType(type)
    }

    open fun customIsSubtypeOf(subType: KotlinTypeMarker, superType: KotlinTypeMarker): Boolean = true

    protected var argumentsDepth = 0

    internal inline fun <T> runWithArgumentsSettings(subArgument: KotlinTypeMarker, f: TypeCheckerState.() -> T): T {
        if (argumentsDepth > 100) {
            error("Arguments depth is too high. Some related argument: $subArgument")
        }

        argumentsDepth++
        val result = f()
        argumentsDepth--
        return result
    }

    open fun getLowerCapturedTypePolicy(subType: RigidTypeMarker, superType: CapturedTypeMarker): LowerCapturedTypePolicy =
        CHECK_SUBTYPE_AND_LOWER

    open fun addSubtypeConstraint(
        subType: KotlinTypeMarker,
        superType: KotlinTypeMarker,
        isFromNullabilityConstraint: Boolean = false
    ): Boolean? = null

    // Handling cases like A<Int> & A<T> <: A<F_var>
    // There are two possible solutions for F_var (Int and T) and both of them may work well or not with other constrains
    // Effectively, we need to fork constraint system to two copies: one with F_var=Int and the other with F_var=T
    // and then maintain them both until we find some contradiction with one of the versions.
    //
    // But that might lead to the exponential size of CS, thus we use the following heuristics:
    // we accumulate forks data until the last stage of the candidate resolution and then try to apply back then
    // until some of the constraints set has no contradiction.
    //
    // `atForkPoint` works trivially in non-inference context and for FE1.0: it just runs basic subtyping mechanism for each subTypeArguments
    // component until the first success
    open fun runForkingPoint(block: ForkPointContext.() -> Unit): Boolean = with(ForkPointContext.Default()) {
        block()
        result
    }

    interface ForkPointContext {
        fun fork(block: () -> Boolean)

        class Default : ForkPointContext {
            var result: Boolean = false
            override fun fork(block: () -> Boolean) {
                if (result) return
                result = block()
            }
        }
    }

    enum class LowerCapturedTypePolicy {
        CHECK_ONLY_LOWER,
        CHECK_SUBTYPE_AND_LOWER,
        SKIP_LOWER
    }

    private var supertypesLocked = false

    var supertypesDeque: ArrayDeque<RigidTypeMarker>? = null
        private set
    var supertypesSet: MutableSet<RigidTypeMarker>? = null
        private set


    fun initialize() {
        assert(!supertypesLocked) {
            "Supertypes were locked for ${this::class}"
        }
        supertypesLocked = true

        if (supertypesDeque == null) {
            supertypesDeque = ArrayDeque(4)
        }
        if (supertypesSet == null) {
            supertypesSet = SmartSet.create()
        }
    }

    fun clear() {
        supertypesDeque!!.clear()
        supertypesSet!!.clear()
        supertypesLocked = false
    }

    inline fun anySupertype(
        start: RigidTypeMarker,
        predicate: (RigidTypeMarker) -> Boolean,
        supertypesPolicy: (RigidTypeMarker) -> SupertypesPolicy
    ): Boolean {
        if (predicate(start)) return true

        initialize()

        val deque = supertypesDeque!!
        val visitedSupertypes = supertypesSet!!

        deque.push(start)
        while (deque.isNotEmpty()) {
            val current = deque.pop()
            if (!visitedSupertypes.add(current)) continue

            val policy = supertypesPolicy(current).takeIf { it != SupertypesPolicy.None } ?: continue
            val supertypes = with(typeSystemContext) { current.typeConstructor().supertypes() }
            for (supertype in supertypes) {
                val newType = policy.transformType(this, supertype)
                if (predicate(newType)) {
                    clear()
                    return true
                }
                deque.add(newType)
            }
        }

        clear()
        return false
    }

    sealed class SupertypesPolicy {
        abstract fun transformType(state: TypeCheckerState, type: KotlinTypeMarker): RigidTypeMarker

        object None : SupertypesPolicy() {
            override fun transformType(state: TypeCheckerState, type: KotlinTypeMarker) =
                throw UnsupportedOperationException("Should not be called")
        }

        object UpperIfFlexible : SupertypesPolicy() {
            override fun transformType(state: TypeCheckerState, type: KotlinTypeMarker) =
                with(state.typeSystemContext) { type.upperBoundIfFlexible() }
        }

        object LowerIfFlexible : SupertypesPolicy() {
            override fun transformType(state: TypeCheckerState, type: KotlinTypeMarker) =
                with(state.typeSystemContext) { type.lowerBoundIfFlexible() }
        }

        abstract class DoCustomTransform : SupertypesPolicy()
    }

    fun isAllowedTypeVariable(type: KotlinTypeMarker): Boolean {
        return allowedTypeVariable && with(typeSystemContext) { type.isTypeVariableType() }
    }
}

object AbstractTypeChecker {
    @JvmField
    var RUN_SLOW_ASSERTIONS = false

    fun prepareType(
        context: TypeCheckerProviderContext,
        type: KotlinTypeMarker,
        stubTypesEqualToAnything: Boolean = true
    ) = context.newTypeCheckerState(true, stubTypesEqualToAnything).prepareType(type)

    fun isSubtypeOf(
        context: TypeCheckerProviderContext,
        subType: KotlinTypeMarker,
        superType: KotlinTypeMarker,
        stubTypesEqualToAnything: Boolean = true
    ): Boolean {
        return isSubtypeOf(context.newTypeCheckerState(true, stubTypesEqualToAnything), subType, superType)
    }

    /**
     * It matches class types but ignores their type parameters
     *
     * Consider the following example:
     *
     * ```
     * abstract class Foo<T>
     * class FooBar : Foo<Any>()
     * ```
     *
     * In this case `isSubtypeOfClass` returns `true` for `FooBar` and `Foo<T>` input arguments
     * But `isSubtypeOf` returns `false` for the same input arguments
     */
    fun isSubtypeOfClass(
        state: TypeCheckerState,
        typeConstructor: TypeConstructorMarker,
        superConstructor: TypeConstructorMarker
    ): Boolean {
        return isSubtypeOfClass(state.typeSystemContext, typeConstructor, superConstructor)
    }

    fun isSubtypeOfClass(
        typeSystemContext: TypeSystemContext,
        typeConstructor: TypeConstructorMarker,
        superConstructor: TypeConstructorMarker,
    ): Boolean {
        if (typeConstructor == superConstructor) return true
        with(typeSystemContext) {
            for (superType in typeConstructor.supertypes()) {
                if (isSubtypeOfClass(typeSystemContext, superType.typeConstructor(), superConstructor)) {
                    return true
                }
            }
        }
        return false
    }

    fun equalTypes(
        context: TypeCheckerProviderContext,
        a: KotlinTypeMarker,
        b: KotlinTypeMarker,
        stubTypesEqualToAnything: Boolean = true,
        dnnTypesEqualToFlexible: Boolean = false,
    ): Boolean {
        return equalTypes(
            context.newTypeCheckerState(errorTypesEqualToAnything = false, stubTypesEqualToAnything, dnnTypesEqualToFlexible),
            a, b
        )
    }

    @JvmOverloads
    fun isSubtypeOf(
        state: TypeCheckerState,
        subType: KotlinTypeMarker,
        superType: KotlinTypeMarker,
        isFromNullabilityConstraint: Boolean = false
    ): Boolean {
        if (subType === superType) return true

        if (!state.customIsSubtypeOf(subType, superType)) return false

        return with(state) {
            with(state.typeSystemContext) {
                completeIsSubTypeOf(subType, superType, isFromNullabilityConstraint)
            }
        }
    }

    fun equalTypes(state: TypeCheckerState, a: KotlinTypeMarker, b: KotlinTypeMarker): Boolean =
        with(state.typeSystemContext) {
            if (a === b) return true

            if (a.isCommonDenotableType() && b.isCommonDenotableType()) {
                val refinedA = state.prepareType(state.refineType(a))
                val refinedB = state.prepareType(state.refineType(b))
                val simpleA = refinedA.lowerBoundIfFlexible()
                if (!areEqualTypeConstructors(refinedA.typeConstructor(), refinedB.typeConstructor())) return false
                if (simpleA.argumentsCount() == 0) {
                    if (refinedA.hasFlexibleNullability() || refinedB.hasFlexibleNullability()) return true

                    return simpleA.isMarkedNullable() == refinedB.lowerBoundIfFlexible().isMarkedNullable()
                }
            }

            return isSubtypeOf(state, a, b) && isSubtypeOf(state, b, a)
        }


    context(state: TypeCheckerState, c: TypeSystemContext)
    private fun completeIsSubTypeOf(
        subType: KotlinTypeMarker,
        superType: KotlinTypeMarker,
        isFromNullabilityConstraint: Boolean
    ): Boolean {
        val preparedSubType = state.prepareType(state.refineType(subType))
        val preparedSuperType = state.prepareType(state.refineType(superType))

        // With this flag (true for override matching), `T .. T? <: T & Any` is true (normally it's not so).
        // Together with `T & Any <: T .. T?` being satisfied via regular subtyping, we get `T .. T? = T & Any` for override matching.
        if (state.isDnnTypesEqualToFlexible && preparedSubType.isFlexible() && preparedSuperType.isDefinitelyNotNullType()) {
            return completeIsSubTypeOf(
                preparedSubType.asFlexibleType()!!.lowerBound(),
                preparedSuperType.asRigidType()!!.originalIfDefinitelyNotNullable(),
                isFromNullabilityConstraint
            )
        }

        checkSubtypeForSpecialCases(preparedSubType.lowerBoundIfFlexible(), preparedSuperType.upperBoundIfFlexible())?.let {
            state.addSubtypeConstraint(preparedSubType, preparedSuperType, isFromNullabilityConstraint)
            return it
        }

        // we should add constraints with flexible types, otherwise we never get flexible type as answer in constraint system
        state.addSubtypeConstraint(preparedSubType, preparedSuperType, isFromNullabilityConstraint)?.let { return it }

        return isSubtypeOfForSingleClassifierType(preparedSubType.lowerBoundIfFlexible(), preparedSuperType.upperBoundIfFlexible())
    }

    context(state: TypeCheckerState, c: TypeSystemContext)
    private fun checkSubtypeForIntegerLiteralType(
        subType: RigidTypeMarker,
        superType: RigidTypeMarker
    ): Boolean? {
        if (!subType.isIntegerLiteralType() && !superType.isIntegerLiteralType()) return null

        fun isTypeInIntegerLiteralType(integerLiteralType: RigidTypeMarker, type: RigidTypeMarker, checkSupertypes: Boolean): Boolean =
            integerLiteralType.possibleIntegerTypes().any { possibleType ->
                (possibleType.typeConstructor() == type.typeConstructor()) || (checkSupertypes && isSubtypeOf(state, type, possibleType))
            }

        fun isIntegerLiteralTypeInIntersectionComponents(type: RigidTypeMarker): Boolean {
            val typeConstructor = type.typeConstructor()

            return typeConstructor is IntersectionTypeConstructorMarker
                    && typeConstructor.supertypes().any { it.asRigidType()?.isIntegerLiteralType() == true }
        }

        fun isCapturedIntegerLiteralType(type: RigidTypeMarker): Boolean {
            if (type !is CapturedTypeMarker) return false
            val projection = type.typeConstructor().projection()
            return projection.getType()?.upperBoundIfFlexible()?.isIntegerLiteralType() == true
        }

        fun isIntegerLiteralTypeOrCapturedOne(type: RigidTypeMarker) = type.isIntegerLiteralType() || isCapturedIntegerLiteralType(type)

        when {
            isIntegerLiteralTypeOrCapturedOne(subType) && isIntegerLiteralTypeOrCapturedOne(superType) -> {
                return true
            }

            subType.isIntegerLiteralType() -> {
                if (isTypeInIntegerLiteralType(subType, superType, checkSupertypes = false)) {
                    return true
                }
            }

            superType.isIntegerLiteralType() -> {
                // Here we also have to check supertypes for intersection types: { Int & String } <: IntegerLiteralTypes
                if (isIntegerLiteralTypeInIntersectionComponents(subType)
                    || isTypeInIntegerLiteralType(superType, subType, checkSupertypes = true)
                ) {
                    return true
                }
            }
        }
        return null
    }

    context(state: TypeCheckerState, c: TypeSystemContext)
    private fun hasNothingSupertype(type: RigidTypeMarker): Boolean {
        val typeConstructor = type.typeConstructor()
        if (typeConstructor.isClassTypeConstructor()) {
            return typeConstructor.isNothingConstructor()
        }
        return state.anySupertype(type, { it.typeConstructor().isNothingConstructor() }) {
            if (it.isClassType()) {
                SupertypesPolicy.None
            } else {
                SupertypesPolicy.LowerIfFlexible
            }
        }
    }

    context(state: TypeCheckerState, c: TypeSystemContext)
    private fun isSubtypeOfForSingleClassifierType(
        subType: RigidTypeMarker,
        superType: RigidTypeMarker
    ): Boolean {
        if (RUN_SLOW_ASSERTIONS) {
            assert(subType.isSingleClassifierType() || subType.typeConstructor().isIntersection() || state.isAllowedTypeVariable(subType)) {
                "Not singleClassifierType and not intersection subType: $subType"
            }
            assert(superType.isSingleClassifierType() || state.isAllowedTypeVariable(superType)) {
                "Not singleClassifierType superType: $superType"
            }
        }

        if (!AbstractNullabilityChecker.isPossibleSubtype(state, subType, superType)) return false

        checkSubtypeForIntegerLiteralType(subType, superType)?.let {
            state.addSubtypeConstraint(subType, superType)
            return it
        }

        val superConstructor = superType.typeConstructor()

        if (c.areEqualTypeConstructors(subType.typeConstructor(), superConstructor) && superConstructor.parametersCount() == 0) return true
        if (superType.typeConstructor().isAnyConstructor()) return true

        val supertypesWithSameConstructor = with(findCorrespondingSupertypes(state, subType, superConstructor)) {
            // Note: in K1, we can have partially computed types here, like SomeType<NON COMPUTED YET>
            // (see e.g. interClassesRecursion.kt from diagnostic tests)
            // In this case we don't want to affect lazy computation in normal case (size <= 1), that's why we don't create a set
            // (adding to a hash set requires hash-code calculation for each set element)

            if (size > 1 && (state.typeSystemContext as? TypeSystemInferenceExtensionContext)?.isK2 == true) {
                // Here we want to filter out equivalent types to avoid unnecessary forking
                mapTo(mutableSetOf()) { state.prepareType(it).asRigidType() ?: it }
            } else {
                // TODO: drop this branch together with K1 code
                map { state.prepareType(it).asRigidType() ?: it }
            }
        }
        when (supertypesWithSameConstructor.size) {
            0 -> return hasNothingSupertype(subType) // todo Nothing & Array<Number> <: Array<String>
            1 -> return isSubtypeForSameConstructor(supertypesWithSameConstructor.first().asArgumentList(), superType)

            else -> { // at least 2 supertypes with same constructors. Such case is rare
                val newArguments = ArgumentList(superConstructor.parametersCount())
                var anyNonOutParameter = false
                for (index in 0 until superConstructor.parametersCount()) {
                    anyNonOutParameter = anyNonOutParameter || superConstructor.getParameter(index).getVariance() != TypeVariance.OUT
                    if (anyNonOutParameter) continue
                    val allProjections = supertypesWithSameConstructor.map {
                        it.getArgumentOrNull(index)?.takeIf { it.getVariance() == TypeVariance.INV }?.getType()
                            ?: error("Incorrect type: $it, subType: $subType, superType: $superType")
                    }

                    // todo discuss
                    val intersection = c.intersectTypes(allProjections).asTypeArgument()
                    newArguments.add(intersection)
                }

                if (!anyNonOutParameter && isSubtypeForSameConstructor(newArguments, superType)) return true

                return state.runForkingPoint {
                    for (subTypeArguments in supertypesWithSameConstructor) {
                        fork { isSubtypeForSameConstructor(subTypeArguments.asArgumentList(), superType) }
                    }
                }
            }
        }
    }

    context(context: TypeSystemContext)
    private fun isTypeVariableAgainstStarProjectionForSelfType(
        subArgumentType: KotlinTypeMarker,
        superArgumentType: KotlinTypeMarker,
        selfConstructor: TypeConstructorMarker
    ): Boolean {
        val simpleSubArgumentType = subArgumentType.asRigidType()

        if (simpleSubArgumentType !is CapturedTypeMarker || simpleSubArgumentType.isOldCapturedType()
            || !simpleSubArgumentType.typeConstructor().projection().isStarProjection()
        ) return false
        // Only 'for subtyping' captured types are approximated before adding constraints (see ConstraintInjector.addNewIncorporatedConstraint)
        // that can lead to adding problematic constraints like UPPER(Nothing) given by CapturedType(*) <: TypeVariable(A)
        if (simpleSubArgumentType.captureStatus() != CaptureStatus.FOR_SUBTYPING) return false

        val typeVariableConstructor = superArgumentType.typeConstructor() as? TypeVariableTypeConstructorMarker ?: return false

        return typeVariableConstructor.typeParameter?.hasRecursiveBounds(selfConstructor) == true
    }

    context(state: TypeCheckerState, c: TypeSystemContext)
    fun isSubtypeForSameConstructor(
        capturedSubArguments: TypeArgumentListMarker,
        superType: RigidTypeMarker
    ): Boolean {
        // No way to check, as no index sometimes
        //if (capturedSubArguments === superType.arguments) return true

        val superTypeConstructor = superType.typeConstructor()

        // Sometimes we can get two classes from different modules with different counts of type parameters
        // So for such situations we assume that those types are not subtype of each other
        val argumentsCount = capturedSubArguments.size()
        val parametersCount = superTypeConstructor.parametersCount()
        if (argumentsCount != parametersCount || argumentsCount != superType.argumentsCount()) {
            return false
        }

        for (index in 0 until parametersCount) {
            val superProjection = superType.getArgument(index) // todo error index

            val superArgumentType = superProjection.getType() ?: continue // A<B> <: A<*>
            val subArgumentType = capturedSubArguments[index].let {
                assert(it.getVariance() == TypeVariance.INV) { "Incorrect sub argument: $it" }
                // it.getVariance() == TypeVariance.INV means it's not a star projection
                it.getType()!!
            }

            val variance = effectiveVariance(superTypeConstructor.getParameter(index).getVariance(), superProjection.getVariance())
                ?: return state.isErrorTypeEqualsToAnything // todo exception?

            val isTypeVariableAgainstStarProjectionForSelfType = if (variance == TypeVariance.INV) {
                isTypeVariableAgainstStarProjectionForSelfType(subArgumentType, superArgumentType, superTypeConstructor) ||
                        isTypeVariableAgainstStarProjectionForSelfType(superArgumentType, subArgumentType, superTypeConstructor)
            } else false

            /*
             * We don't check subtyping between types like CapturedType(*) and TypeVariable(E) if the corresponding type parameter forms self type, for instance, Enum<E: Enum<E>>.
             * It can return false and produce unwanted constraints like UPPER(Nothing) (by CapturedType(*) <:> TypeVariable(E)) in the type inference context
             * due to approximation captured types.
             * Instead this type check we move on self-type level anyway: checking CapturedType(out Enum<*>) against TypeVariable(E).
             * This subtyping can already be successful and not add unwanted constraints in the type inference context.
             */
            if (isTypeVariableAgainstStarProjectionForSelfType)
                continue

            val correctArgument = state.runWithArgumentsSettings(subArgumentType) {
                when (variance) {
                    TypeVariance.INV -> equalTypes(state, subArgumentType, superArgumentType)
                    TypeVariance.OUT -> isSubtypeOf(state, subArgumentType, superArgumentType)
                    TypeVariance.IN -> isSubtypeOf(state, superArgumentType, subArgumentType)
                }
            }
            if (!correctArgument) return false
        }
        return true
    }

    @OptIn(ObsoleteTypeKind::class)
    context(context: TypeSystemContext)
    private fun KotlinTypeMarker.isCommonDenotableType(): Boolean =
        typeConstructor().isDenotable() &&
                !isDynamic() && !isDefinitelyNotNullType() && !isNotNullTypeParameter() &&
                !isFlexibleWithDifferentTypeConstructors()

    fun effectiveVariance(declared: TypeVariance, useSite: TypeVariance): TypeVariance? {
        if (declared == TypeVariance.INV) return useSite
        if (useSite == TypeVariance.INV) return declared

        // both not INVARIANT
        if (declared == useSite) return declared

        // composite In with Out
        return null
    }

    context(context: TypeSystemContext)
    private fun isStubTypeSubtypeOfAnother(a: RigidTypeMarker, b: RigidTypeMarker): Boolean {
        if (a.typeConstructor() !== b.typeConstructor()) return false
        if (!a.isDefinitelyNotNullType() && b.isDefinitelyNotNullType()) return false
        if (a.isMarkedNullable() && !b.isMarkedNullable()) return false

        return true // A!! == B!!, A? == B? or A == B
    }

    context(state: TypeCheckerState, c: TypeSystemContext)
    private fun checkSubtypeForSpecialCases(
        subType: RigidTypeMarker,
        superType: RigidTypeMarker
    ): Boolean? {
        if (subType.isError() || superType.isError()) {
            if (state.isErrorTypeEqualsToAnything) return true

            if (subType.isMarkedNullable() && !superType.isMarkedNullable()) return false

            return AbstractStrictEqualityTypeChecker.strictEqualTypes(
                c,
                subType.withNullability(false),
                superType.withNullability(false)
            )
        }

        if (subType.isStubTypeForBuilderInference() && superType.isStubTypeForBuilderInference())
            return isStubTypeSubtypeOfAnother(subType, superType) || state.isStubTypeEqualsToAnything

        if (subType.isStubType() || superType.isStubType())
            return state.isStubTypeEqualsToAnything

        // superType might be a definitely notNull type (see KT-42824)
        val superTypeCaptured = superType.asCapturedTypeUnwrappingDnn()
        val lowerType = superTypeCaptured?.lowerType()
        if (superTypeCaptured != null && lowerType != null) {
            // If superType is nullable, e.g., to check if Foo? a subtype of Captured<in Foo>?, we check the LHS, Foo?,
            // against the nullable version of the lower type of RHS. See KT-42825
            val nullableLowerType = if (superType.isMarkedNullable()) {
                lowerType.withNullability(true)
            } else {
                if (superType.isDefinitelyNotNullType()) lowerType.makeDefinitelyNotNullOrNotNull() else lowerType
            }
            when (state.getLowerCapturedTypePolicy(subType, superTypeCaptured)) {
                CHECK_ONLY_LOWER -> return isSubtypeOf(state, subType, nullableLowerType)
                CHECK_SUBTYPE_AND_LOWER -> if (isSubtypeOf(state, subType, nullableLowerType)) return true
                SKIP_LOWER -> Unit
            }
        }

        val superTypeConstructor = superType.typeConstructor()
        if (superTypeConstructor.isIntersection()) {
            assert(!superType.isMarkedNullable()) { "Intersection type should not be marked nullable!: $superType" }

            return superTypeConstructor.supertypes().all { isSubtypeOf(state, subType, it) }
        }

        /*
         * We handle cases like CapturedType(out Bar) <: Foo<CapturedType(out Bar)> separately here.
         * If Foo is a self type i.g. Foo<E: Foo<E>>, then argument for E will certainly be subtype of Foo<same_argument_for_E>,
         * so if CapturedType(out Bar) is the same as a type of Foo's argument and Foo is a self type, then subtyping should return true.
         * If we don't handle this case separately, subtyping may not converge due to the nature of the capturing.
         */
        val subTypeConstructor = subType.typeConstructor()
        if (subType is CapturedTypeMarker
            || (subTypeConstructor.isIntersection() && subTypeConstructor.supertypes().all { it is CapturedTypeMarker })
        ) {
            val typeParameter =
                getTypeParameterForArgumentInBaseIfItEqualToTarget(baseType = superType, targetType = subType)
            if (typeParameter != null && typeParameter.hasRecursiveBounds(superType.typeConstructor())) {
                return true
            }
        }

        return null
    }

    context(context: TypeSystemContext)
    private fun getTypeParameterForArgumentInBaseIfItEqualToTarget(
        baseType: KotlinTypeMarker,
        targetType: KotlinTypeMarker
    ): TypeParameterMarker? {
        for (i in 0 until baseType.argumentsCount()) {
            val typeArgument = baseType.getArgument(i).takeIf { !it.isStarProjection() }?.getType() ?: continue
            val areBothTypesCaptured = typeArgument.lowerBoundIfFlexible().isCapturedType() &&
                    targetType.lowerBoundIfFlexible().isCapturedType()

            if (typeArgument == targetType || (areBothTypesCaptured && typeArgument.typeConstructor() == targetType.typeConstructor())) {
                return baseType.typeConstructor().getParameter(i)
            }

            getTypeParameterForArgumentInBaseIfItEqualToTarget(typeArgument, targetType)?.let { return it }
        }

        return null
    }

    context(state: TypeCheckerState, c: TypeSystemContext)
    private fun collectAllSupertypesWithGivenTypeConstructor(
        subType: RigidTypeMarker,
        superConstructor: TypeConstructorMarker
    ): List<RigidTypeMarker> {
        subType.fastCorrespondingSupertypes(superConstructor)?.let {
            return it
        }

        if (!superConstructor.isClassTypeConstructor() && subType.isClassType()) return emptyList()

        if (superConstructor.isCommonFinalClassConstructor()) {
            return if (c.areEqualTypeConstructors(subType.typeConstructor(), superConstructor))
                listOf(c.captureFromArguments(subType, CaptureStatus.FOR_SUBTYPING) ?: subType)
            else
                emptyList()
        }

        val result: MutableList<RigidTypeMarker> = SmartList()

        state.anySupertype(subType, { false }) {

            val current = c.captureFromArguments(it, CaptureStatus.FOR_SUBTYPING) ?: it

            when {
                c.areEqualTypeConstructors(current.typeConstructor(), superConstructor) -> {
                    result.add(current)
                    SupertypesPolicy.None
                }
                current.argumentsCount() == 0 -> {
                    SupertypesPolicy.LowerIfFlexible
                }
                else -> {
                    state.typeSystemContext.substitutionSupertypePolicy(current)
                }
            }
        }

        return result
    }

    context(state: TypeCheckerState, c: TypeSystemContext)
    private fun collectAndFilter(
        classType: RigidTypeMarker,
        constructor: TypeConstructorMarker
    ) =
        selectOnlyPureKotlinSupertypes(collectAllSupertypesWithGivenTypeConstructor(classType, constructor))


    /**
     * If we have several paths to some interface, we should prefer pure kotlin path.
     * Example:
     *
     * class MyList : AbstractList<String>(), MutableList<String>
     *
     * We should see `String` in `get` function and others, also MyList is not subtype of MutableList<String?>
     *
     * More tests: javaAndKotlinSuperType & purelyImplementedCollection folder
     */
    context(c: TypeSystemContext)
    private fun selectOnlyPureKotlinSupertypes(
        supertypes: List<RigidTypeMarker>
    ): List<RigidTypeMarker> {
        if (supertypes.size < 2) return supertypes

        val allPureSupertypes = supertypes.filter {
            it.asArgumentList().all(c) { it.getType()?.asFlexibleType() == null }
        }
        return if (allPureSupertypes.isNotEmpty()) allPureSupertypes else supertypes
    }

    fun findCorrespondingSupertypes(
        state: TypeCheckerState,
        subType: RigidTypeMarker,
        superConstructor: TypeConstructorMarker,
    ): List<RigidTypeMarker> = context(state, state.typeSystemContext) {
        findCorrespondingSupertypes(subType, superConstructor)
    }

    // nullability was checked earlier via nullabilityChecker
    // should be used only if you really sure that it is correct
    context(state: TypeCheckerState, c: TypeSystemContext)
    fun findCorrespondingSupertypes(
        subType: RigidTypeMarker,
        superConstructor: TypeConstructorMarker
    ): List<RigidTypeMarker> {
        if (subType.isClassType()) {
            return collectAndFilter(subType, superConstructor)
        }

        // i.e. superType is not a classType
        if (!superConstructor.isClassTypeConstructor() && !superConstructor.isIntegerLiteralTypeConstructor()) {
            return collectAllSupertypesWithGivenTypeConstructor(subType, superConstructor)
        }

        // todo add tests
        val classTypeSupertypes = SmartList<RigidTypeMarker>()
        state.anySupertype(subType, { false }) {
            if (it.isClassType()) {
                classTypeSupertypes.add(it)
                SupertypesPolicy.None
            } else {
                SupertypesPolicy.LowerIfFlexible
            }
        }

        return classTypeSupertypes.flatMap { collectAndFilter(it, superConstructor) }
    }
}


object AbstractNullabilityChecker {
    // this method checks only nullability
    fun isPossibleSubtype(state: TypeCheckerState, subType: RigidTypeMarker, superType: RigidTypeMarker): Boolean =
        runIsPossibleSubtype(state, subType, superType)

    fun isSubtypeOfAny(context: TypeCheckerProviderContext, type: KotlinTypeMarker): Boolean =
        isSubtypeOfAny(
            context.newTypeCheckerState(
                errorTypesEqualToAnything = false,
                stubTypesEqualToAnything = true
            ),
            type
        )

    fun isSubtypeOfAny(state: TypeCheckerState, type: KotlinTypeMarker): Boolean =
        with(state.typeSystemContext) {
            state.hasNotNullSupertype(type.lowerBoundIfFlexible(), SupertypesPolicy.LowerIfFlexible)
        }

    private fun runIsPossibleSubtype(state: TypeCheckerState, subType: RigidTypeMarker, superType: RigidTypeMarker): Boolean =
        with(state.typeSystemContext) {
            if (AbstractTypeChecker.RUN_SLOW_ASSERTIONS) {
                // it makes for case String? & Any <: String
                assert(
                    subType.isSingleClassifierType() || subType.typeConstructor().isIntersection() || state.isAllowedTypeVariable(
                        subType
                    )
                ) {
                    "Not singleClassifierType and not intersection subType: $subType"
                }
                assert(superType.isSingleClassifierType() || state.isAllowedTypeVariable(superType)) {
                    "Not singleClassifierType superType: $superType"
                }
            }

            // superType is actually nullable
            if (superType.isMarkedNullable()) return true

            // i.e. subType is definitely not null
            @OptIn(ObsoleteTypeKind::class)
            if (subType.isNotNullTypeParameter()) return true
            if (subType.isDefinitelyNotNullType()) return true

            // i.e. subType is captured type, projection of which is marked not-null
            @OptIn(AllowedToUsedOnlyInK1::class) // Here it's quite hard to check if we're in K2, but isProjectionNotNull is always false there
            if (subType is CapturedTypeMarker && subType.isProjectionNotNull()) return true

            // i.e. subType is not-nullable
            if (state.hasNotNullSupertype(subType, SupertypesPolicy.LowerIfFlexible)) return true

            // i.e. subType hasn't not-null supertype and isn't definitely not-null, but superType is definitely not-null
            if (superType.isDefinitelyNotNullType()) return false

            // i.e. subType hasn't not-null supertype, but superType has
            if (state.hasNotNullSupertype(superType, SupertypesPolicy.UpperIfFlexible)) return false

            // both superType and subType hasn't not-null supertype and are not definitely not null.

            /**
             * If we still don't know, it means, that superType is not classType, for example -- type parameter.
             *
             * For captured types with lower bound this function can give to you false result. Example:
             *  class A<T>, A<in Number> => \exist Q : Number <: Q. A<Q>
             *      isPossibleSubtype(Number, Q) = false.
             *      Such cases should be taken into account in [NewKotlinTypeChecker.isSubtypeOf] (same for intersection types)
             */

            // classType cannot have special type in supertype list
            if (subType.isClassType()) return false

            return hasPathByNotMarkedNullableNodes(state, subType, superType.typeConstructor())
        }

    fun TypeCheckerState.hasNotNullSupertype(type: RigidTypeMarker, supertypesPolicy: SupertypesPolicy) =
        with(typeSystemContext) {
            anySupertype(type, {
                (it.isClassType() && !it.isMarkedNullable()) || it.isDefinitelyNotNullType()
            }) {
                if (it.isMarkedNullable()) SupertypesPolicy.None else supertypesPolicy
            }
        }

    fun TypeCheckerProviderContext.hasPathByNotMarkedNullableNodes(start: RigidTypeMarker, end: TypeConstructorMarker) =
        hasPathByNotMarkedNullableNodes(
            newTypeCheckerState(errorTypesEqualToAnything = false, stubTypesEqualToAnything = true), start, end
        )

    fun hasPathByNotMarkedNullableNodes(state: TypeCheckerState, start: RigidTypeMarker, end: TypeConstructorMarker) =
        with(state.typeSystemContext) {
            state.anySupertype(
                start,
                { isApplicableAsEndNode(state, it, end) },
                { if (it.isMarkedNullable()) SupertypesPolicy.None else SupertypesPolicy.LowerIfFlexible }
            )
        }

    private fun isApplicableAsEndNode(state: TypeCheckerState, type: RigidTypeMarker, end: TypeConstructorMarker): Boolean =
        with(state.typeSystemContext) {
            if (type.isNothing()) return true
            if (type.isMarkedNullable()) return false

            if (state.isStubTypeEqualsToAnything && type.isStubType()) return true

            return areEqualTypeConstructors(type.typeConstructor(), end)
        }
}


object AbstractFlexibilityChecker {
    context(c: TypeSystemCommonSuperTypesContext)
    fun hasDifferentFlexibilityAtDepth(types: Collection<KotlinTypeMarker>): Boolean {
        if (types.isEmpty()) return false
        if (hasDifferentFlexibility(types)) return true

        for (i in 0 until types.first().argumentsCount()) {
            val typeArgumentForOtherTypes = types.mapNotNull {
                if (it.argumentsCount() > i) it.getArgument(i).getType() else null
            }

            if (hasDifferentFlexibilityAtDepth(typeArgumentForOtherTypes)) return true
        }

        return false
    }

    context(c: TypeSystemCommonSuperTypesContext)
    private fun hasDifferentFlexibility(types: Collection<KotlinTypeMarker>): Boolean {
        val firstType = types.first()
        if (types.all { it === firstType }) return false

        return !types.all { it.isFlexible() } && !types.all { !it.isFlexible() }
    }
}
