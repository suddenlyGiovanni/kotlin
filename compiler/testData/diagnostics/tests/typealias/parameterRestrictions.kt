// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_TYPEALIAS_PARAMETER

typealias WithVariance<<!VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED!>in<!> X, <!VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED!>out<!> Y> = Int
typealias WithBounds1<T : <!BOUND_ON_TYPE_ALIAS_PARAMETER_NOT_ALLOWED!>T<!>> = Int
typealias WithBounds2<X : <!BOUND_ON_TYPE_ALIAS_PARAMETER_NOT_ALLOWED!>Y<!>, Y : <!BOUND_ON_TYPE_ALIAS_PARAMETER_NOT_ALLOWED!>X<!>> = Int

typealias WithBounds3<X> <!SYNTAX!>where <!DEBUG_INFO_MISSING_UNRESOLVED!>X<!> : <!DEBUG_INFO_MISSING_UNRESOLVED!>Any<!><!> = Int

val x: WithVariance<Int, Int> = 0

/* GENERATED_FIR_TAGS: in, integerLiteral, nullableType, out, propertyDeclaration, typeAliasDeclaration,
typeAliasDeclarationWithTypeParameter, typeConstraint, typeParameter */
