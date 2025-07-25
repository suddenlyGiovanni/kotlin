// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
enum class E {
    E1 {
        override fun foo() = outerFun() + super.outerFun()
    },
    E2 {
        override fun foo() = E1.foo()
    };
    
    abstract fun foo(): Int
    
    fun outerFun() = 42
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, functionDeclaration, integerLiteral */
