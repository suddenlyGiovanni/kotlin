// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// ISSUE: KT-65235

class Scope {
    operator fun String.invoke(): Int = 1
}

fun scope(s: Scope.() -> Unit) {}

fun somewhere() {
    val x: String = listOf("a").first()

    scope {
        val y: String = listOf("a").first()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType, integerLiteral,
lambdaLiteral, localProperty, operator, propertyDeclaration, stringLiteral, typeWithExtension */
