// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// FILE: Java.java

import java.util.List;

class Java {
    public static List<Integer> get(List<Integer> o) { return o; }
}

// FILE: test.kt

import java.util.ArrayList

fun call(): List<Int> {
    // No errors should be here
    return Java.get(if (true) ArrayList<Int>() else listOf(0))
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, ifExpression, integerLiteral, javaFunction */
