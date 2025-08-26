package org.morecup.pragmaddd.analyzer.callanalysis

import org.morecup.pragmaddd.analyzer.callanalysis.model.RepositoryIdentificationConfig
import org.morecup.pragmaddd.analyzer.callanalysis.model.RepositoryIdentificationMethod
import org.morecup.pragmaddd.analyzer.callanalysis.model.RepositoryInfo
import org.objectweb.asm.*
import java.io.File
import java.io.FileInputStream

/**
 * Repository识别器
 * 
 * 按优先级顺序识别Repository：
 * 1. 继承 DomainRepository<T> 泛型接口
 * 2. 使用 @DomainRepository 注解标注的接口
 * 3. 通过命名约定推导
 */
class RepositoryIdentifier(
    private val config: RepositoryIdentificationConfig = RepositoryIdentificationConfig(),
    private val aggregateRootClasses: Set<String> = emptySet()
) {
    
    companion object {
        private const val DOMAIN_REPOSITORY_INTERFACE = "org/morecup/pragmaddd/core/repository/DomainRepository"
        private const val DOMAIN_REPOSITORY_ANNOTATION = "Lorg/morecup/pragmaddd/core/annotation/DomainRepository;"
    }
    
    /**
     * 识别目录中的所有Repository
     */
    fun identifyRepositories(directory: File): List<RepositoryInfo> {
        val repositories = mutableListOf<RepositoryInfo>()
        
        directory.walkTopDown()
            .filter { it.isFile && it.extension == "class" }
            .forEach { classFile ->
                identifyRepository(classFile)?.let { repositories.add(it) }
            }
        
        return repositories
    }
    
    /**
     * 识别单个类文件是否为Repository
     */
    fun identifyRepository(classFile: File): RepositoryInfo? {
        return try {
            FileInputStream(classFile).use { input ->
                val classReader = ClassReader(input)
                val visitor = RepositoryClassVisitor()
                classReader.accept(visitor, ClassReader.SKIP_CODE)
                visitor.getRepositoryInfo()
            }
        } catch (e: Exception) {
            println("识别Repository失败: ${classFile.absolutePath}, 错误: ${e.message}")
            null
        }
    }
    
    /**
     * Repository类访问器
     */
    private inner class RepositoryClassVisitor : ClassVisitor(Opcodes.ASM9) {
        private var className: String = ""
        private var isInterface: Boolean = false
        private var hasDomainRepositoryAnnotation: Boolean = false
        private var domainRepositoryGenericType: String? = null
        private var interfaces: Array<String> = emptyArray()
        
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
            this.interfaces = interfaces?.map { it.replace('/', '.') }?.toTypedArray() ?: emptyArray()
            
            // 解析泛型签名以获取DomainRepository的类型参数
            signature?.let { sig ->
                parseDomainRepositoryGeneric(sig)
            }
            
            super.visit(version, access, name, signature, superName, interfaces)
        }
        
        override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
            if (descriptor == DOMAIN_REPOSITORY_ANNOTATION) {
                hasDomainRepositoryAnnotation = true
            }
            return super.visitAnnotation(descriptor, visible)
        }
        
        /**
         * 解析DomainRepository泛型参数
         */
        private fun parseDomainRepositoryGeneric(signature: String) {
            // 匹配类似 Ljava/lang/Object;Lorg/morecup/pragmaddd/core/repository/DomainRepository<Lcom/example/domain/Order;>;
            val escapedInterface = DOMAIN_REPOSITORY_INTERFACE.replace("/", "\\/")
            val domainRepoPattern = Regex("L$escapedInterface<L([^;]+);>")
            domainRepoPattern.find(signature)?.let { match ->
                domainRepositoryGenericType = match.groupValues[1].replace('/', '.')
            }
        }
        
        fun getRepositoryInfo(): RepositoryInfo? {
            if (!isInterface) return null
            
            // 优先级1: 继承 DomainRepository<T>
            domainRepositoryGenericType?.let { aggregateRootType ->
                if (aggregateRootClasses.isEmpty() || aggregateRootClasses.contains(aggregateRootType)) {
                    return RepositoryInfo(
                        className = className,
                        aggregateRootClass = aggregateRootType,
                        identificationMethod = RepositoryIdentificationMethod.DOMAIN_REPOSITORY_INTERFACE
                    )
                }
            }
            
            // 优先级2: @DomainRepository 注解
            if (hasDomainRepositoryAnnotation) {
                // 尝试从命名约定推导聚合根类型
                val aggregateRootType = inferAggregateRootFromNaming(className)
                if (aggregateRootType != null && (aggregateRootClasses.isEmpty() || aggregateRootClasses.contains(aggregateRootType))) {
                    return RepositoryInfo(
                        className = className,
                        aggregateRootClass = aggregateRootType,
                        identificationMethod = RepositoryIdentificationMethod.DOMAIN_REPOSITORY_ANNOTATION
                    )
                }
            }
            
            // 优先级3: 命名约定推导
            val aggregateRootType = inferAggregateRootFromNaming(className)
            if (aggregateRootType != null && (aggregateRootClasses.isEmpty() || aggregateRootClasses.contains(aggregateRootType))) {
                return RepositoryInfo(
                    className = className,
                    aggregateRootClass = aggregateRootType,
                    identificationMethod = RepositoryIdentificationMethod.NAMING_CONVENTION
                )
            }
            
            return null
        }
        
        /**
         * 从命名约定推导聚合根类型
         */
        private fun inferAggregateRootFromNaming(repositoryClassName: String): String? {
            val simpleClassName = repositoryClassName.substringAfterLast('.')
            val packageName = repositoryClassName.substringBeforeLast('.', "")
            
            for (rule in config.repositoryNamingRules) {
                val pattern = rule.replace("{AggregateRoot}", "(.+)")
                val regex = Regex("^$pattern$")
                
                regex.find(simpleClassName)?.let { match ->
                    val aggregateRootName = match.groupValues[1]
                    
                    // 尝试不同的包名组合
                    val possiblePackages = listOf(
                        packageName.replace(".repository", ".domain"),
                        packageName.replace(".repo", ".domain"),
                        packageName,
                        "$packageName.domain"
                    ).distinct()
                    
                    for (pkg in possiblePackages) {
                        val fullClassName = if (pkg.isNotEmpty()) "$pkg.$aggregateRootName" else aggregateRootName
                        if (aggregateRootClasses.isEmpty() || aggregateRootClasses.contains(fullClassName)) {
                            return fullClassName
                        }
                    }
                }
            }
            
            return null
        }
    }
}