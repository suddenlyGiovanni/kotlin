// RUN_PIPELINE_TILL: BACKEND

class Bar

class Foo {
    fun bar() = this
    fun Bar.buz() = this
    fun Bar.foo() = this@Foo
    fun Bar.foobar() = this@foobar
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, thisExpression */
