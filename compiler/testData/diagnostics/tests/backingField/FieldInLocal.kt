// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE
fun foo() {
    open class Local {
        val my: Int = 2
            get() = field
    }
    val your = object: Local() {
        val your: Int = 3
            get() = field
    }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, functionDeclaration, getter, integerLiteral,
localClass, localProperty, propertyDeclaration */
