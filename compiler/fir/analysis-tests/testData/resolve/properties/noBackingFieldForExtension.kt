// RUN_PIPELINE_TILL: BACKEND
interface B {
    fun foo(): Int
}

class A {
    val String.x: Int get() {
        return field.foo()
    }

    val String.field: B get() = TODO()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, getter, interfaceDeclaration, propertyDeclaration,
propertyWithExtensionReceiver */
