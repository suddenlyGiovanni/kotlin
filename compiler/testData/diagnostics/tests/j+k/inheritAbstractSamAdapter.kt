// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// FILE: A.java
public interface A {
    void foo(Runnable r);
}

// FILE: B.java
public interface B extends A {
    public void bar(Runnable r);
}

// FILE: test.kt
class C: B {
    override fun foo(r: Runnable?) {
    }

    override fun bar(r: Runnable?) {
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, javaType, nullableType, override */
