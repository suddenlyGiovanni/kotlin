// RUN_PIPELINE_TILL: FRONTEND
fun test(a: Int) {
    run<Int>f@{
      if (a > 0) return@f <!TYPE_MISMATCH!>""<!>
      return@f 1
    }

    run<Int>{ <!TYPE_MISMATCH!>""<!> }
    run<Int>{ 1 }
}

/* GENERATED_FIR_TAGS: comparisonExpression, functionDeclaration, ifExpression, integerLiteral, lambdaLiteral,
stringLiteral */
