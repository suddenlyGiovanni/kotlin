
// FILE: test.kt

fun box(){
    test(test("1", "2"),
                fail())

    test(fail(),
                test("1", "2"))
}

public fun checkEquals(p1: String, p2: String) {
    "check"
}

inline fun test(p: String, s: String) : String {
    return "123"
}

fun fail() : String {
    return "fail"
}

// EXPECTATIONS JVM_IR
// test.kt:5 box
// test.kt:17 box
// test.kt:6 box
// test.kt:21 fail
// test.kt:6 box
// test.kt:5 box
// test.kt:17 box
// test.kt:8 box
// test.kt:21 fail
// test.kt:8 box
// test.kt:9 box
// test.kt:17 box
// test.kt:8 box
// test.kt:17 box
// test.kt:10 box

// EXPECTATIONS JS_IR
// test.kt:6 box
// test.kt:21 fail
// test.kt:8 box
// test.kt:21 fail
// test.kt:10 box

// EXPECTATIONS WASM
// test.kt:5 $box (14, 19, 9)
// test.kt:17 $box (11, 4)
// test.kt:6 $box (16)
// test.kt:21 $fail (11, 4)
// test.kt:6 $box (16)
// test.kt:5 $box (4)
// test.kt:17 $box (11, 4)
// test.kt:8 $box (9)
// test.kt:21 $fail (11, 4)
// test.kt:8 $box (9)
// test.kt:9 $box (21, 26, 16)
// test.kt:17 $box (11, 4)
// test.kt:8 $box (4)
// test.kt:17 $box (11, 4)
// test.kt:10 $box (1)
