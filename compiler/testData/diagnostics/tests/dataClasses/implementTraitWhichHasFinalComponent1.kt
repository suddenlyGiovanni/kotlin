// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
interface T {
    <!WRONG_MODIFIER_CONTAINING_DECLARATION!>final<!> fun component1(): Int = 42
}

<!DATA_CLASS_OVERRIDE_CONFLICT!>data<!> class A(val x: Int) : T

/* GENERATED_FIR_TAGS: classDeclaration, data, functionDeclaration, integerLiteral, interfaceDeclaration,
primaryConstructor, propertyDeclaration */
