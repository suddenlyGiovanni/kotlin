// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// FILE: KotlinFile.kt
fun foo(javaClass: JavaClass) {
    javaClass.a
    javaClass.<!UNRESOLVED_REFERENCE!>A<!>
}

// FILE: JavaClass.java
public class JavaClass {
    public int getA() { return 1; }
}

/* GENERATED_FIR_TAGS: functionDeclaration, javaProperty, javaType */
