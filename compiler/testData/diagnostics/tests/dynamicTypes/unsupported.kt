// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
val foo: <!UNSUPPORTED!>dynamic<!> = 1

fun foo() {
    class C {
        val foo: <!UNSUPPORTED!>dynamic<!> = 1
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, flexibleType, functionDeclaration, integerLiteral, localClass,
propertyDeclaration */
