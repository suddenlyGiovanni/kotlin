// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

// CHECK_BYTECODE_TEXT
// 1 java/lang/invoke/LambdaMetafactory

// FILE: enumValues.kt
enum class ABC(val x: String = "") {
    A("OK"), B, C
}

fun box() =
    Sam(::enumValues).get()[0].x

// FILE: Sam.java
public interface Sam {
    ABC[] get();
}
