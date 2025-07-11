// RUN_PIPELINE_TILL: FRONTEND
interface Iterator<out T> {
 fun next() : T
 val hasNext : Boolean

 fun <R> map(transform: (element: T) -> R) : Iterator<R> =
    object : Iterator<R> {
      override fun next() : R = transform(this<!UNRESOLVED_LABEL!>@map<!>.next())

      override val hasNext : Boolean
        // There's no 'this' associated with the map() function, only this of the Iterator class
        get() = this<!UNRESOLVED_LABEL!>@map<!>.hasNext
    }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, functionDeclaration, functionalType, getter, interfaceDeclaration,
nullableType, out, override, propertyDeclaration, thisExpression, typeParameter */
