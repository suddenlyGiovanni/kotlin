// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// FILE: aa/A.java
package aa;

public class A {
    public void f() { }
}

// FILE: B.kt
import aa.A

fun foo(a: A) = a.f()

/* GENERATED_FIR_TAGS: functionDeclaration, javaFunction, javaType */
