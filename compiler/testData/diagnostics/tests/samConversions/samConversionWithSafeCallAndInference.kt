// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// FULL_JDK
// SKIP_TXT
// ISSUE: KT-56663

import java.util.function.Supplier

class Panel(supplier: Supplier<String>?)

fun main(s: String?) {
    Panel(s?.let { { it } })
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, lambdaLiteral, nullableType, primaryConstructor, safeCall,
samConversion */
