// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
interface A

fun foo(invoke: A.()->Unit, a: A) {
    a.invoke()
}

fun bar(invoke: Any.()->Any, a: Any) {
    a.invoke()
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, interfaceDeclaration, typeWithExtension */
