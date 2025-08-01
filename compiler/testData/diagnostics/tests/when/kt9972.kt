// RUN_PIPELINE_TILL: FRONTEND
/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-152
 * PRIMARY LINKS: expressions, when-expression -> paragraph 2 -> sentence 1
 * expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 1
 * expressions, when-expression -> paragraph 9 -> sentence 1
 * expressions, conditional-expression -> paragraph 4 -> sentence 1
 * expressions, conditional-expression -> paragraph 5 -> sentence 1
 */

fun test1(): Int {
    val x: String = if (true) <!TYPE_MISMATCH!>{
        when {
            true -> Any()
            else -> null
        }
    }<!> else ""
    return x.hashCode()
}

fun test2(): Int {
    val x: String = <!TYPE_MISMATCH!>when {
                        true -> Any()
                        else -> null
                    } ?: return 0<!>
    return x.hashCode()
}

/* GENERATED_FIR_TAGS: elvisExpression, functionDeclaration, ifExpression, integerLiteral, localProperty,
propertyDeclaration, stringLiteral, whenExpression */
