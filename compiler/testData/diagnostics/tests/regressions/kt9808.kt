// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// KT-9808 Extension function on object for new resolve
object O

val foo: O.() -> Unit  = null!!

fun test() {
    O.foo()
}

/* GENERATED_FIR_TAGS: checkNotNullCall, functionDeclaration, functionalType, objectDeclaration, propertyDeclaration,
typeWithExtension */
