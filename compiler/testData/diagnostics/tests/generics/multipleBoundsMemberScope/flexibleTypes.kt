// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// CHECK_TYPE
// FILE: A.java
public interface A {
    String foo();
}

// FILE: main.kt

interface B {
    fun foo(): String?
}

interface C {
    fun foo(): String
}

fun <T> test(x: T) where T : B, T : A, T : C {
    x.foo().checkType { _<String>() }
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType, infix,
interfaceDeclaration, javaType, lambdaLiteral, nullableType, typeConstraint, typeParameter, typeWithExtension */
