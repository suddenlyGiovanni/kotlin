// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class Ann1

@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class Ann2(val x: String)

fun bar() {}
fun bar(block: () -> Unit) {}

fun foo(y: IntArray) {
    @Ann1 bar()
    @Ann1 bar() { }
    @Ann1 bar { }

    @Ann2("") bar()
    @Ann2("") bar() { }
    @Ann2("") bar { }

    @Ann1 @Ann2("") bar()

    var x = 1

    @Ann1 ++x
    @Ann1 x++
    @Ann2("") ++x
    @Ann2("") x++
    @Ann1 @Ann2("") ++x
    @Ann1 @Ann2("") x++

    @Ann1 y[0]

    @Ann1 { x: Int -> x }
    @Ann1 { x: Int -> x }(1)
    @Ann1 object { fun foo() = 1 }
    @Ann1 object { fun foo() = 1 }.foo()

    @Ann1() (x * x)
    var z = 1
    <!ANNOTATIONS_ON_BLOCK_LEVEL_EXPRESSION_ON_THE_SAME_LINE!>@Ann1 x + z<!>

    <!ANNOTATIONS_ON_BLOCK_LEVEL_EXPRESSION_ON_THE_SAME_LINE!><!WRAPPED_LHS_IN_ASSIGNMENT_ERROR!>@Ann1 x<!> = x + 2<!>
    <!ANNOTATIONS_ON_BLOCK_LEVEL_EXPRESSION_ON_THE_SAME_LINE!>@Ann1 x <!UNRESOLVED_REFERENCE!>+=<!> z + 2<!>

    <!ANNOTATIONS_ON_BLOCK_LEVEL_EXPRESSION_ON_THE_SAME_LINE!>@Ann1 x + 6 * 2<!> > 0
    <!ANNOTATIONS_ON_BLOCK_LEVEL_EXPRESSION_ON_THE_SAME_LINE!>@Ann1 x * 6<!> + 2 > 0

    <!ANNOTATIONS_ON_BLOCK_LEVEL_EXPRESSION_ON_THE_SAME_LINE!>@Ann1 object { operator fun plus(x: Int) = 1 } + 1<!>
    <!ANNOTATIONS_ON_BLOCK_LEVEL_EXPRESSION_ON_THE_SAME_LINE!>@Ann1 object { operator fun plus(x: Int) = 1 } + 1 * 4<!> > 0

    <!ANNOTATIONS_ON_BLOCK_LEVEL_EXPRESSION_ON_THE_SAME_LINE!>@Ann1 x foo z + 8<!>

    1 + @Ann1 x
    1 + @Ann1 x * z + 8

    x foo @Ann1 z + 8
}

infix fun Int.foo(other: Int) = 1

/* GENERATED_FIR_TAGS: additiveExpression, annotationDeclaration, anonymousObjectExpression, assignment,
comparisonExpression, funWithExtensionReceiver, functionDeclaration, functionalType, incrementDecrementExpression, infix,
integerLiteral, lambdaLiteral, localProperty, multiplicativeExpression, operator, primaryConstructor,
propertyDeclaration, stringLiteral */
