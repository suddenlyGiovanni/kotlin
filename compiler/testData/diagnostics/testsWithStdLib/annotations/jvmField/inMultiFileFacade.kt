// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
@file:kotlin.jvm.JvmMultifileClass
@file:JvmName("SomeName")

<!INAPPLICABLE_JVM_FIELD!>@JvmField<!>
val c = 4

<!INAPPLICABLE_JVM_FIELD!>@JvmField<!>
var g = 5

class C {
    @JvmField
    var g = 5
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, classDeclaration, integerLiteral, propertyDeclaration, stringLiteral */
