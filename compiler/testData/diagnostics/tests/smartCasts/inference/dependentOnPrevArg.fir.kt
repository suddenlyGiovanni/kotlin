// RUN_PIPELINE_TILL: BACKEND
package a

fun <T> foo(u: T, v: T): T = u

fun test(s: String?) {
    val r: String = foo(s!!, s)
}

/* GENERATED_FIR_TAGS: checkNotNullCall, functionDeclaration, localProperty, nullableType, propertyDeclaration,
smartcast, typeParameter */
