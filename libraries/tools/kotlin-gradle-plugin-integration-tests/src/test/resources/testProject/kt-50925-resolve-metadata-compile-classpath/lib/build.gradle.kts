plugins {
    kotlin("multiplatform").version("<pluginMarkerVersion>")
    `maven-publish`
}

group = "kt50925"
version = "1.0"

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    jvm()
    linuxX64()
}

publishing {
    repositories {
        maven("$rootDir/../repo")
    }
}
