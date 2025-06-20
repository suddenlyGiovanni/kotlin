// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// ISSUE: KT-65584

fun <T> giveItName(it: T, block: (myName: T) -> Unit) = block(it)

fun <T> duplicateIt(it: T, block: (T, T) -> Unit) = block

class MyTriple<T, K, M>(val a: T, val b: K, val c: M)

fun test() {
    giveItName(10) {
        MyTriple(it, it, it).also { self -> }
        (duplicateIt(it) { a, b -> }).also { function -> }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionalType, integerLiteral, lambdaLiteral,
nullableType, primaryConstructor, propertyDeclaration, typeParameter */
