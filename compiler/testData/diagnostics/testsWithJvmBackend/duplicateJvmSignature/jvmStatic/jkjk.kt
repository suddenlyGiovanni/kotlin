// FIR_IDENTICAL
// WITH_STDLIB
// DIAGNOSTICS: -UNUSED_PARAMETER

// FILE: A.java

public class A {
    public static void foo() {}
    public static void baz(String s) {}
}

// FILE: B.kt

open class B : A() {
}

// FILE: C.java

public class C extends B {
    public static void bar(int i) {}
}

// FILE: K.kt

open class K : C() {
    companion object {
        @JvmStatic
        <!ACCIDENTAL_OVERRIDE!>fun foo() {}<!>
        @JvmStatic
        fun foo(a: Any) {}
        @JvmStatic
        <!ACCIDENTAL_OVERRIDE!>fun bar(i: Int) {}<!>
        @JvmStatic
        fun bar(i: String) {}
        @JvmStatic
        fun baz(i: Int) {}
    }
}
