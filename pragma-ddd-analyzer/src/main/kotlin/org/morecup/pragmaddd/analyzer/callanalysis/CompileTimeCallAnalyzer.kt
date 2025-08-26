package org.morecup.pragmaddd.analyzer.callanalysis

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.morecup.pragmaddd.analyzer.callanalysis.model.*
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 编译期调用分析器主类
 * 
 * 整合Repository识别、方法调用图分析和字段访问分析，
 * 生成完整的编译期调用分析结果
 */
class CompileTimeCallAnalyzer(
    private val config: RepositoryIdentificationConfig = RepositoryIdentificationConfig()
) {
    
    private val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
    
    /**
     * 分析编译输出目录
     */
    fun analyzeDirectory(
        compiledClassesDir: File,
        domainAnalysisFile: File,
        outputFile: File
    ) {
        println("[编译期调用分析] 开始分析目录: ${compiledClassesDir.absolutePath}")
        
        // 1. 加载现有的domain-analyzer.json结果
        val domainAnalysisResult = loadDomainAnalysisResult(domainAnalysisFile)
        val aggregateRootClasses = extractAggregateRootClasses(domainAnalysisResult)
        
        println("[编译期调用分析] 发现 ${aggregateRootClasses.size} 个聚合根类: ${aggregateRootClasses.joinToString(", ")}")
        
        // 2. 识别Repository
        val repositoryIdentifier = RepositoryIdentifier(config, aggregateRootClasses)
        val repositories = repositoryIdentifier.identifyRepositories(compiledClassesDir)
        
        println("[编译期调用分析] 发现 ${repositories.size} 个Repository:")
        repositories.forEach { repo ->
            println("  - ${repo.className} -> ${repo.aggregateRootClass} (${repo.identificationMethod})")
        }
        
        // 3. 分析方法调用图
        val callGraphAnalyzer = MethodCallGraphAnalyzer(repositories, aggregateRootClasses)
        val methodCallContexts = callGraphAnalyzer.analyzeDirectory(compiledClassesDir)
        
        println("[编译期调用分析] 发现 ${methodCallContexts.size} 个包含Repository调用的方法")
        
        // 4. 增强字段访问分析
        val fieldAccessAnalyzer = FieldAccessAnalyzer(domainAnalysisResult, aggregateRootClasses)
        val enhancedContexts = fieldAccessAnalyzer.enhanceWithFieldAccess(methodCallContexts)
        
        // 5. 构建最终结果
        val callGraphResult = buildCallGraphResult(enhancedContexts)
        
        // 6. 输出结果
        outputResult(callGraphResult, outputFile)
        
        println("[编译期调用分析] 分析完成，结果已保存到: ${outputFile.absolutePath}")
    }
    
    /**
     * 加载domain-analyzer.json结果
     */
    private fun loadDomainAnalysisResult(domainAnalysisFile: File): Map<String, Any> {
        return if (domainAnalysisFile.exists()) {
            try {
                objectMapper.readValue(domainAnalysisFile)
            } catch (e: Exception) {
                println("[编译期调用分析] 警告: 无法加载domain-analyzer.json: ${e.message}")
                emptyMap()
            }
        } else {
            println("[编译期调用分析] 警告: domain-analyzer.json不存在: ${domainAnalysisFile.absolutePath}")
            emptyMap()
        }
    }
    
    /**
     * 从domain分析结果中提取聚合根类
     */
    private fun extractAggregateRootClasses(domainAnalysisResult: Map<String, Any>): Set<String> {
        val aggregateRoots = mutableSetOf<String>()
        
        @Suppress("UNCHECKED_CAST")
        val classes = domainAnalysisResult["classes"] as? List<Map<String, Any>> ?: return aggregateRoots
        
        for (classInfo in classes) {
            val className = classInfo["className"] as? String ?: continue
            val domainObjectType = classInfo["domainObjectType"] as? String ?: continue
            
            if (domainObjectType == "AGGREGATE_ROOT") {
                aggregateRoots.add(className)
            }
        }
        
        return aggregateRoots
    }
    
    /**
     * 构建调用图分析结果
     */
    private fun buildCallGraphResult(contexts: List<MethodCallContext>): CallGraphAnalysisResult {
        val callGraph = mutableMapOf<String, AggregateRootCallAnalysis>()
        
        // 按聚合根分组
        val contextsByAggregateRoot = contexts.groupBy { context ->
            context.repositoryCall?.aggregateRootClass ?: ""
        }.filterKeys { it.isNotEmpty() }
        
        for ((aggregateRootClass, aggregateContexts) in contextsByAggregateRoot) {
            val methods = mutableMapOf<String, RepositoryMethodAnalysis>()
            
            // 按Repository方法分组
            val contextsByRepoMethod = aggregateContexts.groupBy { context ->
                val repoCall = context.repositoryCall
                "${repoCall?.repositoryMethod}${repoCall?.repositoryMethodDescriptor}"
            }
            
            for ((repoMethodKey, repoContexts) in contextsByRepoMethod) {
                val calls = mutableMapOf<String, MethodCallLocation>()
                
                for (context in repoContexts) {
                    val locationKey = "${context.className}.${context.methodName}+${context.startLine}-${context.endLine}"
                    
                    val repoCall = context.repositoryCall!!
                    val location = MethodCallLocation(
                        methodClass = context.className,
                        method = context.methodName,
                        methodDescriptor = context.methodDescriptor,
                        repository = repoCall.repositoryClass,
                        repositoryMethod = repoCall.repositoryMethod,
                        repositoryMethodDescriptor = repoCall.repositoryMethodDescriptor,
                        aggregateRoot = repoCall.aggregateRootClass,
                        calledAggregateRootMethod = context.aggregateRootMethodCalls,
                        requiredFields = context.requiredFields
                    )
                    
                    calls[locationKey] = location
                }
                
                methods[repoMethodKey] = RepositoryMethodAnalysis(calls)
            }
            
            callGraph[aggregateRootClass] = AggregateRootCallAnalysis(methods)
        }
        
        return CallGraphAnalysisResult(
            timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            callGraph = callGraph
        )
    }
    
    /**
     * 输出分析结果
     */
    private fun outputResult(result: CallGraphAnalysisResult, outputFile: File) {
        outputFile.parentFile?.mkdirs()
        
        objectMapper.writerWithDefaultPrettyPrinter()
            .writeValue(outputFile, result)
    }
}