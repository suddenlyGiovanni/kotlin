// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

interface Foo
fun (Foo.() -> Unit).invoke(b : Foo.() -> Unit)  {}

object Z {
    infix fun add(b : Foo.() -> Unit) : Z = Z
}

val t2 = Z add <!ARGUMENT_TYPE_MISMATCH!>{ } <!TOO_MANY_ARGUMENTS!>{ }<!><!>

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, functionalType, infix, interfaceDeclaration,
lambdaLiteral, objectDeclaration, propertyDeclaration, typeWithExtension */
