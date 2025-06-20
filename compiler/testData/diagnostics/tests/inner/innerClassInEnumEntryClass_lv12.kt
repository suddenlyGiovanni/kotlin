// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +InnerClassInEnumEntryClass -NestedClassesInEnumEntryShouldBeInner

enum class Enum {
    ENTRY_WITH_CLASS {
        inner class TestInner

        <!NESTED_CLASS_DEPRECATED!>class TestNested<!>

        <!NESTED_CLASS_DEPRECATED!>interface TestInterface<!>

        <!NESTED_CLASS_DEPRECATED!>object TestObject<!>

        <!NESTED_CLASS_DEPRECATED!>enum class TestEnumClass<!> {
            OTHER_ENTRY
        }

        <!WRONG_MODIFIER_CONTAINING_DECLARATION!>companion<!> object {}
    }
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry */
