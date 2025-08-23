plugins {
    kotlin("jvm")
    id("io.freefair.aspectj.post-compile-weaving") version "8.4"
    id("org.morecup.pragmaddd.pragma-ddd-analyzer")
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(project(":pragma-ddd-core"))
    aspect(project(":pragma-ddd-aspect"))
}

// 可选的插件配置
pragmaDddAnalyzer {
    verbose.set(true)  // 启用详细输出
}