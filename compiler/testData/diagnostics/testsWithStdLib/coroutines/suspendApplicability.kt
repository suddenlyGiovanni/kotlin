// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER -NOTHING_TO_INLINE
import kotlin.coroutines.*

suspend fun notMember(q: Double) = 1

suspend fun String.wrongExtension(x: Any) = 1

suspend fun Controller.controllerReceiver() = 1

class Controller {
    // is still valid
    suspend fun oldConvention(x: Continuation<Int>) {

    }

    suspend fun noParameters() {

    }

    suspend fun oneParameter(q: Any) {}
    suspend fun varargParameter(vararg q: Any) {}
    suspend fun returnsString(q: Any) = ""

    inline suspend fun inlineFun(x: Int) {}

    suspend fun String.memberExtension() = 1
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, inline, integerLiteral,
stringLiteral, suspend, vararg */
