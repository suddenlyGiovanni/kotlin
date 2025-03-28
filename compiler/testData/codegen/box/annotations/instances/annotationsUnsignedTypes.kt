// WITH_STDLIB
// LANGUAGE: +InstantiationOfAnnotationClasses

annotation class AnnotationWithSignedArray(val array: IntArray)
annotation class AnnotationWithUnsignedArray(val array: UIntArray)

fun box(): String {
    if (!(AnnotationWithSignedArray(intArrayOf()) == AnnotationWithSignedArray(intArrayOf()))) return "Fail signed"
    if (!(AnnotationWithUnsignedArray(uintArrayOf()) == AnnotationWithUnsignedArray(uintArrayOf()))) return "Fail unsigned"
    return "OK"
}