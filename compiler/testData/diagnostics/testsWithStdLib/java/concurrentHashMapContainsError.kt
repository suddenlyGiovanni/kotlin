// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// FULL_JDK

class A : java.util.concurrent.ConcurrentHashMap<String, Int>() {
    operator fun contains(x: Char): Boolean = true
}
class B : java.util.concurrent.ConcurrentHashMap<String, Int>() {
    override fun contains(value: Any?): Boolean {
        return super.contains(value)
    }
}

class C : java.util.concurrent.ConcurrentHashMap<String, Int>() {
    operator override fun contains(value: Any?): Boolean {
        return super.contains(value)
    }
}

open class D<K, V> : java.util.concurrent.ConcurrentHashMap<K, V>()

class E : D<String, Int>()

fun main() {
    run { // CHM test
        val hm = java.util.concurrent.ConcurrentHashMap<String, Int>()
        "" <!CONCURRENT_HASH_MAP_CONTAINS_OPERATOR_ERROR!>in<!> hm
        "" <!CONCURRENT_HASH_MAP_CONTAINS_OPERATOR_ERROR!>!in<!> hm
        1 <!CONCURRENT_HASH_MAP_CONTAINS_OPERATOR_ERROR!>!in<!> hm
        2 <!CONCURRENT_HASH_MAP_CONTAINS_OPERATOR_ERROR!>in<!> hm

        hm.contains("")
        hm.contains(1)

        "" in (hm as Map<String, Int>)
        "" !in (hm as Map<String, Int>)
        1 <!TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR!>in<!> (hm as Map<String, Int>)
        1 <!TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR!>!in<!> (hm as Map<String, Int>)
    }

    run { // A : CHM test
        val a = A()
        "" <!CONCURRENT_HASH_MAP_CONTAINS_OPERATOR_ERROR!>in<!> a
        "" <!CONCURRENT_HASH_MAP_CONTAINS_OPERATOR_ERROR!>!in<!> a
        1 <!CONCURRENT_HASH_MAP_CONTAINS_OPERATOR_ERROR!>!in<!> a
        2 <!CONCURRENT_HASH_MAP_CONTAINS_OPERATOR_ERROR!>in<!> a

        ' ' in a
        ' ' !in a
        a.contains("")
        a.contains(1)

        "" in (a as Map<String, Int>)
        "" !in (a as Map<String, Int>)
        1 <!TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR!>in<!> (a as Map<String, Int>)
        1 <!TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR!>!in<!> (a as Map<String, Int>)
    }

    run { // B : CHM test
        val b = B()
        "" <!CONCURRENT_HASH_MAP_CONTAINS_OPERATOR_ERROR!>in<!> b
        "" <!CONCURRENT_HASH_MAP_CONTAINS_OPERATOR_ERROR!>!in<!> b
        1 <!CONCURRENT_HASH_MAP_CONTAINS_OPERATOR_ERROR!>!in<!> b
        2 <!CONCURRENT_HASH_MAP_CONTAINS_OPERATOR_ERROR!>in<!> b

        b.contains("")
        b.contains(1)

        "" in (b as Map<String, Int>)
        "" !in (b as Map<String, Int>)
        1 <!TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR!>in<!> (b as Map<String, Int>)
        1 <!TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR!>!in<!> (b as Map<String, Int>)
    }

    run { // C : CHM
        // Actually, we could've allow calls here because the owner explicitly declared as operator, but semantics is still weird
        val c = C()
        "" <!CONCURRENT_HASH_MAP_CONTAINS_OPERATOR_ERROR!>in<!> c
        "" <!CONCURRENT_HASH_MAP_CONTAINS_OPERATOR_ERROR!>!in<!> c
        1 <!CONCURRENT_HASH_MAP_CONTAINS_OPERATOR_ERROR!>!in<!> c
        2 <!CONCURRENT_HASH_MAP_CONTAINS_OPERATOR_ERROR!>in<!> c

        c.contains("")
        c.contains(1)

        "" in (c as Map<String, Int>)
        "" !in (c as Map<String, Int>)
        1 <!TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR!>in<!> (c as Map<String, Int>)
        1 <!TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR!>!in<!> (c as Map<String, Int>)
    }

    run { // D<K, V> : CHM<K, V>
        val d = D<String, Int>()
        "" <!CONCURRENT_HASH_MAP_CONTAINS_OPERATOR_ERROR!>in<!> d
        "" <!CONCURRENT_HASH_MAP_CONTAINS_OPERATOR_ERROR!>!in<!> d
        1 <!CONCURRENT_HASH_MAP_CONTAINS_OPERATOR_ERROR!>!in<!> d
        2 <!CONCURRENT_HASH_MAP_CONTAINS_OPERATOR_ERROR!>in<!> d

        d.contains("")
        d.contains(1)

        "" in (d as Map<String, Int>)
        "" !in (d as Map<String, Int>)
        1 <!TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR!>in<!> (d as Map<String, Int>)
        1 <!TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR!>!in<!> (d as Map<String, Int>)
    }

    run { // E : D<String ,Int>
        val e = E()
        "" <!CONCURRENT_HASH_MAP_CONTAINS_OPERATOR_ERROR!>in<!> e
        "" <!CONCURRENT_HASH_MAP_CONTAINS_OPERATOR_ERROR!>!in<!> e
        1 <!CONCURRENT_HASH_MAP_CONTAINS_OPERATOR_ERROR!>!in<!> e
        2 <!CONCURRENT_HASH_MAP_CONTAINS_OPERATOR_ERROR!>in<!> e

        e.contains("")
        e.contains(1)

        "" in (e as Map<String, Int>)
        "" !in (e as Map<String, Int>)
        1 <!TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR!>in<!> (e as Map<String, Int>)
        1 <!TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR!>!in<!> (e as Map<String, Int>)
    }
}

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, flexibleType, functionDeclaration, integerLiteral,
intersectionType, javaFunction, lambdaLiteral, localProperty, nullableType, operator, override, propertyDeclaration,
stringLiteral, superExpression, typeParameter */
