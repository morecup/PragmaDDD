//import sun.jvmstat.monitor.MonitoredVmUtil.mainClass

plugins {
    kotlin("jvm") version "2.1.0"
    `java-gradle-plugin`
    `maven-publish`
    application
}

group = "org.morecup.pragmaddd"
version = "0.0.1"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    // Kotlin compiler plugin dependencies
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.1.0")
    
    // Kotlin Gradle plugin dependencies
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.0")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api:2.1.0")
    
    // Kotlin reflection for bytecode analysis
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.1.0")
    
    // JSON serialization
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
    
    // Core module for DDD annotations - using local Maven repository
    // implementation(project(":pragma-ddd-core"))

    // Gradle API for plugin development
    implementation(gradleApi())
    
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.1.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
    testImplementation("org.mockito:mockito-core:5.14.2")
}

gradlePlugin {
    plugins {
        create("pragmaDddAnalyzer") {
            id = "org.morecup.pragmaddd.analyzer"
            implementationClass = "org.morecup.pragmaddd.analyzer.PragmaDddAnalyzerPlugin"
            displayName = "Pragma DDD Analyzer"
            description = "Analyzes DDD aggregate root classes for property access patterns"
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

//// Configure application main class
//application {
//    mainClass.set("org.morecup.pragmaddd.analyzer.standalone.StandaloneAnalyzerKt")
//}