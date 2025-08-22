plugins {
    id("kotlin-jvm")
    id("org.morecup.pragmaddd.pragma-ddd-analyzer")
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(project(":pragma-ddd-core"))
}

// 配置 DDD 分析器
pragmaDddAnalyzer {
    // Use default output directory (build/resources/main)
    jsonFileNaming.set("demo-ddd-analysis")
}

// 禁用测试任务，因为测试源集中的类只是用于演示DDD分析器的示例类，不是真正的测试
tasks.test {
    enabled = false
}