// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

@Target(AnnotationTarget.TYPE)
@DslMarker
annotation class MyDsl

interface A {
    fun foo()
}

interface B {
    fun bar()
}

fun baz1(x: (@MyDsl A).() -> Unit) {}
fun baz2(x: (@MyDsl B).() -> Unit) {}
fun baz3(x: @MyDsl A.() -> Unit) {}
fun baz4(x: @MyDsl B.() -> Unit) {}

fun @MyDsl A.baz5() {
    baz4 {
        bar()
        <!DSL_SCOPE_VIOLATION!>foo<!>()
    }
}

fun main() {
    baz1 {
        baz2 {
            bar()
            <!DSL_SCOPE_VIOLATION!>foo<!>()
        }
    }

    baz3 {
        baz2 {
            bar()
            <!DSL_SCOPE_VIOLATION!>foo<!>()
        }
    }

    baz1 {
        baz4 {
            bar()
            <!DSL_SCOPE_VIOLATION!>foo<!>()
        }
    }

    baz3 {
        baz4 {
            bar()
            <!DSL_SCOPE_VIOLATION!>foo<!>()
        }
    }

}

/* GENERATED_FIR_TAGS: annotationDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType,
interfaceDeclaration, lambdaLiteral, typeWithExtension */
