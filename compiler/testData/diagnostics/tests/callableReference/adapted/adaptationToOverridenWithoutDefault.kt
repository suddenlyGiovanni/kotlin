// RUN_PIPELINE_TILL: BACKEND
// SKIP_TXT
// FIR_IDENTICAL

interface Some {
    fun foo(b: Boolean? = null): Int = 10
}

class SomeImpl : Some {
    override fun foo(b: Boolean?): Int {
        return 0
    }

    private fun buz() {
        bar(::foo)
    }
}

private fun buz() {
    bar(SomeImpl()::foo)
}

private fun <T> bar(actionForAll: () -> T) {
    actionForAll()
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, functionalType, integerLiteral,
interfaceDeclaration, nullableType, override, typeParameter */
