// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
open class E : <!CYCLIC_INHERITANCE_HIERARCHY!>E.EE<!>() {
    open class EE
}

/* GENERATED_FIR_TAGS: classDeclaration, nestedClass */
