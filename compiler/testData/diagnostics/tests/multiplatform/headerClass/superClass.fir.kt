// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: m1-common
// FILE: common.kt

interface I
open class C
interface J

expect class Foo : I, C, J

<!EXPECT_ACTUAL_IR_INCOMPATIBILITY{JVM}!>expect<!> class Bar : <!SUPERTYPE_INITIALIZED_WITHOUT_PRIMARY_CONSTRUCTOR!>C<!><!SUPERTYPE_INITIALIZED_IN_EXPECTED_CLASS!>()<!>

expect class WithExplicitPrimaryConstructor() : C<!SUPERTYPE_INITIALIZED_IN_EXPECTED_CLASS!>()<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
actual class Foo : I, C(), J

actual class <!EXPECT_ACTUAL_INCOMPATIBLE_SUPERTYPES!>Bar<!>

actual class WithExplicitPrimaryConstructor : C()

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, interfaceDeclaration, primaryConstructor */
