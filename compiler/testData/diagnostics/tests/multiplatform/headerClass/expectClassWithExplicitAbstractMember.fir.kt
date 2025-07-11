// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: m1-common
// FILE: common.kt

interface Foo {
    fun foo()
}

<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect<!> class NonAbstractClass : Foo {
    <!ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS!>abstract<!> fun bar()

    <!ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS!>abstract<!> val baz: Int

    <!ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS!>abstract<!> override fun foo()
}

<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect<!> abstract class AbstractClass : Foo {
    abstract fun bar()

    abstract val baz: Int

    abstract override fun foo()
}

// MODULE: m1-jvm()()(m1-common)

/* GENERATED_FIR_TAGS: classDeclaration, expect, functionDeclaration, interfaceDeclaration, override,
propertyDeclaration */
