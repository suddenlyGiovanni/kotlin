/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package plugins

import capitalize
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Usage
import org.gradle.api.component.SoftwareComponentFactory
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.provider.Provider
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension
import org.gradle.plugins.signing.SigningPlugin
import java.util.*
import javax.inject.Inject

class KotlinBuildPublishingPlugin @Inject constructor(
    private val componentFactory: SoftwareComponentFactory,
) : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        apply<MavenPublishPlugin>()

        val publishedRuntime = configurations.maybeCreate(RUNTIME_CONFIGURATION).apply {
            isCanBeConsumed = false
            isCanBeResolved = false
            attributes {
                attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
            }
        }

        val publishedCompile = configurations.maybeCreate(COMPILE_CONFIGURATION).apply {
            isCanBeConsumed = false
            isCanBeResolved = false
            attributes {
                attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_API))
            }
        }

        val kotlinLibraryComponent = componentFactory.adhoc(ADHOC_COMPONENT_NAME)
        components.add(kotlinLibraryComponent)
        kotlinLibraryComponent.addVariantsFromConfiguration(publishedCompile) { mapToMavenScope("compile") }
        kotlinLibraryComponent.addVariantsFromConfiguration(publishedRuntime) { mapToMavenScope("runtime") }

        pluginManager.withPlugin("java-base") {
            val runtimeElements by configurations
            val apiElements by configurations

            publishedRuntime.extendsFrom(runtimeElements)
            publishedCompile.extendsFrom(apiElements)

            kotlinLibraryComponent.addVariantsFromConfiguration(runtimeElements) {
                mapToMavenScope("runtime")

                if (configurationVariant.artifacts.any { JavaBasePlugin.UNPUBLISHABLE_VARIANT_ARTIFACTS.contains(it.type) }) {
                    skip()
                }
            }
        }

        configure<PublishingExtension> {
            publications {
                create<MavenPublication>(project.mainPublicationName) {
                    from(kotlinLibraryComponent)

                    configureKotlinPomAttributes(project)
                }
            }
        }
        configureDefaultPublishing()
    }

    companion object {
        const val DEFAULT_MAIN_PUBLICATION_NAME = "Main"
        const val MAIN_PUBLICATION_NAME_PROPERTY = "MainPublicationName"
        const val REPOSITORY_NAME = "Maven"
        const val ADHOC_COMPONENT_NAME = "kotlinLibrary"

        const val COMPILE_CONFIGURATION = "publishedCompile"
        const val RUNTIME_CONFIGURATION = "publishedRuntime"
    }
}

var Project.mainPublicationName: String
    get() {
        return if (project.extra.has(KotlinBuildPublishingPlugin.MAIN_PUBLICATION_NAME_PROPERTY))
            project.extra.get(KotlinBuildPublishingPlugin.MAIN_PUBLICATION_NAME_PROPERTY) as String
        else KotlinBuildPublishingPlugin.DEFAULT_MAIN_PUBLICATION_NAME
    }
    set(value) {
        project.extra.set(KotlinBuildPublishingPlugin.MAIN_PUBLICATION_NAME_PROPERTY, value)
    }

private fun humanReadableName(name: String) =
    name.split("-").joinToString(separator = " ") { it.capitalize(Locale.ROOT) }

fun MavenPublication.configureKotlinPomAttributes(
    project: Project,
    explicitDescription: String? = null,
    packaging: String = "jar",
    explicitName: String? = null,
) = configureKotlinPomAttributes(project, project.provider { explicitDescription }, packaging, project.provider { explicitName })

fun MavenPublication.configureKotlinPomAttributes(
    project: Project,
    explicitDescription: Provider<String>,
    packaging: String = "jar",
    explicitName: Provider<String>,
) {
    val publication = this
    pom {
        this.packaging = packaging
        name.set(explicitName.orElse(humanReadableName(publication.artifactId)))
        description.set(explicitDescription.orElse(project.description ?: humanReadableName(publication.artifactId)))
        url.set("https://kotlinlang.org/")
        licenses {
            license {
                name.set("Apache-2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        scm {
            url.set("https://github.com/JetBrains/kotlin")
            connection.set("scm:git:https://github.com/JetBrains/kotlin.git")
            developerConnection.set("scm:git:https://github.com/JetBrains/kotlin.git")
        }
        developers {
            developer {
                name.set("Kotlin Team")
                organization.set("JetBrains")
                organizationUrl.set("https://www.jetbrains.com")
            }
        }
    }
}

val Project.signLibraryPublication: Boolean
    get() = project.providers.gradleProperty("signingRequired").orNull?.toBoolean()
        ?: project.providers.gradleProperty("kotlin.build.signing-required").orNull?.toBoolean()
        ?: false

// overload for Groovy
fun Project.configureDefaultPublishing() = configureDefaultPublishing(signingRequired = signLibraryPublication)

fun Project.configureDefaultPublishing(
    signingRequired: Boolean = signLibraryPublication,
) {
    configure<PublishingExtension> {
        repositories {
            maven {
                name = KotlinBuildPublishingPlugin.REPOSITORY_NAME

                val repo: String? = project.properties["kotlin.build.deploy-repo"]?.toString() ?: project.properties["deploy-repo"]?.toString()

                val deployRepoUrl = (project.properties["kotlin.build.deploy-url"] ?: project.properties["deploy-url"])?.toString()?.takeIf { it.isNotBlank() }

                val repoUrl: String by extra(
                    (deployRepoUrl ?: "file://${project.rootProject.layout.buildDirectory.dir("repo").get().asFile}")
                )

                val username: String? by extra(
                    project.properties["kotlin.build.deploy-username"]?.toString() ?: project.properties["kotlin.${repo}.user"]?.toString()
                )
                val password: String? by extra(
                    project.properties["kotlin.build.deploy-password"]?.toString() ?: project.properties["kotlin.${repo}.password"]?.toString()
                )

                setUrl(repoUrl)
                if (url.scheme != "file" && username != null && password != null) {
                    credentials {
                        this.username = username
                        this.password = password
                    }
                }
            }
        }
    }

    if (signingRequired) {
        apply<SigningPlugin>()
        configureSigning()
    }


    tasks.register("install") {
        group = "publishing"
        dependsOn(tasks.named("publishToMavenLocal"))
    }.also {
        rootProject.tasks.named("mvnInstall").configure {
            dependsOn(it)
        }
    }
}

private fun Project.getSensitiveProperty(name: String): String? {
    return project.findProperty(name) as? String ?: System.getenv(name)
}

private fun Project.configureSigning() {
    configure<SigningExtension> {
        sign(extensions.getByType<PublishingExtension>().publications) // all publications

        val signKeyId = project.getSensitiveProperty("signKeyId")
        if (!signKeyId.isNullOrBlank()) {
            val signKeyPrivate = project.getSensitiveProperty("signKeyPrivate") ?: error("Parameter `signKeyPrivate` not found")
            val signKeyPassphrase = project.getSensitiveProperty("signKeyPassphrase") ?: error("Parameter `signKeyPassphrase` not found")
            useInMemoryPgpKeys(signKeyId, signKeyPrivate, signKeyPassphrase)
        } else {
            useGpgCmd()
        }
    }
    // Workaround for https://github.com/gradle/gradle/issues/15568
    tasks.withType<AbstractPublishToMaven>().configureEach {
        mustRunAfter(tasks.withType<Sign>())
    }
}
