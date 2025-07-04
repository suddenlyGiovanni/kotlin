// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// WITH_STDLIB

// MODULE: m1-common
// FILE: common.kt
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
<!EXPECT_ACTUAL_IR_INCOMPATIBILITY{JVM}!>expect<!> class Foo {
    fun <!EXPECT_ACTUAL_IR_MISMATCH{JVM}!>test<!>(a: AtomicInt): AtomicInt
}

// MODULE: m2-jvm()()(m1-common)
// FILE: Bar.java
import java.util.concurrent.atomic.AtomicInteger;

public class Bar {
    public AtomicInteger test(AtomicInteger i){
        return i;
    }
}

//FILE: test.kt
actual typealias <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>Foo<!> = Bar

/* GENERATED_FIR_TAGS: actual, classDeclaration, classReference, expect, functionDeclaration, javaType,
typeAliasDeclaration */
