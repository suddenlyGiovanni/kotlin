// RUN_PIPELINE_TILL: FRONTEND
fun (() -> String).foo() {}
fun String.foo() {}

fun String.bar() {}

fun main1() {
    { "" }.foo()
    "".foo()
}

fun main2() {
    { "" }.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>bar<!>()
    "".bar()
}

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, functionalType, lambdaLiteral, stringLiteral */
