// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
enum class E {
    ENTRY;

    companion object {
        fun foo(): E = ENTRY
        fun bar(): Array<E> = values()
        fun baz(): E = valueOf("ENTRY")
        val valuez = values()
    }

    fun oof(): E = ENTRY
    fun rab(): Array<E> = values()
    fun zab(): E = valueOf("ENTRY")
}

fun foo() = E.ENTRY
fun bar() = E.values()
fun baz() = E.valueOf("ENTRY")

/* GENERATED_FIR_TAGS: companionObject, enumDeclaration, enumEntry, functionDeclaration, objectDeclaration,
propertyDeclaration, stringLiteral */
