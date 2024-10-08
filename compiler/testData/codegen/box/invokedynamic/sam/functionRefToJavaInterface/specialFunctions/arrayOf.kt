// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

// CHECK_BYTECODE_TEXT
// 1 java/lang/invoke/LambdaMetafactory

// FILE: arrayOf.kt
fun box() =
    Sam(::arrayOf).get(arrayOf("OK"))[0]

// FILE: Sam.java
public interface Sam {
    String[] get(String[] s);
}
