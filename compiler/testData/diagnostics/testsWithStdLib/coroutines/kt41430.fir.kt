// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-41430, KT-47830

class A

fun test_1(list: List<Set<A>>) {
    list.flatMapTo(mutableSetOf()) { it }
}

fun test_2(list: List<Set<A>>) {
    sequence<A> {
        list.flatMapTo(mutableSetOf()) { it }
    }
}

fun test_3(list: List<Set<A>>) {
    sequence {
        list.flatMapTo(mutableSetOf()) { it }
        yield(A())
    }
}

fun test_4(list: List<Set<A>>) {
    sequence {
        yield(A())
        list.flatMapTo(mutableSetOf()) { it }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, lambdaLiteral */
