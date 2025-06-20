// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_EXPRESSION
// CHECK_TYPE

fun case_0() {
    val z: Any? = 10
    val y = z.run {
        this as Int
        <!DEBUG_INFO_SMARTCAST!>this<!> // error in NI: required Int, found Any?; just inferred to Any? in OI
    }
    y checkType { <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><Any?>() }
    y checkType { _<Int>() }
}

fun case_1(z: Any?) {
    val y = z.run {
        when (this) {
            is String -> return@run <!DEBUG_INFO_SMARTCAST!>this<!> // type mismatch in the new inference (required String, found Any?)
            is Float -> ""
            else -> return@run ""
        }
    }
    y checkType { <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><Any?>() }
    y checkType { _<kotlin.String>() }
    // y is inferred to Any?
}

fun case_2(z: Any?) {
    val y = z.run {
        when (this) {
            is String -> <!DEBUG_INFO_SMARTCAST!>this<!>
            is Float -> ""
            else -> return@run ""
        }
    }
    y checkType { _<kotlin.String>() }
    // y is inferred to String
}

fun case_3(z: Any?) {
    val y = z.let {
        when (it) {
            is String -> return@let <!DEBUG_INFO_SMARTCAST!>it<!>
            is Float -> ""
            else -> return@let ""
        }
    }
    y checkType { <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><Any?>() }
    y checkType { _<kotlin.String>() }
    // y is inferred to String
}

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType,
infix, integerLiteral, isExpression, lambdaLiteral, localProperty, nullableType, propertyDeclaration, smartcast,
stringLiteral, thisExpression, typeParameter, typeWithExtension, whenExpression, whenWithSubject */
