// RUN_PIPELINE_TILL: BACKEND
// SKIP_JAVAC
// This directive is needed to skip this test in LazyBodyIsNotTouchedTilContractsPhaseTestGenerated,
//  because it fails to parse module structure of multimodule test

// MODULE: lib
package dependency

abstract class A<T> {
    fun foo() = ""
}

// MODULE: main(lib)
// FILE: B.kt
package main
import c.C
import dependency.A

class D {
    abstract class B : A<C>() {}
}

fun usage(b : D.B?) {
    if (b != null) {
        b.foo()
    }
}
// FILE: C.kt
package c
class C

/* GENERATED_FIR_TAGS: classDeclaration, equalityExpression, functionDeclaration, ifExpression, nestedClass,
nullableType, smartcast, stringLiteral, typeParameter */
