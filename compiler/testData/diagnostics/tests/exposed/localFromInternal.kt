// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
class My {
    internal open class ThreadLocal
    // Private from local: ???
    private val values = 
            // Local from internal: Ok
            object: ThreadLocal() {}
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, nestedClass, propertyDeclaration */
