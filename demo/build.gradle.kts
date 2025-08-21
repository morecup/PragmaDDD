plugins {
    kotlin("jvm")
    id("org.morecup.pragmaddd.pragma-ddd-analyzer")
//    id("io.freefair.aspectj.post-compile-weaving") version "8.4"
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(project(":pragma-ddd-core"))
}

// 可选的插件配置
pragmaDddAnalyzer {
    verbose.set(false)  // 关闭详细输出，减少日志噪音
    outputFormat.set("JSON")
    aspectJMode.set(org.morecup.pragmaddd.analyzer.AspectJMode.ENABLED)  // 启用AspectJ织入
}
