// LANGUAGE: +JvmStaticInInterface
// DIAGNOSTICS: -UNUSED_VARIABLE
fun main() {
    <!JVM_STATIC_NOT_IN_OBJECT_OR_COMPANION!>@JvmStatic<!> fun a(){

    }
}