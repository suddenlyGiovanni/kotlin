// RUN_PIPELINE_TILL: FRONTEND
// FILE: abc/A.java
package abc;
public class A {
    protected void foo(Runnable x) {}
}

// FILE: main.kt
import abc.A;

class Data(var x: A)

class B : A() {
    fun baz(a: A, b: B, d: Data) {
        a.<!INVISIBLE_MEMBER!>foo<!> { }

        b.foo { }

        if (a is B) {
            <!DEBUG_INFO_SMARTCAST!>a<!>.foo {}
        }

        if (d.x is B) {
            d.x.<!INVISIBLE_MEMBER!>foo<!> {}
        }
    }
}

fun baz(a: A) {
    a.<!INVISIBLE_MEMBER!>foo<!> { }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, ifExpression, isExpression, javaFunction, javaType,
lambdaLiteral, primaryConstructor, propertyDeclaration, samConversion, smartcast */
