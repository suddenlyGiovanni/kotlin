plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(intellijCore())
    implementation(project(":compiler:psi:psi-api"))
    api(project(":analysis:analysis-api"))
    api(project(":analysis:analysis-api-platform-interface"))
    api(project(":analysis:analysis-api-fir"))
    api(project(":analysis:low-level-api-fir"))
    api(project(":analysis:symbol-light-classes"))
    api(project(":analysis:decompiled:light-classes-for-decompiled"))
    api(project(":analysis:analysis-api-standalone:analysis-api-standalone-base"))
    implementation(project(":analysis:analysis-api-standalone:analysis-api-fir-standalone-base"))
    testImplementation(projectTests(":analysis:analysis-api-fir"))
    testImplementation(projectTests(":analysis:analysis-api-impl-base"))
    testImplementation(projectTests(":analysis:analysis-test-framework"))
    testImplementation(projectTests(":analysis:low-level-api-fir"))

    testImplementation(kotlinTest("junit"))
    testCompileOnly(toolsJarApi())
    testRuntimeOnly(toolsJar())
    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

kotlin {
    explicitApi()

    compilerOptions {
        optIn.add("org.jetbrains.kotlin.analysis.api.KaPlatformInterface")
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

projectTest(jUnitMode = JUnitMode.JUnit5) {
    dependsOn(":dist")
    workingDir = rootDir
    useJUnitPlatform()
}.also { confugureFirPluginAnnotationsDependency(it) }

testsJar()
