// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_ANONYMOUS_PARAMETER -UNUSED_VARIABLE
// COMPARE_WITH_LIGHT_TREE

fun <T> select(vararg x: T) = x[0]
fun <K> id(x: K) = x

fun main() {
    val x1 = select<Any?>(id { x, <!CANNOT_INFER_PARAMETER_TYPE!>y<!> -> }, { x: Int, <!CANNOT_INFER_PARAMETER_TYPE!>y<!> -> })
    val x2 = <!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>select<!>(<!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>id<!> { x, <!CANNOT_INFER_PARAMETER_TYPE!>y<!> -> }, { x: Int, <!CANNOT_INFER_PARAMETER_TYPE!>y<!> -> })

    val x3 = <!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>select<!>(<!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>id<!>(fun (x, <!CANNOT_INFER_PARAMETER_TYPE!>y<!>) {}), fun (x: Int, <!CANNOT_INFER_PARAMETER_TYPE!>y<!>) {})

    val x4 = select<Any?>((fun (<!CANNOT_INFER_PARAMETER_TYPE!>x<!>, <!CANNOT_INFER_PARAMETER_TYPE!>y<!>) {}), fun (x: Int, <!CANNOT_INFER_PARAMETER_TYPE!>y<!>) {})
    val x5 = select<Any?>(id(fun (<!CANNOT_INFER_PARAMETER_TYPE!>x<!>, <!CANNOT_INFER_PARAMETER_TYPE!>y<!>) {}), fun (x: Int, <!CANNOT_INFER_PARAMETER_TYPE!>y<!>) {})
    val x6 = id<Any?>(fun (<!CANNOT_INFER_PARAMETER_TYPE!>x<!>) {})

    select<Any?>(fun (<!CANNOT_INFER_PARAMETER_TYPE!>x<!>) {}, fun (<!CANNOT_INFER_PARAMETER_TYPE!>x<!>) {})
}
