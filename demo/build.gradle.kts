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
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
}

// 配置 DDD 分析器
pragmaDddAnalyzer {
    // Use default output directory (build/resources/main)
    jsonFileNaming.set("demo-ddd-analysis")
}

