// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// FILE: JavaInterface.java

public interface JavaInterface {
    void foo(int javaName);
}

// FILE: kotlin.kt

class SimpleSubclass : JavaInterface {
    override fun foo(kotlinName: Int) {}
}


interface SubtraitWithFakeOverride : JavaInterface

class Subclass : SubtraitWithFakeOverride {
    override fun foo(otherKotlinName: Int) {}
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, interfaceDeclaration, javaType, override */
