// RUN_PIPELINE_TILL: FRONTEND
// CHECK_TYPE
// NI_EXPECTED_FILE

interface Tr
interface G<T>

fun test(tr: Tr) = checkSubtype<<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>G<!>>(tr)

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType, infix,
interfaceDeclaration, nullableType, typeParameter, typeWithExtension */
