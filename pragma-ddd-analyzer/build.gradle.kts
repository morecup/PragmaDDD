
plugins {
    kotlin("jvm") version "2.1.0"
    `java-gradle-plugin`
}

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

    // AspectJ support for auto-configuration
    implementation("org.aspectj:aspectjrt:1.9.7")
    implementation("org.aspectj:aspectjtools:1.9.7")
    
    // 预先声明 AspectJ 插件依赖，这样用户项目就能找到它
    implementation("io.freefair.gradle:aspectj-plugin:8.4")

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
            description = "Analyzes DDD aggregate root classes for property access patterns and provides AspectJ integration"
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

// 生成版本属性文件
val generateVersionProperties by tasks.registering {
    val outputDir = file("src/main/resources")
    outputs.dir(outputDir)
    
    doLast {
        val versionFile = file("src/main/resources/pragma-ddd-analyzer-version.properties")
        versionFile.parentFile.mkdirs()
        versionFile.writeText("""
            version=${project.version}
            name=${project.name}
            group=${project.group}
        """.trimIndent())
    }
}

// 确保在编译前生成版本文件
tasks.processResources {
    dependsOn(generateVersionProperties)
}

// 确保版本信息包含在 JAR 的 MANIFEST.MF 中
tasks.jar {
    dependsOn(generateVersionProperties)
    manifest {
        attributes(
            "Implementation-Title" to "Pragma DDD Analyzer",
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to "MoreCup"
        )
    }
}