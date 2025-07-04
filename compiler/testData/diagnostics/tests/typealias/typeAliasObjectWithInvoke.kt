// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// NI_EXPECTED_FILE

object ObjectWithInvoke {
    operator fun invoke() = this
}

class ClassWithCompanionObjectWithInvoke {
    companion object {
        operator fun invoke(x: Any) = x
    }
}

typealias WI = ObjectWithInvoke

typealias CWI = ClassWithCompanionObjectWithInvoke

val test1 = WI()
val test2 = WI(<!TOO_MANY_ARGUMENTS!>null<!>)

val test3 = CWI()
val test4 = CWI("")
val test5 = CWI(<!NULL_FOR_NONNULL_TYPE!>null<!>)
val test5a = ClassWithCompanionObjectWithInvoke(<!NULL_FOR_NONNULL_TYPE!>null<!>)

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, objectDeclaration, operator,
propertyDeclaration, stringLiteral, thisExpression, typeAliasDeclaration */
