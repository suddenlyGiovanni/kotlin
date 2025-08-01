// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// check that there is no SOE on checking for instance
interface Visitor<T>
interface Acceptor<T>

class Word : Acceptor<Visitor<Word>>

class V : Visitor<Word>

class S<T : Acceptor<U>, U : Visitor<T>>(val visitor: U, val acceptor: T) {
    fun test() {
        visitor is V
        acceptor is Word
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, interfaceDeclaration, isExpression, nullableType,
primaryConstructor, propertyDeclaration, typeConstraint, typeParameter */
