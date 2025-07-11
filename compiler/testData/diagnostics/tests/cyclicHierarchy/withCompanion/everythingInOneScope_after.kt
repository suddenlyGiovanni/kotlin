// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// LANGUAGE: +ProhibitVisibilityOfNestedClassifiersFromSupertypesOfCompanion
// see https://youtrack.jetbrains.com/issue/KT-21515

open class Container {
    open class Base {
        open fun m() {}
    }

    // note that Base() supertype will be resolved in scope that was created on recursion
    abstract class DerivedAbstract : Base()

    companion object : DerivedAbstract() {
        override fun m() {}
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, nestedClass, objectDeclaration, override */
