package org.morecup.pragmaddd.analyzer

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.*
import java.io.File
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature

/**
 * 分析 DDD 类的 Gradle 任务
 */
open class AnalyzeDddClassesTask : DefaultTask() {

    @get:Internal
    lateinit var extension: PragmaDddAnalyzerExtension
    
    @get:Input
    var sourceSetName: String = "main"

    @get:Input
    val verbose: Boolean
        get() = extension.verbose.getOrElse(false)

    @get:OutputFile
    val outputFile: File
        get() {
            val defaultPath = if (sourceSetName == "main") {
                "build/generated/pragmaddd/main/resources/META-INF/pragma-ddd-analyzer/domain-analyzer.json"
            } else {
                "build/generated/pragmaddd/$sourceSetName/resources/META-INF/pragma-ddd-analyzer/domain-analyzer-$sourceSetName.json"
            }
            return project.file(defaultPath)
        }

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val inputClassFiles: FileTree
        get() {
            val classPaths = extension.classPaths.getOrElse(emptySet()).ifEmpty {
                // 根据 sourceSet 自动检测可能的编译输出目录
                val possiblePaths = mutableSetOf<String>()

                // Kotlin 编译输出
                val kotlinPath = "build/classes/kotlin/$sourceSetName"
                if (project.file(kotlinPath).exists()) {
                    possiblePaths.add(kotlinPath)
                }

                // Java 编译输出  
                val javaPath = "build/classes/java/$sourceSetName"
                if (project.file(javaPath).exists()) {
                    possiblePaths.add(javaPath)
                }

                // 如果都不存在，使用默认路径
                if (possiblePaths.isEmpty()) {
                    possiblePaths.addAll(setOf(
                        "build/classes/kotlin/$sourceSetName",
                        "build/classes/java/$sourceSetName"
                    ))
                }

                possiblePaths
            }

            // 创建包含所有 .class 文件的 FileTree
            return project.fileTree(mapOf("includes" to listOf("**/*.class"))).apply {
                classPaths.forEach { classPath ->
                    val dir = project.file(classPath)
                    if (dir.exists()) {
                        from(dir)
                    }
                }
            }
        }

    @TaskAction
    fun analyze() {
        println("开始分析 DDD 类的字节码...")

        val analyzer = AggregateRootAnalyzer()
        val results = mutableListOf<ClassAnalysisResult>()

        // 获取输入的 class 文件目录
        val classDirectories = inputClassFiles.files
            .map { it.parentFile }
            .distinct()
            .filter { it.exists() && it.isDirectory }

        if (classDirectories.isEmpty()) {
            println("警告：没有找到编译后的 class 文件目录")
            // 仍然输出空结果文件
            outputResults(results)
            return
        }

        classDirectories.forEach { dir ->
            println("正在分析目录: ${dir.absolutePath}")
            if (verbose) {
                println("  正在读取编译后的字节码文件")
            }
            val dirResults = analyzer.analyzeDirectory(dir)
            results.addAll(dirResults)
            println("  在目录 ${dir.name} 中找到 ${dirResults.size} 个 @AggregateRoot 类")
        }

        // 输出结果
        outputResults(results)

        if (verbose) {
            println("分析完成，找到 ${results.size} 个 @AggregateRoot 类")
            results.forEach { result ->
                println("类: ${result.className}")
                result.methods.forEach { method ->
                    println("  方法: ${method.methodName}")
                    if (method.accessedProperties.isNotEmpty()) {
                        println("    访问属性: ${method.accessedProperties}")
                    }
                    if (method.modifiedProperties.isNotEmpty()) {
                        println("    修改属性: ${method.modifiedProperties}")
                    }
                    if (method.calledMethods.isNotEmpty()) {
                        println("    调用方法: ${method.calledMethods.map { "${it.methodName}(调用${it.callCount}次)" }}")
                    }
                }
            }
        }
    }

    private fun outputResults(results: List<ClassAnalysisResult>) {
        outputFile.parentFile.mkdirs()

        // 固定为JSON格式输出
        val mapper = jacksonObjectMapper().apply {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
        outputFile.writeText(mapper.writeValueAsString(results))

        println("分析结果已保存到: ${outputFile.absolutePath}")
    }


}