// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-40510

// FILE: foo/A.java
package foo;

public abstract class A {
    // package-private
    abstract void foo();
}

// FILE: main.kt
import foo.A

class DelegatedA(val a: A) : <!DELEGATION_NOT_TO_INTERFACE!>A<!> by a

/* GENERATED_FIR_TAGS: classDeclaration, inheritanceDelegation, javaType, primaryConstructor, propertyDeclaration */
