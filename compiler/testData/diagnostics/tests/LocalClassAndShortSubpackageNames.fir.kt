// RUN_PIPELINE_TILL: FRONTEND
// FILE: A.kt
package foo.bar

class X

// FILE: B.kt

package foo

fun f() {
    class Local1 {
        fun g() : <!UNRESOLVED_REFERENCE!>bar<!>.X? = null
    }
    class Local2 {
        fun g() : foo.bar.X? = null
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, localClass, nullableType */
