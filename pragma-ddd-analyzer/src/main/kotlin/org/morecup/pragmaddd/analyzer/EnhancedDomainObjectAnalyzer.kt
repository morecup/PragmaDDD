package org.morecup.pragmaddd.analyzer

import org.morecup.pragmaddd.analyzer.model.*
import org.morecup.pragmaddd.analyzer.source.*
import org.objectweb.asm.ClassReader
import java.io.File
import java.io.FileInputStream

/**
 * 增强的领域对象分析器
 * 结合原有的属性访问分析功能和新的详细信息收集功能
 */
class EnhancedDomainObjectAnalyzer {
    
    private val documentationExtractor = CompositeSourceDocumentationExtractor()
    private val sourceFileFinder = SourceFileFinder()
    private val originalAnalyzer = DomainObjectAnalyzer()
    
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
        // 第一步：使用原有分析器获取属性访问分析结果
        val originalResults = originalAnalyzer.analyzeDirectory(classDirectory)
        
        // 第二步：获取源码目录
        val sourceDirs = sourceFileFinder.getSourceDirectories(projectDir, sourceSetName)
        
        // 第三步：为每个原有结果增强详细信息
        val enhancedClasses = originalResults.map { originalResult ->
            enhanceClassAnalysis(originalResult, classDirectory, sourceDirs, sourceSetName)
        }
        
        return DetailedAnalysisResult(
            sourceSetName = sourceSetName,
            classes = enhancedClasses,
            summary = createSummary(enhancedClasses)
        )
    }
    
    /**
     * 增强单个类的分析结果
     */
    private fun enhanceClassAnalysis(
        originalResult: ClassAnalysisResult,
        classDirectory: File,
        sourceDirs: List<File>,
        sourceSetName: String
    ): DetailedClassInfo {
        // 查找对应的class文件
        val classFile = findClassFile(originalResult.className, classDirectory)
        
        // 使用详细分析器获取结构信息
        val detailedInfo = if (classFile != null) {
            analyzeClassStructure(classFile, sourceDirs, sourceSetName)
        } else {
            null
        }
        
        // 合并原有分析结果和详细信息
        return mergeAnalysisResults(originalResult, detailedInfo, sourceSetName)
    }
    
    /**
     * 查找类文件
     */
    private fun findClassFile(className: String, classDirectory: File): File? {
        val classPath = className.replace('.', '/') + ".class"
        val classFile = File(classDirectory, classPath)
        return if (classFile.exists()) classFile else null
    }
    
    /**
     * 分析类的结构信息
     */
    private fun analyzeClassStructure(
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
            println("分析类结构失败: ${classFile.absolutePath}, 错误: ${e.message}")
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
            return classInfo.copy(sourceSetName = sourceSetName)
        }
        
        val sourceDoc = documentationExtractor.extractDocumentation(sourceFile)
        if (sourceDoc == null) {
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
     * 合并原有分析结果和详细信息
     */
    private fun mergeAnalysisResults(
        originalResult: ClassAnalysisResult,
        detailedInfo: DetailedClassInfo?,
        sourceSetName: String
    ): DetailedClassInfo {
        // 转换原有的PropertyAccessInfo到新的模型
        val convertedPropertyAccess: List<org.morecup.pragmaddd.analyzer.model.PropertyAccessInfo> = convertPropertyAccessInfo(originalResult.methods)

        if (detailedInfo != null) {
            // 如果有详细信息，将原有的属性访问分析添加到详细信息中
            return detailedInfo.copy(
                propertyAccessAnalysis = convertedPropertyAccess,
                sourceSetName = sourceSetName
            )
        } else {
            // 如果没有详细信息，创建基本的详细信息结构
            val className = originalResult.className
            val packageName = className.substringBeforeLast('.', "")
            val simpleName = className.substringAfterLast('.')

            // 转换领域对象类型
            val convertedDomainObjectType: org.morecup.pragmaddd.analyzer.model.DomainObjectType = convertDomainObjectType(originalResult.domainObjectType)

            return DetailedClassInfo(
                className = className,
                simpleName = simpleName,
                packageName = packageName,
                modifiers = ModifierInfo(0), // 默认修饰符
                domainObjectType = convertedDomainObjectType,
                sourceSetName = sourceSetName,
                propertyAccessAnalysis = convertedPropertyAccess
            )
        }
    }
    
    /**
     * 构建方法签名用于匹配
     */
    private fun buildMethodSignature(method: DetailedMethodInfo): String {
        // 简化的方法签名构建
        val paramTypes = extractParameterTypes(method.descriptor)
        return "${method.name}(${paramTypes.joinToString(",")})"
    }
    
    /**
     * 从方法描述符中提取参数类型
     */
    private fun extractParameterTypes(descriptor: String): List<String> {
        // 复用DetailedDomainObjectAnalyzer中的实现
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
     * 转换领域对象类型
     */
    private fun convertDomainObjectType(originalType: org.morecup.pragmaddd.analyzer.DomainObjectType): org.morecup.pragmaddd.analyzer.model.DomainObjectType {
        return when (originalType) {
            org.morecup.pragmaddd.analyzer.DomainObjectType.AGGREGATE_ROOT -> org.morecup.pragmaddd.analyzer.model.DomainObjectType.AGGREGATE_ROOT
            org.morecup.pragmaddd.analyzer.DomainObjectType.DOMAIN_ENTITY -> org.morecup.pragmaddd.analyzer.model.DomainObjectType.DOMAIN_ENTITY
            org.morecup.pragmaddd.analyzer.DomainObjectType.VALUE_OBJECT -> org.morecup.pragmaddd.analyzer.model.DomainObjectType.VALUE_OBJECT
        }
    }

    /**
     * 转换属性访问信息
     */
    private fun convertPropertyAccessInfo(originalList: List<org.morecup.pragmaddd.analyzer.PropertyAccessInfo>): List<org.morecup.pragmaddd.analyzer.model.PropertyAccessInfo> {
        return originalList.map { original ->
            org.morecup.pragmaddd.analyzer.model.PropertyAccessInfo(
                className = original.className,
                methodName = original.methodName,
                methodDescriptor = original.methodDescriptor,
                accessedProperties = original.accessedProperties,
                modifiedProperties = original.modifiedProperties,
                calledMethods = convertMethodCallInfo(original.calledMethods),
                lambdaExpressions = convertLambdaInfo(original.lambdaExpressions),
                externalPropertyAccesses = convertExternalPropertyAccessInfo(original.externalPropertyAccesses)
            )
        }
    }

    /**
     * 转换方法调用信息
     */
    private fun convertMethodCallInfo(originalSet: Set<org.morecup.pragmaddd.analyzer.MethodCallInfo>): Set<org.morecup.pragmaddd.analyzer.model.MethodCallInfo> {
        return originalSet.map { original ->
            org.morecup.pragmaddd.analyzer.model.MethodCallInfo(
                className = original.className,
                methodName = original.methodName,
                methodDescriptor = original.methodDescriptor,
                callCount = original.callCount,
                associatedLambdas = convertLambdaInfo(original.associatedLambdas)
            )
        }.toSet()
    }

    /**
     * 转换Lambda信息
     */
    private fun convertLambdaInfo(originalSet: Set<org.morecup.pragmaddd.analyzer.LambdaInfo>): Set<org.morecup.pragmaddd.analyzer.model.LambdaInfo> {
        return originalSet.map { original ->
            org.morecup.pragmaddd.analyzer.model.LambdaInfo(
                className = original.className,
                methodName = original.methodName,
                methodDescriptor = original.methodDescriptor,
                lambdaType = original.lambdaType,
                capturedVariables = original.capturedVariables
            )
        }.toSet()
    }

    /**
     * 转换外部属性访问信息
     */
    private fun convertExternalPropertyAccessInfo(originalSet: Set<org.morecup.pragmaddd.analyzer.ExternalPropertyAccessInfo>): Set<org.morecup.pragmaddd.analyzer.model.ExternalPropertyAccessInfo> {
        return originalSet.map { original ->
            // 将三个布尔字段转换为单个枚举字段
            val targetDomainObjectType = when {
                original.hasAggregateRootAnnotation -> org.morecup.pragmaddd.analyzer.model.DomainObjectType.AGGREGATE_ROOT
                original.hasDomainEntityAnnotation -> org.morecup.pragmaddd.analyzer.model.DomainObjectType.DOMAIN_ENTITY
                original.hasValueObjectAnnotation -> org.morecup.pragmaddd.analyzer.model.DomainObjectType.VALUE_OBJECT
                else -> null
            }

            org.morecup.pragmaddd.analyzer.model.ExternalPropertyAccessInfo(
                targetClassName = original.targetClassName,
                propertyName = original.propertyName,
                accessType = convertPropertyAccessType(original.accessType),
                targetDomainObjectType = targetDomainObjectType
            )
        }.toSet()
    }

    /**
     * 转换属性访问类型
     */
    private fun convertPropertyAccessType(originalType: org.morecup.pragmaddd.analyzer.PropertyAccessType): org.morecup.pragmaddd.analyzer.model.PropertyAccessType {
        return when (originalType) {
            org.morecup.pragmaddd.analyzer.PropertyAccessType.READ -> org.morecup.pragmaddd.analyzer.model.PropertyAccessType.READ
            org.morecup.pragmaddd.analyzer.PropertyAccessType.WRITE -> org.morecup.pragmaddd.analyzer.model.PropertyAccessType.WRITE
        }
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
