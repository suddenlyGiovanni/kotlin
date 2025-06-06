// KLIB_RELATIVE_PATH_BASES: a, b

// IGNORE_IR_DESERIALIZATION_TEST: JS_IR
// ^ Pre-serialization IR contains absolute paths, post-deserialization IR contains paths relative to KLIB_RELATIVE_PATH_BASES

// Test that if we have two different files with the same name in the same package, KT-54028 doesn't reproduce.

// MODULE: lib

// FILE: a/cursed.kt

sealed interface LazyGridLayoutInfo {
    fun ok(): String
}

// FILE: b/cursed.kt

class LazyGridState {
    val layoutInfo: LazyGridLayoutInfo
        get() = EmptyLazyGridLayoutInfo
}

private object EmptyLazyGridLayoutInfo : LazyGridLayoutInfo {
    override fun ok() = "OK"
}

// MODULE: main(lib)
// FILE: main.kt

fun box(): String {
    return LazyGridState().layoutInfo.ok()
}

