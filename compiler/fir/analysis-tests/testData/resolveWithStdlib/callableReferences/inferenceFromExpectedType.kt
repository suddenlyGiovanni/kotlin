// RUN_PIPELINE_TILL: BACKEND
fun foo(x: (String) -> Int) {}
fun foo2(x: (A, String) -> Int) {}

class A {
    fun <T, E> baz(x: T): E = null!!
}

fun <T, E> bar(x: T): E = null!!

fun main() {
    foo(::bar)
    foo(A()::baz)
    foo2(A::baz)
}

/* GENERATED_FIR_TAGS: callableReference, checkNotNullCall, classDeclaration, functionDeclaration, functionalType,
nullableType, typeParameter */
