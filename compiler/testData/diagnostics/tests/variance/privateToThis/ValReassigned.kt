// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
fun <T> getT(): T = null!!

class A<in I>(init: I) {
    private val i: I

    init {
        i = getT()
    }

    private var i2 = i
    private val i3: I

    private var i4 = getT<I>()

    init {
        i2 = getT()
        i3 = init
        i4 = i3
    }
}

/* GENERATED_FIR_TAGS: assignment, checkNotNullCall, classDeclaration, functionDeclaration, in, init, nullableType,
primaryConstructor, propertyDeclaration, typeParameter */
