// RUN_PIPELINE_TILL: FRONTEND
val f: Boolean = true
private fun doUpdateRegularTasks() {
    try {
        while (f) {
            val xmlText = <!UNRESOLVED_REFERENCE!>getText<!>()
            if (<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>xmlText<!> <!DEBUG_INFO_MISSING_UNRESOLVED!>==<!> null) {}
            else {
                <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>xmlText<!>.<!DEBUG_INFO_MISSING_UNRESOLVED, VARIABLE_EXPECTED!>value<!> = 0 // !!!
            }
        }

    }
    finally {
        fun execute() {}
    }
}

/* GENERATED_FIR_TAGS: assignment, equalityExpression, functionDeclaration, ifExpression, integerLiteral, localFunction,
localProperty, propertyDeclaration, smartcast, tryExpression, whileLoop */
