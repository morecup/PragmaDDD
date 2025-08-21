package org.morecup.pragmaddd.analyzer

import org.objectweb.asm.*
import org.objectweb.asm.commons.AnalyzerAdapter
import java.io.File
import java.io.FileInputStream
import java.util.jar.JarFile

/**
 * AggregateRoot 类分析器
 * 分析编译后的字节码中的属性访问情况
 */
class AggregateRootAnalyzer {
    
    companion object {
        private const val AGGREGATE_ROOT_ANNOTATION = "Lorg/morecup/pragmaddd/core/annotation/AggregateRoot;"
    }
    
    /**
     * 分析单个类文件
     */
    fun analyzeClass(classFile: File): ClassAnalysisResult? {
        return try {
            FileInputStream(classFile).use { input ->
                val classReader = ClassReader(input)
                val visitor = AggregateRootClassVisitor()
                classReader.accept(visitor, ClassReader.EXPAND_FRAMES)
                visitor.getResult()
            }
        } catch (e: Exception) {
            println("分析类文件失败: ${classFile.absolutePath}, 错误: ${e.message}")
            null
        }
    }
    
    /**
     * 分析 JAR 文件中的所有类
     */
    fun analyzeJar(jarFile: File): List<ClassAnalysisResult> {
        val results = mutableListOf<ClassAnalysisResult>()
        
        JarFile(jarFile).use { jar ->
            jar.entries().asSequence()
                .filter { it.name.endsWith(".class") }
                .forEach { entry ->
                    try {
                        jar.getInputStream(entry).use { input ->
                            val classReader = ClassReader(input)
                            val visitor = AggregateRootClassVisitor()
                            classReader.accept(visitor, ClassReader.EXPAND_FRAMES)
                            visitor.getResult()?.let { results.add(it) }
                        }
                    } catch (e: Exception) {
                        println("分析 JAR 中的类失败: ${entry.name}, 错误: ${e.message}")
                    }
                }
        }
        
        return results
    }
    
    /**
     * 分析目录中的所有类文件
     */
    fun analyzeDirectory(directory: File): List<ClassAnalysisResult> {
        val results = mutableListOf<ClassAnalysisResult>()
        
        directory.walkTopDown()
            .filter { it.isFile && it.extension == "class" }
            .forEach { classFile ->
                analyzeClass(classFile)?.let { results.add(it) }
            }
        
        return results
    }
}