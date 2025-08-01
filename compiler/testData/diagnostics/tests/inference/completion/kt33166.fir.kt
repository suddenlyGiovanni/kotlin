// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER

// FILE: GenericRunnable.java

public interface GenericRunnable<T> {
    T invoke();
}

// FILE: OurFuture.java

public class OurFuture<T> {
    static <T> OurFuture<T> createOurFuture(T result) {
        return null;
    }

    public <U> OurFuture<U> compose(GenericRunnable<OurFuture<U>> mapper) {
        return null;
    }
}

// FILE: test.kt

open class Either<out L> {
    class Left<out L>(val a: L) : Either<L>()
}

fun f1(future: OurFuture<String>, e: Either.Left<String>) {
    future.compose<Either<String>> {
        val x = when {
            true -> OurFuture.createOurFuture(e)
            else -> throw Exception()
        }
        <!RETURN_TYPE_MISMATCH!>x<!>
    }
}

fun f2(future: OurFuture<String>, e: Either.Left<String>) {
    future.compose<Either<String>> {
        when {
            true -> OurFuture.createOurFuture(e)
            else -> throw Exception()
        }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, flexibleType, functionDeclaration, javaFunction, javaType, lambdaLiteral,
localProperty, nestedClass, nullableType, out, primaryConstructor, propertyDeclaration, samConversion, typeParameter,
whenExpression */
