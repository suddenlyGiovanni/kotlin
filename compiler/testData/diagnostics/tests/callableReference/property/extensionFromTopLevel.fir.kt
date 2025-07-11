// RUN_PIPELINE_TILL: FRONTEND
// CHECK_TYPE

import kotlin.reflect.*

val String.countCharacters: Int
    get() = length

var Int.meaning: Long
    get() = 42L
    set(value) {}

fun test() {
    val f = String::countCharacters

    checkSubtype<KProperty1<String, Int>>(f)
    checkSubtype<KMutableProperty1<String, Int>>(<!ARGUMENT_TYPE_MISMATCH!>f<!>)
    checkSubtype<Int>(f.get("abc"))
    f.<!UNRESOLVED_REFERENCE!>set<!>("abc", 0)

    val g = Int::meaning

    checkSubtype<KMutableProperty1<Int, Long>>(g)
    checkSubtype<Long>(g.get(0))
    g.set(1, 0L)
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, funWithExtensionReceiver, functionDeclaration,
functionalType, getter, infix, integerLiteral, localProperty, nullableType, propertyDeclaration,
propertyWithExtensionReceiver, setter, stringLiteral, typeParameter, typeWithExtension */
