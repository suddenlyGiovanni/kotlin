// RUN_PIPELINE_TILL: FRONTEND
package f

fun test(a: Boolean, b: Boolean): Int {
    return if(a) {
        1
    } else <!TYPE_MISMATCH!>{
        <!INVALID_IF_AS_EXPRESSION!>if<!> (b) {
            3
        }
    }<!>
}

/* GENERATED_FIR_TAGS: functionDeclaration, ifExpression, integerLiteral */
