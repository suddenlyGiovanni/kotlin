fun test(i: Int) =
    if (i > 0) 1
    else if (i < 0) -1
    else 0

fun testEmptyBranches1(flag: Boolean) {
    if (flag) ; else true
    if (flag) true else;
}

fun testEmptyBranches2(flag: Boolean) {
    if (flag) {} else true
    if (flag) true else {}
}

fun testEmptyBranches3(flag: Boolean) {
    if (flag)
    else true
}

fun testIfElseInElseBlock(first: Boolean, second: Boolean): Int {
    if (first) {
        return 4
    } else {
        if (second) {
            return 2
        } else return -1
    }
}
