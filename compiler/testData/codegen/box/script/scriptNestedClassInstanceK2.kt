// TARGET_BACKEND: JVM
// IGNORE_BACKEND_K1: JVM_IR, JS_IR, JS_IR_ES6
// WITH_STDLIB
// FILE: test.kt

fun box(): String =
    Nested().x

// FILE: script.kts

class Nested {
    val x = "OK"
}
