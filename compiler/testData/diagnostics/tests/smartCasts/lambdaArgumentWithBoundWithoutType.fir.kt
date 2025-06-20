// RUN_PIPELINE_TILL: FRONTEND
interface Foo
interface Bar : Foo

fun foo(): Foo? = null
fun bar(): Bar? = null

fun <T : Bar> run(f: () -> T): T = f()

val foo: Foo = run {
    val x = bar()
    if (x == null) throw Exception()
    x
}

val foofoo: Foo = run {
    val x = foo()
    if (x == null) throw Exception()
    <!RETURN_TYPE_MISMATCH!>x<!>
}

val bar: Bar = run {
    val x = foo()
    if (x == null) throw Exception()
    <!RETURN_TYPE_MISMATCH!>x<!>
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, functionalType, ifExpression, interfaceDeclaration,
lambdaLiteral, localProperty, nullableType, propertyDeclaration, smartcast, typeConstraint, typeParameter */
