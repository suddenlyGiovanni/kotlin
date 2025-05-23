/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm

import kotlin.annotation.AnnotationTarget.*

/**
 * Instructs the Kotlin compiler to generate overloads for this function that substitute default parameter values.
 *
 * If a method has N parameters and M of which have default values, M overloads are generated: the first one
 * takes N-1 parameters (all but the last one that takes a default value), the second takes N-2 parameters, and so on.
 */
@Target(FUNCTION, CONSTRUCTOR)
@MustBeDocumented
@OptionalExpectation
public expect annotation class JvmOverloads()

/**
 * Specifies that an additional static method needs to be generated from this element if it's a function.
 * If this element is a property, additional static getter/setter methods should be generated.
 *
 * See the [Kotlin language documentation](https://kotlinlang.org/docs/reference/java-to-kotlin-interop.html#static-methods)
 * for more information.
 */
@Target(FUNCTION, PROPERTY, PROPERTY_GETTER, PROPERTY_SETTER)
@MustBeDocumented
@OptionalExpectation
public expect annotation class JvmStatic()

/**
 * Specifies the name for the Java class or method which is generated from this element.
 *
 * See the [Kotlin language documentation](https://kotlinlang.org/docs/reference/java-to-kotlin-interop.html#handling-signature-clashes-with-jvmname)
 * for more information.
 * @property name the name of the element.
 */
@Target(FILE, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER)
@MustBeDocumented
@OptionalExpectation
public expect annotation class JvmName(val name: String)

/**
 * Instructs the Kotlin compiler to generate a multifile class with top-level functions and properties declared in this file as one of its parts.
 * Name of the corresponding multifile class is provided by the [JvmName] annotation.
 */
@Target(FILE)
@MustBeDocumented
@OptionalExpectation
public expect annotation class JvmMultifileClass()

/**
 * This annotation marks Kotlin `expect` declarations that are implicitly actualized by Java.
 *
 * ## Safety Risks
 *
 * Implicit actualization bypasses safety features, potentially leading to errors or unexpected behavior. If you use this annotation, some
 * of the expect-actual invariants are not checked.
 *
 * Use this annotation only as a last resort. The annotation might stop working in future Kotlin versions without prior notice.
 *
 * If you use this annotation, consider describing your use cases in [KT-58545](https://youtrack.jetbrains.com/issue/KT-58545) comments.
 *
 * ## Migration
 *
 * Rewrite the code using explicit `actual typealias`. Unfortunately, it requires you to move your expect declarations into another
 * package. Refer to [KT-58545](https://youtrack.jetbrains.com/issue/KT-58545) for more detailed migration example.
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS)
@ExperimentalMultiplatform
@MustBeDocumented
@SinceKotlin("1.9")
@OptionalExpectation
public expect annotation class ImplicitlyActualizedByJvmDeclaration()

/**
 * Instructs the Kotlin compiler not to generate getters/setters for this property and expose it as a field.
 *
 * See the [Kotlin language documentation](https://kotlinlang.org/docs/reference/java-to-kotlin-interop.html#instance-fields)
 * for more information.
 */
@Target(FIELD)
@MustBeDocumented
@OptionalExpectation
public expect annotation class JvmField()

/**
 * Sets `ACC_SYNTHETIC` flag on the annotated target in the Java bytecode.
 *
 * Synthetic targets become inaccessible for Java sources at compile time while still being accessible for Kotlin sources.
 * Marking target as synthetic is a binary compatible change, already compiled Java code will be able to access such target.
 *
 * This annotation is intended for *rare cases* when API designer needs to hide Kotlin-specific target from Java API
 * while keeping it a part of Kotlin API so the resulting API is idiomatic for both languages.
 */
@Target(FILE, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, FIELD)
@OptionalExpectation
public expect annotation class JvmSynthetic()

/**
 * Instructs compiler to generate or omit wildcards for type arguments corresponding to parameters with
 * declaration-site variance, for example such as `Collection<out T>` has.
 *
 * If the innermost applied `@JvmSuppressWildcards` has `suppress=true`, the type is generated without wildcards.
 * If the innermost applied `@JvmSuppressWildcards` has `suppress=false`, the type is generated with wildcards.
 *
 * It may be helpful only if declaration seems to be inconvenient to use from Java.
 */
@Target(CLASS, FUNCTION, PROPERTY, TYPE)
@MustBeDocumented
@OptionalExpectation
public expect annotation class JvmSuppressWildcards(val suppress: Boolean = true)

/**
 * Instructs compiler to generate wildcard for annotated type arguments corresponding to parameters with declaration-site variance.
 *
 * It may be helpful only if declaration seems to be inconvenient to use from Java without wildcard.
 */
@Target(TYPE)
@MustBeDocumented
@OptionalExpectation
public expect annotation class JvmWildcard()

/**
 * Specifies that given value class is inline class.
 *
 * Adding and removing the annotation is binary incompatible change, since inline classes' methods and functions with inline classes
 * in their signature are mangled.
 */
@Target(CLASS)
@MustBeDocumented
@SinceKotlin("1.5")
@OptionalExpectation
public expect annotation class JvmInline()

/**
 * Instructs compiler to mark the class as a record and generate relevant toString/equals/hashCode methods
 */
@Target(CLASS)
@MustBeDocumented
@OptionalExpectation
@SinceKotlin("1.5")
public expect annotation class JvmRecord()

/**
 * Marks the JVM backing field of the annotated `var` property as `volatile`, meaning that reads and writes to this field
 * are atomic and writes are always made visible to other threads. If another thread reads the value of this field (e.g. through its accessor),
 * it sees not only that value, but all side effects that led to writing that value.
 *
 * This annotation has effect only in Kotlin/JVM. It's recommended to use [kotlin.concurrent.Volatile] annotation instead
 * in multiplatform projects.
 *
 * Note that only _backing field_ operations are atomic when the field is annotated with `Volatile`.
 * For example, if the property getter or setter make several operations with the backing field,
 * a _property_ operation, i.e. reading or setting it through these accessors, is not guaranteed to be atomic.
 */
@Target(FIELD)
@MustBeDocumented
@OptionalExpectation
@Deprecated("Use kotlin.concurrent.Volatile annotation in multiplatform code instead.", ReplaceWith("kotlin.concurrent.Volatile", "kotlin.concurrent.Volatile"))
@DeprecatedSinceKotlin(warningSince = "1.9", errorSince = "2.1")
public expect annotation class Volatile()

/**
 * Marks the backing field of the annotated property with the `transient` modifier on the JVM platform, meaning that it is not
 * a part of the serialized form of the object when serialized with `java.io.Serializable` machinery.
 *
 * **Warning:** the `java.io.Serializable` is an unsound mechanism that bypasses classes' invariants.
 * When `@Transient` annotation is applied to a property, the author must ensure that either the property has a nullable type
 * or that an author-supplied `readResolve` is implemented, supplying a conforming value for the non-nullable transient property.
 *
 * See also: ["Java Object Serialization Specification"](https://docs.oracle.com/en/java/javase/21/docs/specs/serialization/index.html)
 */
@Target(FIELD)
@MustBeDocumented
@OptionalExpectation
public expect annotation class Transient()

/**
 * Marks the JVM method generated from the annotated function as `strictfp`, meaning that the precision
 * of floating point operations performed inside the method needs to be restricted in order to
 * achieve better portability.
 */
@Target(FUNCTION, CONSTRUCTOR, PROPERTY_GETTER, PROPERTY_SETTER, CLASS)
@MustBeDocumented
@OptionalExpectation
public expect annotation class Strictfp()

/**
 * Marks the JVM method generated from the annotated function as `synchronized`, meaning that the method
 * will be protected from concurrent execution by multiple threads by the monitor of the instance (or,
 * for static methods, the class) on which the method is defined.
 *
 * Note that for an extension function, the monitor of the facade class, where it gets compiled to a static method, is used.
 * Therefore, this annotation is recommended to be applied only to member functions and properties.
 */
@Target(FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER)
@MustBeDocumented
@OptionalExpectation
@Deprecated("Synchronizing methods on a class instance is not supported on platforms other than JVM. If you need to annotate a common method as JVM-synchronized, introduce your own optional-expectation annotation and actualize it with a typealias to kotlin.jvm.Synchronized.")
@DeprecatedSinceKotlin(warningSince = "1.8", errorSince = "2.1")
public expect annotation class Synchronized()


@Target(FILE)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
@SinceKotlin("1.2")
@OptionalExpectation
internal expect annotation class JvmPackageName(val name: String)


/**
 * Makes the annotated lambda function implement `java.io.Serializable`,
 * generates a pretty `toString` implementation and adds reflection metadata.
 */
@Target(EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
@SinceKotlin("1.8")
@OptionalExpectation
public expect annotation class JvmSerializableLambda()

/**
 * This annotation instructs the compiler to expose the API of functions with inline classes
 * (and the classes containing them, including inline classes themselves)
 * as their boxed variant for effective usage from Java.
 *
 * It performs the following transformations:
 *
 * - For annotated functions and constructors that take or return inline classes,
 *   an unmangled wrapper function is created where inline classes are boxed.
 *   The wrapper is thus visible and callable from Java.
 *
 * - If class is annotated, the annotation implicitly propagates to its methods, forcing
 *   the compiler to generate wrappers for them.
 *
 * - A constructor is made available from Java.
 *
 * These changes maintain backward compatibility, allowing existing API to be safely marked.
 *
 * @property jvmName optional wrapper name. Only applicable to functions, getters and setters.
 */
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
@SinceKotlin("2.2")
@ExperimentalStdlibApi
@OptionalExpectation
@Target(
    // function-like
    FUNCTION, CONSTRUCTOR,
    PROPERTY_GETTER, PROPERTY_SETTER,
    // containers
    CLASS,
)
public expect annotation class JvmExposeBoxed(val jvmName: String = "")
