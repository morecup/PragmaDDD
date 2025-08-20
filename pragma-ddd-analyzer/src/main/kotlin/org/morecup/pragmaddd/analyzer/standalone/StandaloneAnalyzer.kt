package org.morecup.pragmaddd.analyzer.standalone

import org.morecup.pragmaddd.analyzer.*
import java.io.File

/**
 * 独立的分析器工具，可以直接运行而不依赖 Gradle 插件
 */
object StandaloneAnalyzer {
    
    @JvmStatic
    fun main(args: Array<String>) {
        if (args.isEmpty()) {
            printUsage()
            return
        }
        
        val analyzer = AggregateRootAnalyzer()
        val results = mutableListOf<ClassAnalysisResult>()
        
        args.forEach { path ->
            val file = File(path)
            when {
                file.isFile && file.extension == "class" -> {
                    analyzer.analyzeClass(file)?.let { results.add(it) }
                }
                file.isFile && file.extension == "jar" -> {
                    results.addAll(analyzer.analyzeJar(file))
                }
                file.isDirectory -> {
                    results.addAll(analyzer.analyzeDirectory(file))
                }
                else -> {
                    println("跳过无效路径: $path")
                }
            }
        }
        
        // 输出结果
        printResults(results)
    }
    
    private fun printUsage() {
        println("""
            Pragma DDD 分析器
            
            用法: java -jar pragma-ddd-analyzer.jar <路径1> [路径2] ...
            
            支持的路径类型:
            - .class 文件
            - .jar 文件  
            - 包含 .class 文件的目录
            
            示例:
            java -jar pragma-ddd-analyzer.jar build/classes/kotlin/main
            java -jar pragma-ddd-analyzer.jar MyClass.class
            java -jar pragma-ddd-analyzer.jar myapp.jar
        """.trimIndent())
    }
    
    private fun printResults(results: List<ClassAnalysisResult>) {
        if (results.isEmpty()) {
            println("未找到被 @AggregateRoot 注解的类")
            return
        }
        
        println("分析结果:")
        println("=".repeat(50))
        
        results.forEach { result ->
            println("\n类: ${result.className}")
            println("-".repeat(30))
            
            if (result.methods.isEmpty()) {
                println("  无属性访问")
            } else {
                result.methods.forEach { method ->
                    println("  方法: ${method.methodName}")
                    if (method.accessedProperties.isNotEmpty()) {
                        println("    访问属性: ${method.accessedProperties.joinToString(", ")}")
                    }
                    if (method.modifiedProperties.isNotEmpty()) {
                        println("    修改属性: ${method.modifiedProperties.joinToString(", ")}")
                    }
                }
            }
        }
        
        println("\n总计: 找到 ${results.size} 个 @AggregateRoot 类")
    }
}