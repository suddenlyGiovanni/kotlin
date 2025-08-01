// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -UNUSED_PARAMETER

private object TopLevelTypeVariable {
    @Suppress("INVISIBLE_MEMBER", <!ERROR_SUPPRESSION!>"INVISIBLE_REFERENCE"<!>, "HIDDEN")
    fun <T> foo(): @kotlin.internal.NoInfer T = TODO()

    fun <K> bar(k: K) {}

    fun test() {
        bar(foo<Int>())
    }
}

private object NestedTypeVariable {
    class Inv<T>

    @Suppress("INVISIBLE_MEMBER", <!ERROR_SUPPRESSION!>"INVISIBLE_REFERENCE"<!>, "HIDDEN")
    fun <T> foo(): Inv<@kotlin.internal.NoInfer T> = TODO()

    fun <K> bar(p: Inv<K>) {}

    fun test() {
        bar(foo<String>())
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nestedClass, nullableType, objectDeclaration,
stringLiteral, typeParameter */
