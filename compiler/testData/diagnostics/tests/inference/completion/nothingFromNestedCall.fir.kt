// RUN_PIPELINE_TILL: FRONTEND
// CHECK_TYPE
// DIAGNOSTICS: -UNUSED_PARAMETER -UNREACHABLE_CODE

class Inv<T>
class Out<out T>

fun <K> invOut(y: K?): Inv<Out<K>> = TODO()
fun <R> test(x: Inv<Out<R>>): R = TODO()

fun testNothing() {
    test(invOut(null)) checkType { _<Nothing>() }
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType, infix,
lambdaLiteral, nullableType, out, typeParameter, typeWithExtension */
