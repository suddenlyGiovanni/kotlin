// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty1

interface A
inline val <T : A> T.bla get() = 1

class B<T>
fun <K, V> B<K>.foo(p: KProperty1<in K, V>): B<V> = TODO()

fun <K, V> B<out K>.bar(p: KProperty1<out K, V>): B<V> = TODO()

fun <K, V> B<K>.baz(p: KProperty1<out K, V>): B<V> = TODO()

fun <K, V> B<K>.star(p: KProperty1<*, V>): B<V> = TODO()


fun <R : A> B<R>.test(){
    foo(A::bla)
    bar(A::bla)
    <!CANNOT_INFER_PARAMETER_TYPE!>baz<!>(A::<!INAPPLICABLE_CANDIDATE!>bla<!>)
    star(A::bla)
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, funWithExtensionReceiver, functionDeclaration, getter,
inProjection, integerLiteral, interfaceDeclaration, nullableType, outProjection, propertyDeclaration,
propertyWithExtensionReceiver, starProjection, typeConstraint, typeParameter */
