// RUN_PIPELINE_TILL: FRONTEND

interface B<T : S?, S : Any> {
    val t: T
}

class C(override val t: Any?) : B<Any?, Any>

fun f(b: B<*, Any>) {
    val y = b.t
    if (y is String?) {
        y<!UNSAFE_CALL!>.<!>length
    }
}

fun main() {
    f(C("hello"))
    f(C(null))
}

/* GENERATED_FIR_TAGS: capturedType, classDeclaration, functionDeclaration, ifExpression, interfaceDeclaration,
isExpression, localProperty, nullableType, override, primaryConstructor, propertyDeclaration, smartcast, starProjection,
stringLiteral, typeConstraint, typeParameter */
