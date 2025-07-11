// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
interface X {
    fun foo(a : Int = 1)
}

interface Y {
    fun foo(a : Int)
}

class Z : X, Y {
    override fun foo(a : Int) {}
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, interfaceDeclaration, override */
