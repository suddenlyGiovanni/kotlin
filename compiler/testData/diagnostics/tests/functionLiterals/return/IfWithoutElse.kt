// RUN_PIPELINE_TILL: BACKEND
val flag = true

// type of a was checked by txt
val a/*: () -> Any*/ = l@ {
    <!INVALID_IF_AS_EXPRESSION!>if<!> (flag) return@l 4
}

val b/*: () -> Int */ = l@ {
    if (flag) return@l 4
    5
}

val c/*: () -> Unit */ = l@ {
    if (flag) 4
}

/* GENERATED_FIR_TAGS: ifExpression, integerLiteral, lambdaLiteral, propertyDeclaration */
