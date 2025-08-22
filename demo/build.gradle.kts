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
    // Note: Output directory and JSON file naming are now fixed and not configurable
    // Fixed path: build/generated/pragmaddd/main/resources/META-INF/pragma-ddd-analyzer/domain-analyzer.json
}

