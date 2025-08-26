package org.morecup.pragmaddd.analyzer.callgraph

import org.objectweb.asm.*
import java.io.File
import java.io.FileInputStream

/**
 * Repository识别器
 * 
 * 负责识别项目中的Repository接口，支持三种识别模式：
 * 1. DomainRepository泛型接口继承
 * 2. @DomainRepository注解标注
 * 3. 命名约定推导
 */
class RepositoryIdentifier(
    private val namingRules: List<String> = listOf(
        "{AggregateRoot}Repository",
        "I{AggregateRoot}Repository", 
        "{AggregateRoot}Repo"
    )
) {
    
    companion object {
        private const val DOMAIN_REPOSITORY_ANNOTATION = "Lorg/morecup/pragmaddd/core/annotation/DomainRepository;"
        private const val AGGREGATE_ROOT_ANNOTATION = "Lorg/morecup/pragmaddd/core/annotation/AggregateRoot;"
    }
    
    /**
     * Repository信息
     */
    data class RepositoryInfo(
        val className: String,
        val aggregateRootClass: String,
        val identificationMethod: IdentificationMethod,
        val methods: List<RepositoryMethodInfo> = emptyList()
    )
    
    /**
     * Repository方法信息
     */
    data class RepositoryMethodInfo(
        val methodName: String,
        val methodDescriptor: String,
        val returnType: String,
        val parameterTypes: List<String>
    )
    
    /**
     * 识别方法枚举
     */
    enum class IdentificationMethod {
        GENERIC_INTERFACE,  // 泛型接口继承
        ANNOTATION,         // 注解标注
        NAMING_CONVENTION   // 命名约定
    }
    
    /**
     * 分析目录中的所有Repository
     */
    fun identifyRepositories(classDirectory: File): List<RepositoryInfo> {
        val repositories = mutableListOf<RepositoryInfo>()
        val aggregateRoots = findAggregateRoots(classDirectory)
        
        classDirectory.walkTopDown()
            .filter { it.isFile && it.extension == "class" }
            .forEach { classFile ->
                try {
                    val repositoryInfo = analyzeClassFile(classFile, aggregateRoots)
                    if (repositoryInfo != null) {
                        repositories.add(repositoryInfo)
                    }
                } catch (e: Exception) {
                    // 忽略分析失败的类文件
                }
            }
        
        return repositories
    }
    
    /**
     * 查找所有聚合根类
     */
    private fun findAggregateRoots(classDirectory: File): Set<String> {
        val aggregateRoots = mutableSetOf<String>()
        
        classDirectory.walkTopDown()
            .filter { it.isFile && it.extension == "class" }
            .forEach { classFile ->
                try {
                    if (isAggregateRoot(classFile)) {
                        val className = getClassNameFromFile(classFile, classDirectory)
                        aggregateRoots.add(className)
                    }
                } catch (e: Exception) {
                    // 忽略分析失败的类文件
                }
            }
        
        return aggregateRoots
    }
    
    /**
     * 检查类文件是否为聚合根
     */
    private fun isAggregateRoot(classFile: File): Boolean {
        var isAggregateRoot = false
        
        FileInputStream(classFile).use { input ->
            val classReader = ClassReader(input)
            classReader.accept(object : ClassVisitor(Opcodes.ASM9) {
                override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
                    if (descriptor == AGGREGATE_ROOT_ANNOTATION) {
                        isAggregateRoot = true
                    }
                    return null
                }
            }, ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)
        }
        
        return isAggregateRoot
    }
    
    /**
     * 分析单个类文件
     */
    private fun analyzeClassFile(classFile: File, aggregateRoots: Set<String>): RepositoryInfo? {
        var repositoryInfo: RepositoryInfo? = null
        
        FileInputStream(classFile).use { input ->
            val classReader = ClassReader(input)
            val visitor = RepositoryClassVisitor(aggregateRoots)
            classReader.accept(visitor, ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)
            repositoryInfo = visitor.getRepositoryInfo()
        }
        
        return repositoryInfo
    }
    
    /**
     * 从文件路径获取类名
     */
    private fun getClassNameFromFile(classFile: File, baseDirectory: File): String {
        val relativePath = baseDirectory.toURI().relativize(classFile.toURI()).path
        return relativePath.removeSuffix(".class").replace('/', '.')
    }
    
    /**
     * Repository类访问器
     */
    private inner class RepositoryClassVisitor(
        private val aggregateRoots: Set<String>
    ) : ClassVisitor(Opcodes.ASM9) {
        
        private var className: String = ""
        private var isInterface: Boolean = false
        private var superInterfaces: Array<String> = emptyArray()
        private var hasRepositoryAnnotation: Boolean = false
        private var aggregateRootFromAnnotation: String? = null
        private val methods = mutableListOf<RepositoryMethodInfo>()
        
        override fun visit(
            version: Int,
            access: Int,
            name: String,
            signature: String?,
            superName: String?,
            interfaces: Array<String>?
        ) {
            className = name.replace('/', '.')
            isInterface = (access and Opcodes.ACC_INTERFACE) != 0
            superInterfaces = interfaces ?: emptyArray()
        }
        
        override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
            if (descriptor == DOMAIN_REPOSITORY_ANNOTATION) {
                hasRepositoryAnnotation = true
                return object : AnnotationVisitor(Opcodes.ASM9) {
                    override fun visit(name: String?, value: Any?) {
                        if (name == "aggregateRoot" && value is Type) {
                            aggregateRootFromAnnotation = value.className
                        }
                    }
                }
            }
            return null
        }
        
        override fun visitMethod(
            access: Int,
            name: String,
            descriptor: String,
            signature: String?,
            exceptions: Array<String>?
        ): MethodVisitor? {
            // 只记录公共方法，排除构造函数和静态方法
            if ((access and Opcodes.ACC_PUBLIC) != 0 && 
                name != "<init>" && name != "<clinit>" &&
                (access and Opcodes.ACC_STATIC) == 0) {
                
                val returnType = Type.getReturnType(descriptor).className
                val parameterTypes = Type.getArgumentTypes(descriptor).map { it.className }
                
                methods.add(RepositoryMethodInfo(
                    methodName = name,
                    methodDescriptor = descriptor,
                    returnType = returnType,
                    parameterTypes = parameterTypes
                ))
            }
            return null
        }
        
        fun getRepositoryInfo(): RepositoryInfo? {
            if (!isInterface) return null
            
            // 优先级1：检查泛型接口继承
            val genericAggregateRoot = checkGenericInterface()
            if (genericAggregateRoot != null) {
                return RepositoryInfo(
                    className = className,
                    aggregateRootClass = genericAggregateRoot,
                    identificationMethod = IdentificationMethod.GENERIC_INTERFACE,
                    methods = methods
                )
            }
            
            // 优先级2：检查注解标注
            if (hasRepositoryAnnotation && aggregateRootFromAnnotation != null) {
                return RepositoryInfo(
                    className = className,
                    aggregateRootClass = aggregateRootFromAnnotation!!,
                    identificationMethod = IdentificationMethod.ANNOTATION,
                    methods = methods
                )
            }
            
            // 优先级3：检查命名约定
            val conventionAggregateRoot = checkNamingConvention()
            if (conventionAggregateRoot != null) {
                return RepositoryInfo(
                    className = className,
                    aggregateRootClass = conventionAggregateRoot,
                    identificationMethod = IdentificationMethod.NAMING_CONVENTION,
                    methods = methods
                )
            }
            
            return null
        }
        
        /**
         * 检查泛型接口继承
         */
        private fun checkGenericInterface(): String? {
            // 检查是否继承了DomainRepository接口
            for (interfaceName in superInterfaces) {
                if (interfaceName == "org/morecup/pragmaddd/core/repository/DomainRepository") {
                    // 需要从泛型签名中解析T的类型
                    // 这里简化处理，通过命名约定推导
                    val simpleName = className.substringAfterLast('.')
                    if (simpleName.endsWith("Repository")) {
                        val aggregateRootName = simpleName.removeSuffix("Repository")
                        val packageName = className.substringBeforeLast('.', "")
                        val fullAggregateRootName = if (packageName.isNotEmpty()) {
                            "$packageName.$aggregateRootName"
                        } else {
                            aggregateRootName
                        }

                        // 检查对应的聚合根是否存在
                        if (aggregateRoots.contains(fullAggregateRootName)) {
                            return fullAggregateRootName
                        }
                    }
                }
            }
            return null
        }
        
        /**
         * 检查命名约定
         */
        private fun checkNamingConvention(): String? {
            val simpleName = className.substringAfterLast('.')
            
            for (rule in namingRules) {
                val pattern = rule.replace("{AggregateRoot}", "(.+)")
                val regex = Regex(pattern)
                val matchResult = regex.matchEntire(simpleName)
                
                if (matchResult != null) {
                    val aggregateRootName = matchResult.groupValues[1]
                    val packageName = className.substringBeforeLast('.', "")
                    val fullAggregateRootName = if (packageName.isNotEmpty()) {
                        "$packageName.$aggregateRootName"
                    } else {
                        aggregateRootName
                    }
                    
                    // 检查对应的聚合根是否存在
                    if (aggregateRoots.contains(fullAggregateRootName)) {
                        return fullAggregateRootName
                    }
                }
            }
            
            return null
        }
    }
}
