// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-71662

fun testStandardNavigation() {
    val resultA = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner(Value))
        // should fix OTv := ScopeOwner<Value> for scope navigation
        otvOwner.provide().memberFunction()
        // expected: Interloper </: ScopeOwner<Value>
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("Interloper; ScopeOwner<uninferred SOT (of class ScopeOwner<SOT>)>")!>Interloper<!>)
    }
    // expected: ScopeOwner<Value>
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner<Value>")!>resultA<!>

    val resultB = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner(Value))
        // should fix OTv := ScopeOwner<Value> for scope navigation
        otvOwner.provide().extensionFunction()
        // expected: Interloper </: ScopeOwner<Value>
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("Interloper; ScopeOwner<uninferred SOT (of class ScopeOwner<SOT>)> & ScopeOwner<uninferred SOTA (of fun <SOTA> ScopeOwner<SOTA>.extensionFunction)>")!>Interloper<!>)
    }
    // expected: ScopeOwner<Value>
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner<Value>")!>resultB<!>
}

fun testSafeNavigation() {
    val resultA = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner.Nullable(Value))
        // should fix OTv := ScopeOwner<Value>? for scope navigation
        otvOwner.provide()?.memberFunction()
        // expected: Interloper </: ScopeOwner<Value>?
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("Interloper; ScopeOwner<uninferred SOT (of fun <SOT> Nullable)>?")!>Interloper<!>)
    }
    // expected: ScopeOwner<Value>?
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner<Value>?")!>resultA<!>

    val resultB = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner.Nullable(Value))
        // should fix OTv := ScopeOwner<Value>? for scope navigation
        otvOwner.provide()?.extensionFunction()
        // expected: Interloper </: ScopeOwner<Value>?
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("Interloper; Nothing?")!>Interloper<!>)
    }
    // expected: ScopeOwner<Value>?
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner<Value>?")!>resultB<!>
}


class TypeVariableOwner<T> {
    fun constrain(subtypeValue: T) {}
    fun provide(): T = null!!
}

fun <OT> pcla(lambda: (TypeVariableOwner<OT>) -> Unit): OT = null!!

interface BaseType

object Value

class ScopeOwner<SOT>(value: SOT): BaseType {
    fun memberFunction() {}
    companion object {
        fun <SOT> Nullable(value: SOT): ScopeOwner<SOT>? = null
    }
}

fun <SOTA> ScopeOwner<SOTA>.extensionFunction() {}

object Interloper: BaseType

/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, companionObject, funWithExtensionReceiver,
functionDeclaration, functionalType, interfaceDeclaration, lambdaLiteral, localProperty, nullableType, objectDeclaration,
primaryConstructor, propertyDeclaration, safeCall, typeParameter */
