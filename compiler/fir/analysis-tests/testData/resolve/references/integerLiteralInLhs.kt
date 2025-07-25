// RUN_PIPELINE_TILL: BACKEND
fun Short.foo(): Int = 1
fun Int.foo(): Int = 2

fun testRef(f: () -> Int) {}

fun test() {
    // should resolve to Int.foo
    testRef(1::foo)
}

/* GENERATED_FIR_TAGS: callableReference, funWithExtensionReceiver, functionDeclaration, functionalType, integerLiteral */
