// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
package b

fun main() {
    var ints : Array<Int?> = arrayOfNulls<Int>(31)

    ints[0] = 4; ints[11] = 5; ints[2] =7
    for(i in 0..ints.size)
        ints[i<!SYNTAX!><!>
}

/* GENERATED_FIR_TAGS: assignment, forLoop, functionDeclaration, integerLiteral, localProperty, nullableType,
propertyDeclaration, rangeExpression */
