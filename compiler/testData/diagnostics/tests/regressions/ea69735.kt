// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
enum class MyEnum {
    // Here we have a problem 
    // while checking on a deprecated super constructor
    FIRST<!SYNTAX!><!SYNTAX!><!>:<!>
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry */
