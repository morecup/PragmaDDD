import org.gradle.kotlin.dsl.implementation

plugins {
    id("kotlin-jvm")
    id("com.google.devtools.ksp") version "2.1.0-1.0.29"
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.google.devtools.ksp:symbol-processing-api:2.1.0-1.0.29")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}