// RUN_PIPELINE_TILL: BACKEND
fun foo(x: (String) -> Int) {}
fun foo(x: () -> Int) {}


fun bar(): Int = 1

fun main() {
    foo(::bar)
}

/* GENERATED_FIR_TAGS: callableReference, functionDeclaration, functionalType, integerLiteral */
