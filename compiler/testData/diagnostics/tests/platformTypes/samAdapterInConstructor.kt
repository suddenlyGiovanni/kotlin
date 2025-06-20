// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_ANONYMOUS_PARAMETER

// FILE: A.java
import java.util.Comparator;

public class A<E> {
    public A(Comparator<? super E> comparator) {}
}

// FILE: main.kt

fun foo() {
    val result: A<String> = A<String> { x, y -> 1 }
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, inProjection, integerLiteral, javaFunction, javaType,
lambdaLiteral, localProperty, propertyDeclaration, samConversion */
