// RUN_PIPELINE_TILL: FRONTEND
fun foo(u : Unit) : Int = 1

fun test() : Int {
    foo(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>)
    val a : () -> Unit = {
        foo(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>)
    }
    return 1
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, integerLiteral, lambdaLiteral, localProperty,
propertyDeclaration */
