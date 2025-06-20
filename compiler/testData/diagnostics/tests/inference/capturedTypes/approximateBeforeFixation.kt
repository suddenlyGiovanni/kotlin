// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
fun <T> Array<out T>.intersect(other: Iterable<T>) {
    val set = toMutableSet()
    set.retainAll(other)
}

fun <X> Array<out X>.toMutableSet(): MutableSet<X> = TODO()
fun <Y> MutableCollection<in Y>.retainAll(elements: Iterable<Y>) {}

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, inProjection, localProperty, nullableType,
outProjection, propertyDeclaration, typeParameter */
