// SKIP_JDK6
// TARGET_BACKEND: JVM
// WITH_STDLIB
// FULL_JDK
// PARAMETERS_METADATA

class A() {
    fun String.test(OK: String) {

    }
}

fun box(): String {
    val clazz = A::class.java
    val method = clazz.getDeclaredMethod("test", String::class.java, String::class.java)
    val parameters = method.getParameters()

    if (parameters[0].isImplicit() || parameters[0].isSynthetic()) return "wrong modifier on receiver parameter: ${parameters[0].modifiers}"

    return parameters[1].name
}
