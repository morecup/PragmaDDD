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
        val classesTask = project.tasks.findByName("classes")
        
        // 分析任务依赖于编译任务
        compileKotlinTask?.let { analyzeTask.dependsOn(it) }
        compileJavaTask?.let { analyzeTask.dependsOn(it) }
        
        // 让 classes 任务依赖分析任务，这样 build 时会自动运行分析
        classesTask?.dependsOn(analyzeTask)
        
        // 查找可能的 AspectJ 相关任务
        val aspectjTask = project.tasks.findByName("compileAspect") 
            ?: project.tasks.findByName("aspectjWeave")
            ?: project.tasks.findByName("aspectjCompile")
        
        // 如果有 AspectJ 任务，让它依赖于分析任务
        aspectjTask?.dependsOn(analyzeTask)
        
        // 让 jar 任务也依赖分析任务，确保打包前完成分析
        project.tasks.findByName("jar")?.dependsOn(analyzeTask)
        
        // 如果有测试任务，也让它依赖分析任务
        project.tasks.findByName("test")?.dependsOn(analyzeTask)
    }
}