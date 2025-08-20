plugins {
    kotlin("jvm")
    id("org.morecup.pragmaddd.pragma-ddd-analyzer")
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(project(":pragma-ddd-core"))
}

//// 可选的插件配置
//pragmaDddAnalyzer {
//    verbose.set(true)
//    outputFormat.set("JSON")
//    outputFile.set("build/ddd-analysis-report.json")
//}
