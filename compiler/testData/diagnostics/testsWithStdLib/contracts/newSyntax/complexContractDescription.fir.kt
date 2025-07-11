// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContractSyntaxV2
// DIAGNOSTICS: -UNUSED_VARIABLE

import kotlin.contracts.*

fun foo(arg: Any?, num: Int?, block: () -> Unit) contract [
    returns() implies (arg is String),
    returns() implies (num != null),
    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
] {
    require(arg is String)
    require(num != null)
    block()
}

fun bar(arg: Any?, block: () -> Int): Boolean contract [
    returns(true) implies (arg != null),
    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
] {
    val num = block()
    if (arg != null) {
        return true
    }
    return false
}

/* GENERATED_FIR_TAGS: contractCallsEffect, contractConditionalEffect, contracts, equalityExpression,
functionDeclaration, functionalType, ifExpression, isExpression, localProperty, nullableType, propertyDeclaration */
