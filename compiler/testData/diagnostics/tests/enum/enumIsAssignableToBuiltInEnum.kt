// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
import java.lang.annotation.RetentionPolicy

enum class E {
    ENTRY
}

// Test resolve from source
val a: Enum<E> = E.ENTRY

// Test Java resolve
val b: Enum<RetentionPolicy> = RetentionPolicy.RUNTIME

// Test deserialized resolve
val c: Enum<AnnotationTarget> = AnnotationTarget.CLASS

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, javaProperty, propertyDeclaration */
