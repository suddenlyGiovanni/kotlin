// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-51796
// WITH_STDLIB

fun test(list: List<Any>?) {
    if (list?.isNullOrEmpty() == true) {
        return
    }
    list<!UNSAFE_CALL!>.<!>size // should be unsafe call
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, ifExpression, nullableType, safeCall */
