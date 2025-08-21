package org.morecup.pragmaddd.analyzer

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Pragma DDD 分析器插件
 *
 * 使用方式：
 * ```kotlin
 * plugins {
 *     id("org.morecup.pragma-ddd-analyzer")
 * }
 *
 * // 可选配置
 * pragmaDddAnalyzer {
 *     verbose.set(true)
 *     outputFormat.set("JSON") // JSON 或 TXT
 *     outputFile.set("build/reports/pragma-ddd-analysis.json")
 * }
 * ```
 */
class PragmaDddAnalyzerPlugin : Plugin<Project> {

    companion object {
        // 获取插件版本号
        private fun getPluginVersion(): String {
            return try {
                // 方法1: 尝试从 MANIFEST.MF 中读取版本
                val clazz = PragmaDddAnalyzerPlugin::class.java
                val packageInfo = clazz.`package`
                val manifestVersion = packageInfo?.implementationVersion

                if (!manifestVersion.isNullOrBlank()) {
                    return manifestVersion
                }

                // 方法2: 尝试从资源文件中读取版本
                val versionResource = clazz.getResourceAsStream("/pragma-ddd-analyzer-version.properties")
                if (versionResource != null) {
                    val props = java.util.Properties()
                    props.load(versionResource)
                    val resourceVersion = props.getProperty("version")
                    if (!resourceVersion.isNullOrBlank()) {
                        return resourceVersion
                    }
                }

                // 方法3: 使用默认版本
                "1.0.0"
            } catch (e: Exception) {
                // 如果无法获取版本，使用默认版本
                "1.0.0"
            }
        }
    }

    override fun apply(project: Project) {
        // 创建扩展配置
        val extension = project.extensions.create("pragmaDddAnalyzer", PragmaDddAnalyzerExtension::class.java)

        // 注册分析任务
        val analyzeTask = project.tasks.register("analyzeDddClasses", AnalyzeDddClassesTask::class.java) { task ->
            task.group = "pragma-ddd"
            task.description = "分析 @AggregateRoot 注解的类的属性访问情况"
            task.extension = extension
        }

        // 自动集成到构建流程中
        project.afterEvaluate {
            configureTaskDependencies(project, analyzeTask.get())
        }
    }

    private fun configureTaskDependencies(project: Project, analyzeTask: AnalyzeDddClassesTask) {
        // 查找编译任务
        val compileKotlinTask = project.tasks.findByName("compileKotlin")
        val compileJavaTask = project.tasks.findByName("compileJava")
        val processResourcesTask = project.tasks.findByName("processResources")

        // 分析任务依赖于编译任务
        compileKotlinTask?.let { analyzeTask.dependsOn(it) }
        compileJavaTask?.let { analyzeTask.dependsOn(it) }

        // 让 processResources 任务依赖分析任务，确保 JSON 文件在资源处理前生成
        processResourcesTask?.dependsOn(analyzeTask)

        // 让 jar 任务也依赖分析任务，确保打包前完成分析
        project.tasks.findByName("jar")?.dependsOn(analyzeTask)

        // 如果有测试任务，也让它依赖分析任务
        project.tasks.findByName("test")?.dependsOn(analyzeTask)
    }



}