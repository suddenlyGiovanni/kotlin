// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtClass
// MAIN_FILE_NAME: OuterClass

class OuterClass {
    class MyClass {
        var myInt: Int = 0
            get() contract [returnsNotNull()] = 1
            set(value) {
                field = value * 10
            }
    }

    class AnotherClass(val multiplier: Int) {
        var anotherInt: Int = 0
            get() contract [returnsNotNull()] = 1
            set(value) contract [returns()] {
                field = value * multiplier
            }
    }

    class SomeClass(multiplier: Int?) {
        var someInt: Int = 0
            get() contract [returnsNotNull()] = 1
            set(value) contract [returns() implies (value != null)] {
                value ?: throw IllegalArgumentException()
                field = value
            }
    }
}