package org.morecup.pragmaddd.analyzer.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.morecup.pragmaddd.analyzer.bytecode.BytecodeAnalyzer

/**
 * Gradle 任务：分析 DDD 类并生成 JSON 文件
 */
abstract class AnalyzeDddClassesTask : DefaultTask() {
    
    @get:Input
    abstract val outputFileName: Property<String>
    
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty
    
    init {
        description = "Analyze DDD annotated classes and generate JSON metadata"
        group = "pragma-ddd"
        
        // 设置默认值
        outputFileName.convention("ddd-analysis.json")
        outputDirectory.convention(project.layout.buildDirectory.dir("generated/ddd-analysis"))
        
        // 依赖于编译任务
        dependsOn("compileKotlin")
    }
    
    @TaskAction
    fun analyzeDddClasses() {
        val analyzer = BytecodeAnalyzer()
        val outputDir = outputDirectory.get().asFile
        val fileName = outputFileName.get()
        val outputPath = outputDir.resolve(fileName).absolutePath
        
        // 确保输出目录存在
        outputDir.mkdirs()
        
        // 获取编译后的类路径
        val classpathEntries = getClasspathEntries()
        
        if (classpathEntries.isNotEmpty()) {
            // 分析并生成 JSON 文件
            analyzer.analyzeAndGenerateJson(classpathEntries, outputPath, "main")
            logger.lifecycle("DDD analysis completed. Generated file: $outputPath")
        } else {
            logger.warn("No compiled classes found. Make sure to run 'compileKotlin' first.")
        }
    }
    
    /**
     * 获取编译后的类路径
     */
    private fun getClasspathEntries(): List<String> {
        val entries = mutableListOf<String>()
        
        // 添加主源码编译输出目录
        val mainClassesDir = project.layout.buildDirectory.dir("classes/kotlin/main").get().asFile
        if (mainClassesDir.exists()) {
            entries.add(mainClassesDir.absolutePath)
        }
        
        // 添加 Java 编译输出目录（如果存在）
        val javaClassesDir = project.layout.buildDirectory.dir("classes/java/main").get().asFile
        if (javaClassesDir.exists()) {
            entries.add(javaClassesDir.absolutePath)
        }
        
        return entries
    }
}