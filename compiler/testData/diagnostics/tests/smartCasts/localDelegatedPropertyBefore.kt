// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -ProhibitSmartcastsOnLocalDelegatedProperty

class AlternatingDelegate {
    var counter: Int = 0
    operator fun getValue(thisRef: Any?, property: <!UNRESOLVED_REFERENCE!>KProperty<!><*>): Any? =
        if (counter++ % 2 == 0) 42 else ""
}

fun failsWithClassCastException() {
    val sometimesNotInt: Any? by AlternatingDelegate()

    if (sometimesNotInt is Int) {
        <!DEBUG_INFO_SMARTCAST, DEPRECATED_SMARTCAST!>sometimesNotInt<!>.inc()
    }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, equalityExpression, functionDeclaration, ifExpression,
incrementDecrementExpression, integerLiteral, isExpression, localProperty, multiplicativeExpression, nullableType,
operator, propertyDeclaration, propertyDelegate, smartcast, stringLiteral */
