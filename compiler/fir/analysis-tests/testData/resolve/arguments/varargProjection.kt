// RUN_PIPELINE_TILL: BACKEND
open class Base
class Sub : Base()

fun foo(vararg arg: Base) {}

fun bar(base: Array<Base>, sub: Array<Sub>) {
    foo(*base)
    foo(*sub)
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, vararg */
