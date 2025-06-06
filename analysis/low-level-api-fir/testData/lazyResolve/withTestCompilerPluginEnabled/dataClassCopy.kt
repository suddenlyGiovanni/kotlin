// WITH_FIR_TEST_COMPILER_PLUGIN
// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// MEMBER_NAME_FILTER: copy
package pack

annotation class Anno(val value: Int)

@Target(AnnotationTarget.PROPERTY)
annotation class PropertyAnno(val s: String)

@Anno(0)
data cl<caret>ass MyData @Anno(3) constructor(
    @Anno(1) @PropertyAnno("str") val p1: String,
    @param:Anno(2) val p2: Int,
)
