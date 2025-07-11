// RUN_PIPELINE_TILL: BACKEND
import Derived.foo

interface Base {
    fun foo() {}
}

object Derived : Base

fun test() {
    // See KT-35730
    foo() // Derived.foo is more correct here
}

/* GENERATED_FIR_TAGS: functionDeclaration, interfaceDeclaration, objectDeclaration */
