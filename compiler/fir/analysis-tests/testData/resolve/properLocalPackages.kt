// RUN_PIPELINE_TILL: FRONTEND
package a.b

class X {
    fun foo() {
        class U {
            inner class K {
                inner class D {
                    fun check() {
                        class F {
                            inner class L
                        }
                    }
                }
            }

            <!NESTED_CLASS_NOT_ALLOWED!>class T<!>
        }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, inner, localClass, nestedClass */
