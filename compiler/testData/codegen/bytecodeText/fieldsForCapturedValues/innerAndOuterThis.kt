class Outer {
    inner class Inner {
        private fun bar() {
            class NamedLocal {
                fun run() {
                    innerFoo()
                    outerFoo()
                }
            }
        }

        fun innerFoo() {}
    }

    fun outerFoo() {}
}

// 1 final synthetic LOuter\$Inner; this\$0
// 1 final synthetic LOuter; this\$1
