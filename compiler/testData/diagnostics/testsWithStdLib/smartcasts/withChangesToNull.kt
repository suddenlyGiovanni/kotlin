// RUN_PIPELINE_TILL: FRONTEND
fun foo(y: String?) {
    var x: String? = ""
    if (x != null) {
        with(y?.let { x = null; it }) {
            this<!UNSAFE_CALL!>.<!>length
            <!SMARTCAST_IMPOSSIBLE!>x<!>.length
        }
        <!SMARTCAST_IMPOSSIBLE!>x<!>.length
    }
}

/* GENERATED_FIR_TAGS: assignment, equalityExpression, functionDeclaration, ifExpression, lambdaLiteral, localProperty,
nullableType, propertyDeclaration, safeCall, smartcast, stringLiteral, thisExpression */
