// RUN_PIPELINE_TILL: BACKEND
// DISABLE_JAVA_FACADE
// FILE: simulation/State.java

package simulation;

@Retention(RetentionPolicy.RUNTIME)
public @interface State {
    @NotNull @NonNls
    String name();
    String @NotNull [] storages() default {};
}

// FILE: KotlinImporterComponent.kt

package simulation

// KotlinMavenImporter.kt:407:1
@State(
    name = "AutoImportedSourceRoots"
)
class KotlinImporterComponent {
    class State(var directories: List<String> = ArrayList())
}

/* GENERATED_FIR_TAGS: classDeclaration, javaType, nestedClass, primaryConstructor, propertyDeclaration, stringLiteral */
