// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL

class B<O>(val obj: O) {
    val v = B(obj)
}
