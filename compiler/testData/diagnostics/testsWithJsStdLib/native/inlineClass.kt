// FIR_IDENTICAL
// LANGUAGE: +InlineClasses, -JsExternalPropertyParameters
// DIAGNOSTICS: +ENUM_CLASS_IN_EXTERNAL_DECLARATION_WARNING, -INLINE_CLASS_DEPRECATED

external inline class <!WRONG_EXTERNAL_DECLARATION!>C(<!EXTERNAL_CLASS_CONSTRUCTOR_PROPERTY_PARAMETER!>val a: Int<!>)<!> {
    fun foo()
}

<!WRONG_MODIFIER_TARGET!>inline<!> external enum class <!ENUM_CLASS_IN_EXTERNAL_DECLARATION_WARNING, WRONG_EXTERNAL_DECLARATION!>E<!> {
    A
}
