// RUN_PIPELINE_TILL: FRONTEND
fun foo1(b: Boolean, c: Int) {
    if (b && <!CONDITION_TYPE_MISMATCH!>c<!>) {}
    if (b || <!CONDITION_TYPE_MISMATCH!>c<!>) {}
    if (<!CONDITION_TYPE_MISMATCH!>c<!> && b) {}
    if (<!CONDITION_TYPE_MISMATCH!>c<!> || b) {}
}

/* GENERATED_FIR_TAGS: andExpression, disjunctionExpression, functionDeclaration, ifExpression */
