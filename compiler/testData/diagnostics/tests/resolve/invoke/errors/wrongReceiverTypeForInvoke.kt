// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

fun String.invoke(i: Int) {}

fun foo(i: Int) {
    <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>i<!>(1)

    <!FUNCTION_EXPECTED!>1<!>(1)
}

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, integerLiteral */
