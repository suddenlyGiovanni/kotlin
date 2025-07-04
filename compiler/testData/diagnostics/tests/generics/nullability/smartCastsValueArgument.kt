// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_VARIABLE -UNUSED_PARAMETER -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE

fun <T : CharSequence> bar1(x: T) {}
fun bar2(x: CharSequence) {}
fun bar3(x: String) {}

fun <T : CharSequence?> foo(x: T) {
    var y1: CharSequence = ""
    var y2: String = ""
    if (x != null) {
        if (<!SENSELESS_COMPARISON!>x != null<!>) {}

        y1 = <!DEBUG_INFO_SMARTCAST!>x<!>
        y2 = <!TYPE_MISMATCH!>x<!>

        bar1(<!DEBUG_INFO_SMARTCAST!>x<!>)
        bar1<CharSequence>(<!DEBUG_INFO_SMARTCAST!>x<!>)
        bar2(<!DEBUG_INFO_SMARTCAST!>x<!>)
        bar3(<!TYPE_MISMATCH!>x<!>)
    }

    if (x is String) {
        y1 = <!DEBUG_INFO_SMARTCAST!>x<!>
        y2 = <!DEBUG_INFO_SMARTCAST!>x<!>

        bar1(<!DEBUG_INFO_SMARTCAST!>x<!>)
        bar2(<!DEBUG_INFO_SMARTCAST!>x<!>)
        bar3(<!DEBUG_INFO_SMARTCAST!>x<!>)
    }

    if (x is CharSequence) {
        y1 = <!DEBUG_INFO_SMARTCAST!>x<!>
        y2 = <!TYPE_MISMATCH!>x<!>

        bar1(<!DEBUG_INFO_SMARTCAST!>x<!>)
        bar2(<!DEBUG_INFO_SMARTCAST!>x<!>)
        bar3(<!TYPE_MISMATCH!>x<!>)
    }

    if (1 == 1) {
        val y = x!!
        bar1(<!DEBUG_INFO_SMARTCAST!>x<!>)
        bar1<CharSequence>(<!DEBUG_INFO_SMARTCAST!>x<!>)
        bar2(<!DEBUG_INFO_SMARTCAST!>x<!>)
        bar3(<!TYPE_MISMATCH!>x<!>)

        bar1(y)
        bar2(y)
        bar3(<!TYPE_MISMATCH!>y<!>)
    }
}

/* GENERATED_FIR_TAGS: assignment, checkNotNullCall, dnnType, equalityExpression, functionDeclaration, ifExpression,
integerLiteral, intersectionType, isExpression, localProperty, nullableType, propertyDeclaration, smartcast,
stringLiteral, typeConstraint, typeParameter */
