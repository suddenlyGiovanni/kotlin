// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// FILE: KotlinFile.kt
fun foo(javaClass: JavaClass) {
    javaClass.<!UNRESOLVED_REFERENCE!>something<!> = 1
}

// FILE: JavaClass.java
public class JavaClass {
    public void setSomething(int value) { }
}

/* GENERATED_FIR_TAGS: assignment, functionDeclaration, integerLiteral, javaType */
