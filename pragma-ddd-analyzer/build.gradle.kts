
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
    implementation("org.ow2.asm:asm:9.8")
    implementation("org.ow2.asm:asm-commons:9.8")
    implementation("org.ow2.asm:asm-util:9.8")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")

    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.0")

    // 源码解析依赖
    implementation("com.github.javaparser:javaparser-core:3.25.7")
    implementation("com.github.javaparser:javaparser-symbol-solver-core:3.25.7")

    // Kotlin 编译器依赖，用于解析 KDoc
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.1.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.1.0")

    // Kotlin 元数据解析依赖
    implementation("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.7.0")

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