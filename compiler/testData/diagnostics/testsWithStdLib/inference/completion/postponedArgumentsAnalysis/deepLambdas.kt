// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

fun useList(list: List<Any?>) {  }
fun useMap(map: Map<String, Any?>) {  }

enum class Color {BLUE}

fun Color.grayValue() {}

fun test() {
    useList(listOf({ x: Color -> x.grayValue() }, Color.BLUE))
    useMap(mapOf("a" to { x: Color -> x.grayValue() }, "b" to Color.BLUE))
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, funWithExtensionReceiver, functionDeclaration, lambdaLiteral,
nullableType, stringLiteral */
