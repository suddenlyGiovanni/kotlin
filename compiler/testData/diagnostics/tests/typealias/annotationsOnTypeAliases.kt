// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// WITH_STDLIB
import kotlin.annotation.AnnotationTarget.*

annotation class NoTarget

@Target(CLASS)
annotation class IrrelevantTarget

@Target(TYPEALIAS)
annotation class TypealiasTarget

<!WRONG_ANNOTATION_TARGET!>@NoTarget<!>
<!WRONG_ANNOTATION_TARGET!>@IrrelevantTarget<!>
@TypealiasTarget
typealias Test = String

/* GENERATED_FIR_TAGS: annotationDeclaration, typeAliasDeclaration */
