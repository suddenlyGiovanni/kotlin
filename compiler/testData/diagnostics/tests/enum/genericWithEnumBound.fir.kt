// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-59333
// LANGUAGE: +ForbidUpperBoundsViolationOnTypeOperatorAndParameterBounds

interface MyInterface {
    val value: String
}

enum class WrongTypeEnum {
    WrongEnum1,
    WrongEnum2,
}

enum class CorrectTypeEnum(override val value: String) : MyInterface {
    CorrectEnum1("1"),
    CorrectEnum2("2"),
}

fun <T : Enum<<!UPPER_BOUND_VIOLATED_IN_TYPE_OPERATOR_OR_PARAMETER_BOUNDS_ERROR!>out MyInterface<!>>> myFunction(arg: T) {}

class MyClass<T : Enum<<!UPPER_BOUND_VIOLATED!>out MyInterface<!>>>(val arg: T)

fun test() {
    myFunction(CorrectTypeEnum.CorrectEnum1)
    myFunction(<!ARGUMENT_TYPE_MISMATCH!>WrongTypeEnum.WrongEnum1<!>)
    MyClass(CorrectTypeEnum.CorrectEnum1)
    MyClass(<!ARGUMENT_TYPE_MISMATCH!>WrongTypeEnum.WrongEnum1<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, enumDeclaration, enumEntry, functionDeclaration, interfaceDeclaration,
outProjection, override, primaryConstructor, propertyDeclaration, typeConstraint, typeParameter */
