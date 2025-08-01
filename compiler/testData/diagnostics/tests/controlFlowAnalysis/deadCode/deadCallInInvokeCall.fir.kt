// RUN_PIPELINE_TILL: BACKEND
fun testInvoke() {
    operator fun Nothing.invoke(): Nothing = this
    todo()<!UNREACHABLE_CODE!>()<!>
}

fun testInvokeWithLambda() {
    operator fun Nothing.invoke(i: Int, f: () -> Int) = f
    todo()<!UNREACHABLE_CODE!>(1){ 42 }<!>
}

fun todo(): Nothing = throw Exception()

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, functionalType, integerLiteral, lambdaLiteral,
localFunction, operator, thisExpression */
