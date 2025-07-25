// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +WhenGuards
// DIAGNOSTICS: -UNUSED_PARAMETER, -UNUSED_EXPRESSION
// ISSUES: KT-70133

inline fun invokeInline(x: () -> Unit, y: Any) {
    x()
}

fun invokeLater(x: () -> Unit, y: Any) {
    x()
}

fun test1(y: Any) {
    val x: String
    invokeInline(
        { <!UNINITIALIZED_VARIABLE!>x<!>.length },
        when(y) {
            is String if { <!CAPTURED_VAL_INITIALIZATION!>x<!>=" "; true }()  -> ""
            else -> "1"
        }
    )
}

fun test2(y: Any) {
    val x: String
    invokeLater(
        { <!UNINITIALIZED_VARIABLE!>x<!>.length },
        when(y) {
            is String if { <!CAPTURED_VAL_INITIALIZATION!>x<!>=" "; true }()  -> ""
            else -> "1"
        }
    )
}

fun test3() {
    val x: String
    invokeInline(
        { <!UNINITIALIZED_VARIABLE!>x<!>.length  },
        fun() {
            if (true) {
                <!CAPTURED_VAL_INITIALIZATION!>x<!> = "";""
            } else {
                <!CAPTURED_VAL_INITIALIZATION!>x<!> = "";""
            }
        }
    )
}

fun test4() {
    val x: String
    invokeLater(
        { <!UNINITIALIZED_VARIABLE!>x<!>.length },
        fun() {
            if (true) {
                <!CAPTURED_VAL_INITIALIZATION!>x<!> = "";""
            } else {
                <!CAPTURED_VAL_INITIALIZATION!>x<!> = "";""
            }
        }
    )
}

fun test5() {
    val x: String
    invokeInline(
        { <!UNINITIALIZED_VARIABLE!>x<!>.length  },
        object {
            fun foo() {
                <!CAPTURED_VAL_INITIALIZATION!>x<!> = ""
            }
        }.foo()
    )
}

fun test6() {
    val x: String
    invokeLater(
        { <!UNINITIALIZED_VARIABLE!>x<!>.length },
        object {
            fun foo() {
                <!CAPTURED_VAL_INITIALIZATION!>x<!> = ""
            }
        }.foo()
    )
}

fun test7() {
    val x: String
    invokeInline(
        { <!UNINITIALIZED_VARIABLE!>x<!>.length },
        { <!CAPTURED_VAL_INITIALIZATION!>x<!> = "" ; "" } <!USELESS_ELVIS!>?: { <!CAPTURED_VAL_INITIALIZATION!>x<!> = "" ; "" }<!>
    )
}

fun test8() {
    val x: String
    invokeLater(
        { <!UNINITIALIZED_VARIABLE!>x<!>.length },
        { <!CAPTURED_VAL_INITIALIZATION!>x<!> = "" ; "" } <!USELESS_ELVIS!>?: { <!CAPTURED_VAL_INITIALIZATION!>x<!> = "" ; "" }<!>
    )
}

/* GENERATED_FIR_TAGS: andExpression, anonymousFunction, anonymousObjectExpression, assignment, elvisExpression,
functionDeclaration, functionalType, guardCondition, ifExpression, inline, isExpression, lambdaLiteral, localProperty,
propertyDeclaration, stringLiteral, whenExpression, whenWithSubject */
