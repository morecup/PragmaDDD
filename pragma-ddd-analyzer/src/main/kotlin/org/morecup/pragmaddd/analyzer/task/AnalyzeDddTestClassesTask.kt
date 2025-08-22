package org.morecup.pragmaddd.analyzer.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.morecup.pragmaddd.analyzer.bytecode.BytecodeAnalyzer

/**
 * Gradle 任务：分析测试源码中的 DDD 类并生成 JSON 文件
 */
abstract class AnalyzeDddTestClassesTask : DefaultTask() {
    
    @get:Input
    abstract val outputFileName: Property<String>
    
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty
    
    init {
        description = "Analyze DDD annotated test classes and generate JSON metadata"
        group = "pragma-ddd"
        
        // 设置默认值
        outputFileName.convention("ddd-analysis-test.json")
        outputDirectory.convention(project.layout.buildDirectory.dir("generated/ddd-analysis"))
        
        // 依赖于测试编译任务
        dependsOn("compileTestKotlin")
    }
    
    @TaskAction
    fun analyzeDddTestClasses() {
        val analyzer = BytecodeAnalyzer()
        val outputDir = outputDirectory.get().asFile
        val fileName = outputFileName.get()
        val outputPath = outputDir.resolve(fileName).absolutePath
        
        // 确保输出目录存在
        outputDir.mkdirs()
        
        // 获取测试编译后的类路径
        val classpathEntries = getTestClasspathEntries()
        
        if (classpathEntries.isNotEmpty()) {
            // 分析并生成 JSON 文件
            analyzer.analyzeAndGenerateJson(classpathEntries, outputPath, "test")
            logger.lifecycle("DDD test analysis completed. Generated file: $outputPath")
        } else {
            logger.warn("No compiled test classes found. Make sure to run 'compileTestKotlin' first.")
        }
    }
    
    /**
     * 获取测试编译后的类路径
     */
    private fun getTestClasspathEntries(): List<String> {
        val entries = mutableListOf<String>()
        
        // 添加测试源码编译输出目录
        val testClassesDir = project.layout.buildDirectory.dir("classes/kotlin/test").get().asFile
        if (testClassesDir.exists()) {
            entries.add(testClassesDir.absolutePath)
        }
        
        // 添加 Java 测试编译输出目录（如果存在）
        val javaTestClassesDir = project.layout.buildDirectory.dir("classes/java/test").get().asFile
        if (javaTestClassesDir.exists()) {
            entries.add(javaTestClassesDir.absolutePath)
        }
        
        return entries
    }
}