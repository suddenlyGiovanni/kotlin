// RUN_PIPELINE_TILL: BACKEND
// ALLOW_KOTLIN_PACKAGE

package kotlin

@Deprecated("foo test")
@DeprecatedSinceKotlin(warningSince = "1.0")
fun foo() {}

fun test() {
    <!DEPRECATION!>foo<!>()
}

/* GENERATED_FIR_TAGS: functionDeclaration, stringLiteral */
