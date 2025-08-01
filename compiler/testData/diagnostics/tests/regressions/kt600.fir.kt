// RUN_PIPELINE_TILL: BACKEND
//KT-600 Problem with 'sure' extension function type inference

fun <T : Any> T?._sure() : T { if (this != null) return this else throw NullPointerException() }

fun test() {
    val i : Int? = 10
    val i2 : Int = i._sure() // inferred type is Int? but Int was excepted
}

/* GENERATED_FIR_TAGS: dnnType, equalityExpression, funWithExtensionReceiver, functionDeclaration, ifExpression,
integerLiteral, localProperty, nullableType, propertyDeclaration, smartcast, thisExpression, typeConstraint,
typeParameter */
