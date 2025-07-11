// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED
// LANGUAGE: +ContextReceivers

class A {
    val a = 1
}

class B {
    val b = 2
}

class C {
    val c = 3
}

context(A, B) fun C.f() {}

fun main(a: A, b: B, c: C) {
    with(a) {
        with(b) {
            with(c) {
                f()
            }
        }
    }
    with(b) {
        with(c) {
            with(a) {
                f()
            }
        }
    }
    with(a) {
        with(c) {
            <!NO_CONTEXT_ARGUMENT!>f<!>()
        }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionDeclarationWithContext,
integerLiteral, lambdaLiteral, propertyDeclaration */
