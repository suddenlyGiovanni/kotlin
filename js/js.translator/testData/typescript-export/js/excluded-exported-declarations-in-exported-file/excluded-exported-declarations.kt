// This file was generated automatically. See  generateTestDataForTypeScriptWithFileExport.kt
// DO NOT MODIFY IT MANUALLY.

// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// MODULE: JS_TESTS
// WITH_STDLIB
// FILE: declarations.kt

@file:JsExport

package foo


@JsExport.Ignore
val baz: String = "Baz"


@JsExport.Ignore
fun inter(): String = "inter"


@JsExport.Ignore
class NotExportableNestedInsideInterface

@JsExport.Ignore

object Comanion {
    val foo: String ="FOO"
}


val foo: String = "Foo"


fun bar() = "Bar"

@JsExport.Ignore

inline fun <A, reified B> A.notExportableReified(): Boolean = this is B

@JsExport.Ignore

suspend fun notExportableSuspend(): String = "SuspendResult"


@JsExport.Ignore
fun notExportableReturn(): List<String> = listOf("1", "2")


@JsExport.Ignore
val String.notExportableExentsionProperty: String
    get() = "notExportableExentsionProperty"


@JsExport.Ignore
annotation class NotExportableAnnotation


@JsExport.Ignore
value class NotExportableInlineClass(val value: Int)