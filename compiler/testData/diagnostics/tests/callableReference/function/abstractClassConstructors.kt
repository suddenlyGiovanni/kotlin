// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_EXPRESSION
interface A
abstract class B
annotation class C
enum class D

fun main() {
    ::<!UNRESOLVED_REFERENCE!>A<!>
    ::<!CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS!>B<!>
    ::<!CALLABLE_REFERENCE_TO_ANNOTATION_CONSTRUCTOR!>C<!>   // KT-3465
    ::<!INVISIBLE_MEMBER!>D<!>
}

/* GENERATED_FIR_TAGS: annotationDeclaration, callableReference, classDeclaration, enumDeclaration, functionDeclaration,
interfaceDeclaration */
