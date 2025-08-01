// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER
class A<T>
class B<T>

fun <E> foo(b: B<in A<E>>) {}
fun <E> baz(b: B<out A<E>>) {}

// See KT-13950
fun bar(b: B<in A<out Number>>, bOut: B<out A<out Number>>, bOut2: B<out A<Number>>) {
    foo(b)
    foo<Number>(b)

    baz(<!TYPE_MISMATCH!>bOut<!>)
    baz<Number>(<!TYPE_MISMATCH!>bOut<!>)

    baz(bOut2)
    baz<Number>(bOut2)
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, inProjection, nullableType, outProjection, typeParameter */
