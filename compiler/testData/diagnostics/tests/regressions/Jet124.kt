// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
fun foo1() :  (Int) -> Int = { x: Int -> x }

fun foo() {
  val h :  (Int) -> Int = foo1();
  h(1)
  val m :  (Int) -> Int = {a : Int -> 1}//foo1()
  m(1)
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, integerLiteral, lambdaLiteral, localProperty,
propertyDeclaration */
