
plugins {
    kotlin("jvm") version "2.1.0"
    `java-gradle-plugin`
    `maven-publish`
}

group = "org.morecup.pragmaddd"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("org.ow2.asm:asm:9.6")
    implementation("org.ow2.asm:asm-commons:9.6")
    implementation("org.ow2.asm:asm-util:9.6")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")

    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.0")

    // Gradle API for plugin development
    implementation(gradleApi())
    
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.assertj:assertj-core:3.24.2")
}

gradlePlugin {
    plugins {
        create("pragmaDddAnalyzer") {
            id = "org.morecup.pragmaddd.pragma-ddd-analyzer"
            implementationClass = "org.morecup.pragmaddd.analyzer.PragmaDddAnalyzerPlugin"
            displayName = "Pragma DDD Analyzer"
            description = "Analyzes DDD aggregate root classes for property access patterns"
        }
    }
}

tasks.test {
    useJUnitPlatform()
}