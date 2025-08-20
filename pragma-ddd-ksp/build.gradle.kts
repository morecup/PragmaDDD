import org.gradle.kotlin.dsl.implementation

plugins {
    id("kotlin-jvm")
    id("com.google.devtools.ksp") version "1.8.0-1.0.9"
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.google.devtools.ksp:symbol-processing-api:1.8.0-1.0.9")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}