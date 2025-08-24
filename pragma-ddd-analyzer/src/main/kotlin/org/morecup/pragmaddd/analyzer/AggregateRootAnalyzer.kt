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
        private const val DOMAIN_ENTITY_ANNOTATION = "Lorg/morecup/pragmaddd/core/annotation/DomainEntity;"
        private const val VALUE_OBJECT_ANNOTATION = "Lorg/morecup/pragmaddd/core/annotation/ValueObject;"
    }
    
    /**
     * DDD 注解扫描器，用于第一阶段收集所有 DDD 注解类
     */
    private class DddAnnotationScanner : ClassVisitor(Opcodes.ASM9) {
        private var className: String = ""
        private val annotations = mutableSetOf<String>()
        
        override fun visit(
            version: Int,
            access: Int,
            name: String,
            signature: String?,
            superName: String?,
            interfaces: Array<out String>?
        ) {
            this.className = name.replace('/', '.')
            super.visit(version, access, name, signature, superName, interfaces)
        }
        
        override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
            when (descriptor) {
                AGGREGATE_ROOT_ANNOTATION -> annotations.add("AggregateRoot")
                DOMAIN_ENTITY_ANNOTATION -> annotations.add("DomainEntity")
                VALUE_OBJECT_ANNOTATION -> annotations.add("ValueObject")
            }
            return super.visitAnnotation(descriptor, visible)
        }
        
        fun getResult(): Pair<String, Set<String>>? {
            return if (annotations.isNotEmpty()) {
                className to annotations.toSet()
            } else {
                null
            }
        }
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
     * 分析目录中的所有类文件（两阶段分析）
     */
    fun analyzeDirectory(directory: File): List<ClassAnalysisResult> {
        // 第一阶段：扫描所有 DDD 注解类
        val dddAnnotatedClasses = scanDddAnnotatedClasses(directory)
        
        // 第二阶段：详细分析 AggregateRoot 类
        val results = mutableListOf<ClassAnalysisResult>()
        
        directory.walkTopDown()
            .filter { it.isFile && it.extension == "class" }
            .forEach { classFile ->
                analyzeClassWithDddContext(classFile, dddAnnotatedClasses)?.let { 
                    results.add(it) 
                }
            }
        
        return results
    }
    
    /**
     * 第一阶段：扫描所有 DDD 注解类
     */
    private fun scanDddAnnotatedClasses(directory: File): Map<String, Set<String>> {
        val dddClasses = mutableMapOf<String, Set<String>>()
        
        directory.walkTopDown()
            .filter { it.isFile && it.extension == "class" }
            .forEach { classFile ->
                try {
                    FileInputStream(classFile).use { input ->
                        val classReader = ClassReader(input)
                        val scanner = DddAnnotationScanner()
                        classReader.accept(scanner, ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG)
                        scanner.getResult()?.let { (className, annotations) ->
                            dddClasses[className] = annotations
                        }
                    }
                } catch (e: Exception) {
                    println("扫描 DDD 注解失败: ${classFile.absolutePath}, 错误: ${e.message}")
                }
            }
        
        return dddClasses
    }
    
    /**
     * 第二阶段：带 DDD 上下文的详细分析
     */
    private fun analyzeClassWithDddContext(classFile: File, dddAnnotatedClasses: Map<String, Set<String>>): ClassAnalysisResult? {
        return try {
            FileInputStream(classFile).use { input ->
                val classReader = ClassReader(input)
                val visitor = AggregateRootClassVisitor(dddAnnotatedClasses)
                classReader.accept(visitor, ClassReader.EXPAND_FRAMES)
                visitor.getResult()
            }
        } catch (e: Exception) {
            println("分析类文件失败: ${classFile.absolutePath}, 错误: ${e.message}")
            null
        }
    }
}