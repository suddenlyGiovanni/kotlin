// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
fun nonlocals(b : Boolean) {
    a@{
        fun foo() {
            if (b) {
                <!RETURN_NOT_ALLOWED!>return@a<!> 1 // The label must be resolved, but an error should be reported for a non-local return
            }
        }

        return@a 5
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, ifExpression, integerLiteral, lambdaLiteral, localFunction */
