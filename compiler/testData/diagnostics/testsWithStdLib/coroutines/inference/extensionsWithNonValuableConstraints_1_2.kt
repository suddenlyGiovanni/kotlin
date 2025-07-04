// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// LANGUAGE: -ExperimentalBuilderInference
// DIAGNOSTICS: -UNUSED_PARAMETER

interface Base

interface Controller<T> : Base {
    suspend fun yield(t: T) {}
}

fun <S> generate(g: suspend Controller<S>.() -> Unit): S = TODO()

suspend fun Base.baseExtension() {}

val test1 = generate {
    yield("foo")
    baseExtension()
}

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, functionalType, interfaceDeclaration,
lambdaLiteral, nullableType, propertyDeclaration, stringLiteral, suspend, typeParameter, typeWithExtension */
