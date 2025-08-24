package org.morecup.pragmaddd.analyzer

import org.objectweb.asm.ClassReader
import java.io.File
import java.io.FileInputStream

/**
 * DDD 领域对象文档分析器
 * 分析编译后的字节码并结合源代码，收集带有 @AggregateRoot、@DomainEntity、@ValueObject 注解的类的详细信息
 */
class DomainDocumentationAnalyzer {
    
    companion object {
        private const val AGGREGATE_ROOT_ANNOTATION = "Lorg/morecup/pragmaddd/core/annotation/AggregateRoot;"
        private const val DOMAIN_ENTITY_ANNOTATION = "Lorg/morecup/pragmaddd/core/annotation/DomainEntity;"
        private const val VALUE_OBJECT_ANNOTATION = "Lorg/morecup/pragmaddd/core/annotation/ValueObject;"
    }
    
    /**
     * 分析目录中的所有类文件，收集文档信息
     */
    fun analyzeDirectory(
        compiledClassesDir: File,
        sourceDir: File,
        sourceSet: String
    ): List<ClassDocumentationInfo> {
        // 构建源文件映射
        val sourceFileMap = buildSourceFileMap(sourceDir)
        
        val results = mutableListOf<ClassDocumentationInfo>()
        
        compiledClassesDir.walkTopDown()
            .filter { it.isFile && it.extension == "class" }
            .forEach { classFile ->
                analyzeClassFile(classFile, sourceFileMap, sourceSet)?.let { 
                    results.add(it) 
                }
            }
        
        return results
    }
    
    /**
     * 分析单个类文件
     */
    private fun analyzeClassFile(
        classFile: File,
        sourceFileMap: Map<String, File>,
        sourceSet: String
    ): ClassDocumentationInfo? {
        return try {
            FileInputStream(classFile).use { input ->
                val classReader = ClassReader(input)
                val visitor = DocumentationClassVisitor(sourceSet, sourceFileMap)
                classReader.accept(visitor, ClassReader.EXPAND_FRAMES)
                visitor.getResult()
            }
        } catch (e: Exception) {
            println("分析类文件失败: ${classFile.absolutePath}, 错误: ${e.message}")
            null
        }
    }
    
    /**
     * 构建类名到源文件的映射
     */
    private fun buildSourceFileMap(sourceDir: File): Map<String, File> {
        val sourceFileMap = mutableMapOf<String, File>()
        
        if (!sourceDir.exists()) {
            return sourceFileMap
        }
        
        sourceDir.walkTopDown()
            .filter { it.isFile && (it.extension == "java" || it.extension == "kt") }
            .forEach { sourceFile ->
                try {
                    val className = extractClassNameFromSourceFile(sourceFile, sourceDir)
                    if (className != null) {
                        sourceFileMap[className] = sourceFile
                    }
                } catch (e: Exception) {
                    // 忽略无法解析的源文件
                }
            }
        
        return sourceFileMap
    }
    
    /**
     * 从源文件中提取类名
     */
    private fun extractClassNameFromSourceFile(sourceFile: File, sourceDir: File): String? {
        try {
            val content = sourceFile.readText()
            
            // 提取包名
            val packageRegex = Regex("package\\s+([\\w.]+)")
            val packageMatch = packageRegex.find(content)
            val packageName = packageMatch?.groupValues?.get(1) ?: ""
            
            // 提取类名（支持 class, interface, enum, object）
            val classRegex = Regex("(?:public\\s+|private\\s+|protected\\s+|internal\\s+)?(?:abstract\\s+|final\\s+|open\\s+)?(?:class|interface|enum|object)\\s+(\\w+)")
            val classMatch = classRegex.find(content)
            val className = classMatch?.groupValues?.get(1)
            
            return if (className != null) {
                if (packageName.isNotEmpty()) {
                    "$packageName.$className"
                } else {
                    className
                }
            } else {
                null
            }
        } catch (e: Exception) {
            return null
        }
    }
    
    /**
     * 检查类是否有DDD注解（快速检查，用于过滤）
     */
    private fun hasDddAnnotation(classFile: File): Boolean {
        return try {
            FileInputStream(classFile).use { input ->
                val classReader = ClassReader(input)
                var hasDddAnnotation = false
                
                classReader.accept(object : org.objectweb.asm.ClassVisitor(org.objectweb.asm.Opcodes.ASM9) {
                    override fun visitAnnotation(descriptor: String, visible: Boolean): org.objectweb.asm.AnnotationVisitor? {
                        when (descriptor) {
                            AGGREGATE_ROOT_ANNOTATION,
                            DOMAIN_ENTITY_ANNOTATION,
                            VALUE_OBJECT_ANNOTATION -> hasDddAnnotation = true
                        }
                        return super.visitAnnotation(descriptor, visible)
                    }
                }, org.objectweb.asm.ClassReader.SKIP_CODE or org.objectweb.asm.ClassReader.SKIP_DEBUG)
                
                hasDddAnnotation
            }
        } catch (e: Exception) {
            false
        }
    }
}