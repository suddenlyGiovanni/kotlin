// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNNECESSARY_SAFE_CALL -SAFE_CALL_WILL_CHANGE_NULLABILITY

// MODULE: m1
// FILE: a.kt
package p

public interface Foo<T>

public interface B {
    public fun <T> foo(a: T): Foo<T>
}

// MODULE: m2(m1)
// FILE: b.kt
package p

public interface C : B {
    override fun <T> foo(a: T): Foo<T>

}

// MODULE: m3
// FILE: b.kt
package p

public interface Foo<T>

public interface B {
    public fun <T> foo(a: T): Foo<T>
}

// MODULE: m4(m3, m2)
// FILE: c.kt
import p.*

fun test(b: B?) {
    if (b !is C)  return
    b?.foo("")
}

fun test1(b: B?) {
    if (b !is C)  return
    b?.foo<String>("")
}

/* GENERATED_FIR_TAGS: functionDeclaration, ifExpression, interfaceDeclaration, isExpression, nullableType, override,
safeCall, smartcast, stringLiteral, typeParameter */
