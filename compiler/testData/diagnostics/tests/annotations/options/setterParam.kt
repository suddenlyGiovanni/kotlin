// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// KT-9145

@Target(AnnotationTarget.CLASS)
annotation class Ann

var x: Int
    get() = 1
    set(<!WRONG_ANNOTATION_TARGET!>@Ann<!> <!WRONG_MODIFIER_TARGET!>private<!> x) { }

/* GENERATED_FIR_TAGS: annotationDeclaration, getter, integerLiteral, propertyDeclaration, setter */
