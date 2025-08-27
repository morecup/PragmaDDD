package org.morecup.pragmaddd.analyzer.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.objectweb.asm.*
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import java.io.File
import java.io.FileInputStream

/**
 * Repository调用链分析器 - 编译期静态分析系统
 * 用于替代运行时堆栈跟踪分析，提供更准确的Repository调用和聚合根字段访问分析
 */
class RepositoryCallAnalyzer {
    
    private val objectMapper: ObjectMapper = jacksonObjectMapper()
    
    // 聚合根类缓存 (className -> AggregateRootInfo)
    private val aggregateRootCache = mutableMapOf<String, AggregateRootInfo>()
    
    // Repository类缓存 (className -> RepositoryInfo) 
    private val repositoryCache = mutableMapOf<String, RepositoryInfo>()
    
    // 方法调用链缓存 (methodKey -> List<MethodCallInfo>)
    private val callChainCache = mutableMapOf<String, List<MethodCallInfo>>()
    
    companion object {
        private const val AGGREGATE_ROOT_ANNOTATION = "Lorg/morecup/pragmaddd/core/annotation/AggregateRoot;"
        private const val DOMAIN_REPOSITORY_ANNOTATION = "Lorg/morecup/pragmaddd/core/annotation/DomainRepository;"
        private const val DOMAIN_REPOSITORY_INTERFACE = "org/morecup/pragmaddd/core/repository/DomainRepository"
    }
    
    /**
     * 分析目录中的所有class文件，生成Repository调用分析结果
     */
    fun analyzeDirectory(directory: File): RepositoryAnalysisResult {
        // 第一阶段：扫描识别聚合根和Repository
        scanAggregateRoots(directory)
        scanRepositories(directory)
        matchRepositoriesWithAggregateRoots()
        
        // 第二阶段：分析方法调用链和字段访问
        val callAnalysisResults = mutableListOf<CallAnalysisInfo>()
        
        directory.walkTopDown()
            .filter { it.isFile && it.extension == "class" }
            .forEach { classFile ->
                analyzeClassForRepositoryCalls(classFile)?.let { results ->
                    callAnalysisResults.addAll(results)
                }
            }
        
        return RepositoryAnalysisResult(
            aggregateRoots = aggregateRootCache.values.toList(),
            repositories = repositoryCache.values.toList(),
            callAnalysis = callAnalysisResults
        )
    }
    
    /**
     * 第一阶段：扫描识别所有聚合根
     */
    private fun scanAggregateRoots(directory: File) {
        directory.walkTopDown()
            .filter { it.isFile && it.extension == "class" }
            .forEach { classFile ->
                try {
                    FileInputStream(classFile).use { input ->
                        val classReader = ClassReader(input)
                        val scanner = AggregateRootScanner()
                        classReader.accept(scanner, ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG)
                        scanner.getResult()?.let { info ->
                            aggregateRootCache[info.className] = info
                        }
                    }
                } catch (e: Exception) {
                    println("扫描聚合根失败: ${classFile.absolutePath}, 错误: ${e.message}")
                }
            }
    }
    
    /**
     * 第一阶段：扫描识别所有Repository
     */
    private fun scanRepositories(directory: File) {
        directory.walkTopDown()
            .filter { it.isFile && it.extension == "class" }
            .forEach { classFile ->
                try {
                    FileInputStream(classFile).use { input ->
                        val classReader = ClassReader(input)
                        val scanner = RepositoryScanner()
                        classReader.accept(scanner, ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG)
                        scanner.getResult()?.let { info ->
                            repositoryCache[info.className] = info
                        }
                    }
                } catch (e: Exception) {
                    println("扫描Repository失败: ${classFile.absolutePath}, 错误: ${e.message}")
                }
            }
    }
    
    /**
     * 第一阶段：匹配Repository与聚合根的关系
     */
    private fun matchRepositoriesWithAggregateRoots() {
        repositoryCache.values.forEach { repo ->
            // 优先级1：检查DomainRepository泛型参数
            repo.genericAggregateRootType?.let { aggregateRootType ->
                if (aggregateRootCache.containsKey(aggregateRootType)) {
                    repo.targetAggregateRoot = aggregateRootType
                    return@forEach
                }
            }
            
            // 优先级2：检查@DomainRepository注解参数
            repo.annotationTargetType?.let { targetType ->
                if (aggregateRootCache.containsKey(targetType)) {
                    repo.targetAggregateRoot = targetType
                    return@forEach
                }
            }
            
            // 优先级3：通过命名约定推导
            val possibleAggregateRoots = generateNamingConventionCandidates(repo.className)
            for (candidate in possibleAggregateRoots) {
                if (aggregateRootCache.containsKey(candidate)) {
                    repo.targetAggregateRoot = candidate
                    break
                }
            }
        }
    }
    
    /**
     * 生成命名约定候选名称
     */
    private fun generateNamingConventionCandidates(repositoryClassName: String): List<String> {
        val candidates = mutableListOf<String>()
        val simpleName = repositoryClassName.substringAfterLast('.')
        
        // 移除Repository后缀的各种变体
        listOf("Repository", "RepositoryImpl", "Repo").forEach { suffix ->
            if (simpleName.endsWith(suffix)) {
                val baseName = simpleName.removeSuffix(suffix)
                
                // 同包名下的聚合根
                val packageName = repositoryClassName.substringBeforeLast('.')
                candidates.add("$packageName.$baseName")
                
                // 常见的domain包路径
                if (packageName.contains("repository") || packageName.contains("admin")) {
                    val domainPackage = packageName.replace("repository", "domain")
                        .replace("admin", "domain")
                    candidates.add("$domainPackage.$baseName")
                }
            }
        }
        
        // 移除I前缀（接口命名约定）
        if (simpleName.startsWith("I") && simpleName.length > 1) {
            val nameWithoutI = simpleName.substring(1)
            listOf("Repository", "RepositoryImpl", "Repo").forEach { suffix ->
                if (nameWithoutI.endsWith(suffix)) {
                    val baseName = nameWithoutI.removeSuffix(suffix)
                    val packageName = repositoryClassName.substringBeforeLast('.')
                    candidates.add("$packageName.$baseName")
                }
            }
        }
        
        return candidates.distinct()
    }
    
    /**
     * 第二阶段：分析特定类的Repository调用
     */
    private fun analyzeClassForRepositoryCalls(classFile: File): List<CallAnalysisInfo>? {
        return try {
            FileInputStream(classFile).use { input ->
                val classReader = ClassReader(input)
                val classNode = ClassNode()
                classReader.accept(classNode, ClassReader.EXPAND_FRAMES)
                
                val results = mutableListOf<CallAnalysisInfo>()
                
                classNode.methods?.forEach { methodNode ->
                    analyzeMethodForRepositoryCalls(classNode, methodNode)?.let { callInfo ->
                        results.add(callInfo)
                    }
                }
                
                results.takeIf { it.isNotEmpty() }
            }
        } catch (e: Exception) {
            println("分析Repository调用失败: ${classFile.absolutePath}, 错误: ${e.message}")
            null
        }
    }
    
    /**
     * 分析方法中的Repository调用
     */
    private fun analyzeMethodForRepositoryCalls(classNode: ClassNode, methodNode: MethodNode): CallAnalysisInfo? {
        val methodCallVisitor = MethodCallVisitor(classNode.name, methodNode.name, methodNode.desc)
        methodNode.accept(methodCallVisitor)
        
        val repositoryCalls = methodCallVisitor.repositoryCalls
        if (repositoryCalls.isEmpty()) {
            return null
        }
        
        return CallAnalysisInfo(
            methodClass = classNode.name.replace('/', '.'),
            method = methodNode.name,
            methodDescriptor = methodNode.desc,
            repositoryCalls = repositoryCalls.map { repoCall ->
                val targetRepo = repositoryCache[repoCall.repositoryClass]
                val targetAggregateRoot = targetRepo?.targetAggregateRoot?.let { aggregateRootCache[it] }
                
                RepositoryCallInfo(
                    repository = repoCall.repositoryClass,
                    repositoryMethod = repoCall.methodName,
                    repositoryMethodDescriptor = repoCall.methodDescriptor,
                    aggregateRoot = targetAggregateRoot?.className,
                    calledAggregateRootMethod = if (targetAggregateRoot != null) {
                        analyzeAggregateRootMethodCalls(classNode, methodNode, targetAggregateRoot)
                    } else emptyList(),
                    requiredFields = if (targetAggregateRoot != null) {
                        calculateRequiredFields(classNode, methodNode, targetAggregateRoot)
                    } else emptyList()
                )
            }
        )
    }
    
    /**
     * 分析聚合根方法调用
     */
    private fun analyzeAggregateRootMethodCalls(
        classNode: ClassNode, 
        methodNode: MethodNode, 
        aggregateRoot: AggregateRootInfo
    ): List<AggregateRootMethodCall> {
        val visitor = AggregateRootMethodCallVisitor(aggregateRoot.className.replace('.', '/'))
        methodNode.accept(visitor)
        
        return visitor.aggregateRootCalls.map { call ->
            val requiredFields = analyzeMethodFieldAccess(aggregateRoot.className, call.methodName, call.methodDescriptor)
            AggregateRootMethodCall(
                aggregateRootMethod = call.methodName,
                aggregateRootMethodDescriptor = call.methodDescriptor,
                requiredFields = requiredFields
            )
        }
    }
    
    /**
     * 计算所需字段的并集
     */
    private fun calculateRequiredFields(
        classNode: ClassNode,
        methodNode: MethodNode, 
        aggregateRoot: AggregateRootInfo
    ): List<String> {
        val allRequiredFields = mutableSetOf<String>()
        
        // 直接字段访问
        val fieldAccessVisitor = FieldAccessVisitor(aggregateRoot.className.replace('.', '/'))
        methodNode.accept(fieldAccessVisitor)
        allRequiredFields.addAll(fieldAccessVisitor.accessedFields)
        
        // 通过聚合根方法访问的字段
        analyzeAggregateRootMethodCalls(classNode, methodNode, aggregateRoot).forEach { methodCall ->
            allRequiredFields.addAll(methodCall.requiredFields)
        }
        
        return allRequiredFields.toList().sorted()
    }
    
    /**
     * 分析聚合根方法内部的字段访问
     */
    private fun analyzeMethodFieldAccess(className: String, methodName: String, methodDescriptor: String): List<String> {
        try {
            val classReader = ClassReader(className)
            val fieldAccessAnalyzer = MethodFieldAccessAnalyzer(methodName, methodDescriptor)
            classReader.accept(fieldAccessAnalyzer, ClassReader.EXPAND_FRAMES)
            return fieldAccessAnalyzer.getAccessedFields()
        } catch (e: Exception) {
            println("分析方法字段访问失败: $className.$methodName, 错误: ${e.message}")
            return emptyList()
        }
    }
}