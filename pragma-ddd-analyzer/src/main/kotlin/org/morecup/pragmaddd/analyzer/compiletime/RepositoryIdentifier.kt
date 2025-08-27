package org.morecup.pragmaddd.analyzer.compiletime

import org.morecup.pragmaddd.analyzer.compiletime.model.*
import org.morecup.pragmaddd.analyzer.model.DetailedClassInfo
import org.morecup.pragmaddd.analyzer.model.DomainObjectType
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import java.io.File

/**
 * Repository识别器
 * 负责识别聚合根对应的Repository接口
 */
class RepositoryIdentifier(
    private val config: RepositoryIdentificationConfig = RepositoryIdentificationConfig()
) {
    
    /**
     * 识别所有聚合根对应的Repository
     * @param aggregateRoots 聚合根类名集合
     * @param classFiles 编译输出的class文件
     * @return 聚合根-Repository映射关系
     */
    fun identifyRepositories(
        aggregateRoots: Set<String>,
        classFiles: List<File>
    ): List<AggregateRootRepositoryMapping> {
        val mappings = mutableListOf<AggregateRootRepositoryMapping>()
        val interfaceClasses = findInterfaceClasses(classFiles)
        
        for (aggregateRoot in aggregateRoots) {
            val repositoryClass = findRepositoryForAggregateRoot(aggregateRoot, interfaceClasses)
            if (repositoryClass != null) {
                mappings.add(repositoryClass)
            }
        }
        
        return mappings
    }
    
    /**
     * 从domain-analyzer.json结果中提取聚合根类名
     */
    fun extractAggregateRoots(domainAnalysisResult: Map<String, DetailedClassInfo>): Set<String> {
        return domainAnalysisResult.values
            .filter { it.domainObjectType == DomainObjectType.AGGREGATE_ROOT }
            .map { it.className }
            .toSet()
    }
    
    private fun findRepositoryForAggregateRoot(
        aggregateRoot: String,
        interfaceClasses: List<ClassInfo>
    ): AggregateRootRepositoryMapping? {
        
        // 优先级1: DomainRepository<T>泛型接口
        val genericRepo = findGenericRepository(aggregateRoot, interfaceClasses)
        if (genericRepo != null) {
            return AggregateRootRepositoryMapping(
                aggregateRoot, 
                genericRepo, 
                RepositoryMatchType.GENERIC_INTERFACE
            )
        }
        
        // 优先级2: @DomainRepository注解
        val annotatedRepo = findAnnotatedRepository(aggregateRoot, interfaceClasses)
        if (annotatedRepo != null) {
            return AggregateRootRepositoryMapping(
                aggregateRoot, 
                annotatedRepo, 
                RepositoryMatchType.ANNOTATION
            )
        }
        
        // 优先级3: 命名约定
        val conventionRepo = findByNamingConvention(aggregateRoot, interfaceClasses)
        if (conventionRepo != null) {
            return AggregateRootRepositoryMapping(
                aggregateRoot, 
                conventionRepo, 
                RepositoryMatchType.NAMING_CONVENTION
            )
        }
        
        return null
    }
    
    private fun findGenericRepository(
        aggregateRoot: String,
        interfaceClasses: List<ClassInfo>
    ): String? {
        return interfaceClasses.find { classInfo ->
            classInfo.interfaces.any { interfaceName ->
                // 检查是否继承DomainRepository<AggregateRoot>
                when {
                    interfaceName == "org/morecup/pragmaddd/core/DomainRepository" -> {
                        // 检查泛型参数是否匹配聚合根
                        classInfo.signature?.contains(aggregateRoot.replace('.', '/')) == true
                    }
                    interfaceName.contains("DomainRepository") -> {
                        // 检查是否有泛型参数匹配
                        classInfo.signature?.contains(aggregateRoot.replace('.', '/')) == true
                    }
                    else -> false
                }
            }
        }?.className
    }
    
    private fun findAnnotatedRepository(
        aggregateRoot: String,
        interfaceClasses: List<ClassInfo>
    ): String? {
        return interfaceClasses.find { classInfo ->
            classInfo.annotations.any { annotation ->
                annotation.contains("DomainRepository") &&
                // 检查注解参数是否指向该聚合根
                annotation.contains(aggregateRoot.substringAfterLast('.'))
            }
        }?.className
    }
    
    private fun findByNamingConvention(
        aggregateRoot: String,
        interfaceClasses: List<ClassInfo>
    ): String? {
        val aggregateRootSimpleName = aggregateRoot.substringAfterLast('.')
        
        for (rule in config.namingRules) {
            val expectedRepositoryName = rule.replace("{AggregateRoot}", aggregateRootSimpleName)
            
            val found = interfaceClasses.find { classInfo ->
                val simpleName = classInfo.className.substringAfterLast('.')
                simpleName == expectedRepositoryName ||
                classInfo.className.endsWith(".$expectedRepositoryName")
            }
            
            if (found != null) {
                return found.className
            }
        }
        
        return null
    }
    
    private fun findInterfaceClasses(classFiles: List<File>): List<ClassInfo> {
        val interfaces = mutableListOf<ClassInfo>()
        
        for (classFile in classFiles) {
            if (isBusinessClass(classFile)) {
                try {
                    val classInfo = extractClassInfo(classFile)
                    if (classInfo.isInterface) {
                        interfaces.add(classInfo)
                    }
                } catch (e: Exception) {
                    // 忽略无法解析的类文件
                }
            }
        }
        
        return interfaces
    }
    
    private fun extractClassInfo(classFile: File): ClassInfo {
        val classInfoExtractor = ClassInfoExtractor()
        ClassReader(classFile.inputStream()).accept(classInfoExtractor, 0)
        return classInfoExtractor.getClassInfo()
    }
    
    private fun isBusinessClass(classFile: File): Boolean {
        val fileName = classFile.name
        if (!fileName.endsWith(".class")) return false
        
        val className = extractClassNameFromFile(classFile)
        
        return !className.contains("$$") && // 排除CGLIB代理
               !className.startsWith("java.") && // 排除JDK类
               !className.startsWith("kotlin.") && // 排除Kotlin标准库
               !className.startsWith("org.springframework.") && // 排除Spring框架类
               !className.contains("$") && // 排除内部类（简化处理）
               isIncludedPackage(className) && 
               !isExcludedPackage(className)
    }
    
    private fun extractClassNameFromFile(classFile: File): String {
        val relativePath = classFile.relativeTo(classFile.parentFile.let { parent ->
            var current = parent
            while (current != null && !current.name.equals("classes")) {
                current = current.parentFile
            }
            current?.parentFile ?: parent
        })
        
        return relativePath.path.replace(File.separator, ".")
            .removeSuffix(".class")
    }
    
    private fun isIncludedPackage(className: String): Boolean {
        if (config.includePackages.isEmpty() || config.includePackages.contains("**")) {
            return true
        }
        
        return config.includePackages.any { pattern ->
            when {
                pattern == "**" -> true
                pattern.endsWith("**") -> className.startsWith(pattern.removeSuffix("**"))
                pattern.endsWith("*") -> className.startsWith(pattern.removeSuffix("*"))
                else -> className.startsWith(pattern)
            }
        }
    }
    
    private fun isExcludedPackage(className: String): Boolean {
        return config.excludePackages.any { pattern ->
            when {
                pattern == "**" -> true
                pattern.endsWith("**") -> className.startsWith(pattern.removeSuffix("**"))
                pattern.endsWith("*") -> className.startsWith(pattern.removeSuffix("*"))
                else -> className.startsWith(pattern)
            }
        }
    }
}

/**
 * 简化的类信息（用于Repository识别）
 */
data class ClassInfo(
    val className: String,
    val isInterface: Boolean,
    val interfaces: List<String>,
    val signature: String?,
    val annotations: List<String>
)

/**
 * 类信息提取器
 */
private class ClassInfoExtractor : ClassVisitor(Opcodes.ASM9) {
    private lateinit var className: String
    private var isInterface = false
    private val interfaces = mutableListOf<String>()
    private var signature: String? = null
    private val annotations = mutableListOf<String>()
    
    override fun visit(
        version: Int,
        access: Int,
        name: String,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        this.className = name.replace('/', '.')
        this.isInterface = (access and Opcodes.ACC_INTERFACE) != 0
        this.signature = signature
        interfaces?.forEach { this.interfaces.add(it.replace('/', '.')) }
    }
    
    override fun visitAnnotation(descriptor: String, visible: Boolean): org.objectweb.asm.AnnotationVisitor? {
        annotations.add(Type.getType(descriptor).className)
        return null
    }
    
    fun getClassInfo(): ClassInfo {
        return ClassInfo(className, isInterface, interfaces, signature, annotations)
    }
}