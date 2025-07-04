// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// OPT_IN: kotlin.contracts.ExperimentalContracts
// DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

inline fun <T> myRun(block: () -> T): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block()
}

fun getBoolean(): Boolean = false

fun test() {
    val x: Int

    if (getBoolean())
        myRun {
            while (getBoolean()) {
                do {
                    myRun {
                        if (getBoolean()) {
                            x = 42 // No reassignment because of break
                        }
                        else {
                            x = 43 // No reassignment because of break
                        }
                    }
                    break
                } while (getBoolean())
                // Loop executed exectly once, initializing x
                myRun { x.inc() }

                myRun { <!VAL_REASSIGNMENT!>x<!> = 42 }
                break
            }
            // x is ID? here because loop could've been execited
            // VAL_REASSIGNMENT isn't reported because of repeating diagnostic
            <!VAL_REASSIGNMENT!>x<!> = 42
            // x is ID now
        }
    else
        myRun {
            x = 42
        }
    // x is ID because both branches are ID

    x.inc()
}

/* GENERATED_FIR_TAGS: assignment, break, contractCallsEffect, contracts, doWhileLoop, functionDeclaration,
functionalType, ifExpression, inline, integerLiteral, lambdaLiteral, localProperty, nullableType, propertyDeclaration,
typeParameter, whileLoop */
