// RUN_PIPELINE_TILL: FRONTEND
annotation class A(
    <!VAR_ANNOTATION_PARAMETER!>var<!> a: Int,
    <!MISSING_VAL_ON_ANNOTATION_PARAMETER!>b: String<!>
)

/* GENERATED_FIR_TAGS: annotationDeclaration, primaryConstructor, propertyDeclaration */
