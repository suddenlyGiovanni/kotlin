// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// KT-557 Wrong type inference near sure extension function

fun <T : Any> T?.sure() : T = this!!

fun Array<String>.length() : Int {
    return 0;
}

fun test(array : Array<String?>?) {
    array?.sure<Array<String?>>().<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>length<!>()
}

/* GENERATED_FIR_TAGS: checkNotNullCall, funWithExtensionReceiver, functionDeclaration, integerLiteral, nullableType,
safeCall, thisExpression, typeConstraint, typeParameter */
