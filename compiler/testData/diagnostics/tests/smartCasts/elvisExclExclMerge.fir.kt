// RUN_PIPELINE_TILL: BACKEND
fun test(x: Any?): Any {
    val z = x ?: x!!
    // x is not null in both branches
    x.hashCode()
    return z
}

/* GENERATED_FIR_TAGS: checkNotNullCall, elvisExpression, functionDeclaration, localProperty, nullableType,
propertyDeclaration, smartcast */
