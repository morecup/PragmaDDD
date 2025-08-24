package org.morecup.pragmaddd.analyzer

import org.morecup.pragmaddd.analyzer.model.*
import org.morecup.pragmaddd.analyzer.source.*
import org.objectweb.asm.ClassReader
import java.io.File
import java.io.FileInputStream

/**
 * 详细的领域对象分析器
 * 结合 ASM 字节码分析和源码文档注释解析，提供完整的类信息
 */
class DetailedDomainObjectAnalyzer {
    
    private val documentationExtractor = CompositeSourceDocumentationExtractor()
    private val sourceFileFinder = SourceFileFinder()
    
    /**
     * 分析目录中的所有 DDD 注解类
     * 
     * @param classDirectory 编译后的 class 文件目录
     * @param projectDir 项目根目录
     * @param sourceSetName 源集名称（main/test）
     * @return 详细分析结果
     */
    fun analyzeDirectory(
        classDirectory: File, 
        projectDir: File, 
        sourceSetName: String
    ): DetailedAnalysisResult {
        val classes = mutableListOf<DetailedClassInfo>()
        val sourceDirs = sourceFileFinder.getSourceDirectories(projectDir, sourceSetName)
        
        // 扫描所有 class 文件
        classDirectory.walkTopDown()
            .filter { it.isFile && it.extension == "class" }
            .forEach { classFile ->
                analyzeClassFile(classFile, sourceDirs, sourceSetName)?.let { classInfo ->
                    classes.add(classInfo)
                }
            }
        
        return DetailedAnalysisResult(
            sourceSetName = sourceSetName,
            classes = classes,
            summary = createSummary(classes)
        )
    }
    
    /**
     * 分析单个类文件
     */
    private fun analyzeClassFile(
        classFile: File, 
        sourceDirs: List<File>, 
        sourceSetName: String
    ): DetailedClassInfo? {
        return try {
            FileInputStream(classFile).use { input ->
                val classReader = ClassReader(input)
                val visitor = DetailedDomainObjectClassVisitor()
                classReader.accept(visitor, ClassReader.EXPAND_FRAMES)
                
                visitor.getResult()?.let { classInfo ->
                    // 合并源码文档注释
                    mergeWithSourceDocumentation(classInfo, sourceDirs, sourceSetName)
                }
            }
        } catch (e: Exception) {
            println("分析类文件失败: ${classFile.absolutePath}, 错误: ${e.message}")
            null
        }
    }
    
    /**
     * 合并源码文档注释信息
     */
    private fun mergeWithSourceDocumentation(
        classInfo: DetailedClassInfo, 
        sourceDirs: List<File>,
        sourceSetName: String
    ): DetailedClassInfo {
        val sourceFile = sourceFileFinder.findSourceFile(classInfo.className, sourceDirs)
        if (sourceFile == null) {
            println("未找到源文件: ${classInfo.className}")
            return classInfo.copy(sourceSetName = sourceSetName)
        }
        
        val sourceDoc = documentationExtractor.extractDocumentation(sourceFile)
        if (sourceDoc == null) {
            println("无法提取源码文档: ${sourceFile.absolutePath}")
            return classInfo.copy(sourceSetName = sourceSetName)
        }
        
        // 合并类文档注释
        val updatedClassInfo = classInfo.copy(
            documentation = sourceDoc.classDocumentation,
            sourceSetName = sourceSetName
        )
        
        // 合并字段文档注释
        val updatedFields = updatedClassInfo.fields.map { field ->
            val fieldDoc = sourceDoc.fieldDocumentations[field.name]
            field.copy(documentation = fieldDoc)
        }
        
        // 合并方法文档注释
        val updatedMethods = updatedClassInfo.methods.map { method ->
            val methodSignature = buildMethodSignature(method)
            val methodDoc = sourceDoc.methodDocumentations[methodSignature]
                ?: sourceDoc.methodDocumentations[method.name] // 简单匹配
            method.copy(documentation = methodDoc)
        }
        
        return updatedClassInfo.copy(
            fields = updatedFields,
            methods = updatedMethods
        )
    }
    
    /**
     * 构建方法签名用于匹配
     */
    private fun buildMethodSignature(method: DetailedMethodInfo): String {
        // 简化的方法签名构建，实际可能需要更复杂的类型解析
        val paramTypes = extractParameterTypes(method.descriptor)
        return "${method.name}(${paramTypes.joinToString(",")})"
    }
    
    /**
     * 从方法描述符中提取参数类型
     */
    private fun extractParameterTypes(descriptor: String): List<String> {
        val paramPart = descriptor.substringAfter('(').substringBefore(')')
        if (paramPart.isEmpty()) return emptyList()
        
        val types = mutableListOf<String>()
        var i = 0
        while (i < paramPart.length) {
            when (paramPart[i]) {
                'Z' -> { types.add("boolean"); i++ }
                'B' -> { types.add("byte"); i++ }
                'C' -> { types.add("char"); i++ }
                'S' -> { types.add("short"); i++ }
                'I' -> { types.add("int"); i++ }
                'J' -> { types.add("long"); i++ }
                'F' -> { types.add("float"); i++ }
                'D' -> { types.add("double"); i++ }
                'L' -> {
                    val end = paramPart.indexOf(';', i)
                    if (end != -1) {
                        val className = paramPart.substring(i + 1, end).replace('/', '.')
                        types.add(className.substringAfterLast('.'))
                        i = end + 1
                    } else {
                        i++
                    }
                }
                '[' -> {
                    var arrayDepth = 0
                    while (i < paramPart.length && paramPart[i] == '[') {
                        arrayDepth++
                        i++
                    }
                    if (i < paramPart.length) {
                        val baseType = when (paramPart[i]) {
                            'Z' -> "boolean"
                            'B' -> "byte"
                            'C' -> "char"
                            'S' -> "short"
                            'I' -> "int"
                            'J' -> "long"
                            'F' -> "float"
                            'D' -> "double"
                            'L' -> {
                                val end = paramPart.indexOf(';', i)
                                if (end != -1) {
                                    val className = paramPart.substring(i + 1, end).replace('/', '.')
                                    i = end
                                    className.substringAfterLast('.')
                                } else {
                                    "Object"
                                }
                            }
                            else -> "Object"
                        }
                        types.add(baseType + "[]".repeat(arrayDepth))
                        i++
                    }
                }
                else -> i++
            }
        }
        return types
    }
    
    /**
     * 创建分析摘要
     */
    private fun createSummary(classes: List<DetailedClassInfo>): AnalysisSummary {
        return AnalysisSummary(
            totalClasses = classes.size,
            aggregateRootCount = classes.count { it.domainObjectType == org.morecup.pragmaddd.analyzer.model.DomainObjectType.AGGREGATE_ROOT },
            domainEntityCount = classes.count { it.domainObjectType == org.morecup.pragmaddd.analyzer.model.DomainObjectType.DOMAIN_ENTITY },
            valueObjectCount = classes.count { it.domainObjectType == org.morecup.pragmaddd.analyzer.model.DomainObjectType.VALUE_OBJECT },
            totalFields = classes.sumOf { it.fields.size },
            totalMethods = classes.sumOf { it.methods.size },
            classesWithDocumentation = classes.count { !it.documentation.isNullOrBlank() },
            fieldsWithDocumentation = classes.sumOf { classInfo -> 
                classInfo.fields.count { !it.documentation.isNullOrBlank() } 
            },
            methodsWithDocumentation = classes.sumOf { classInfo -> 
                classInfo.methods.count { !it.documentation.isNullOrBlank() } 
            }
        )
    }
}
