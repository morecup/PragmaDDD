package org.morecup.pragmaddd.analyzer.callgraph

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import java.io.File
import java.time.Instant

/**
 * 编译期缓存生成器
 * 
 * 将分析结果序列化为JSON格式的call-analysis.json缓存文件，
 * 支持增量更新和缓存失效检测
 */
class CacheGenerator(
    private val debugMode: Boolean = false
) {
    
    private val objectMapper = ObjectMapper().apply {
        enable(SerializationFeature.INDENT_OUTPUT)
        setSerializationInclusion(JsonInclude.Include.NON_NULL)
    }
    
    /**
     * 调用分析缓存数据
     */
    data class CallAnalysisCache(
        val version: String = "1.0",
        val timestamp: String = Instant.now().toString(),
        val callGraph: Map<String, AggregateRootCallGraph>
    )
    
    /**
     * 聚合根调用图
     */
    data class AggregateRootCallGraph(
        val aggregateRootClass: String,
        val repositoryMethods: Map<String, RepositoryMethodCallGraph>
    )
    
    /**
     * Repository方法调用图
     */
    data class RepositoryMethodCallGraph(
        val repositoryClass: String,
        val repositoryMethod: String,
        val repositoryMethodDescriptor: String,
        val callerMethods: Map<String, CallerMethodCallGraph>
    )
    
    /**
     * 调用方方法调用图
     */
    data class CallerMethodCallGraph(
        val methodClass: String,
        val method: String,
        val methodDescriptor: String,
        val repository: String,
        val repositoryMethod: String,
        val repositoryMethodDescriptor: String,
        val aggregateRoot: String,
        val calledAggregateRootMethod: List<CalledAggregateRootMethod>,
        val requiredFields: Set<String>
    )
    
    /**
     * 调用的聚合根方法
     */
    data class CalledAggregateRootMethod(
        val aggregateRootMethod: String,
        val aggregateRootMethodDescriptor: String,
        val requiredFields: Set<String>
    )
    
    /**
     * 生成缓存文件
     */
    fun generateCache(
        fieldAccessResult: FieldAccessAnalyzer.FieldAccessResult,
        outputFile: File
    ) {
        if (debugMode) {
            println("[CacheGenerator] 开始生成缓存文件: ${outputFile.absolutePath}")
        }
        
        // 构建调用图数据结构
        val callGraph = buildCallGraph(fieldAccessResult)
        
        // 创建缓存对象
        val cache = CallAnalysisCache(callGraph = callGraph)
        
        // 确保输出目录存在
        outputFile.parentFile?.mkdirs()
        
        // 写入文件
        try {
            objectMapper.writeValue(outputFile, cache)
            
            if (debugMode) {
                println("[CacheGenerator] 缓存文件生成成功")
                println("[CacheGenerator] 聚合根数量: ${callGraph.size}")
                println("[CacheGenerator] 总调用方法数量: ${callGraph.values.sumOf { it.repositoryMethods.values.sumOf { rm -> rm.callerMethods.size } }}")
            }
        } catch (e: Exception) {
            if (debugMode) {
                println("[CacheGenerator] 缓存文件生成失败: ${e.message}")
            }
            throw e
        }
    }
    
    /**
     * 构建调用图数据结构
     */
    private fun buildCallGraph(fieldAccessResult: FieldAccessAnalyzer.FieldAccessResult): Map<String, AggregateRootCallGraph> {
        val callGraph = mutableMapOf<String, AggregateRootCallGraph>()
        
        // 按聚合根分组
        val groupedByAggregateRoot = fieldAccessResult.repositoryMethodCalls.values
            .groupBy { it.aggregateRootClass }
        
        groupedByAggregateRoot.forEach { (aggregateRootClass, repositoryMethods) ->
            val repositoryMethodsMap = mutableMapOf<String, RepositoryMethodCallGraph>()
            
            repositoryMethods.forEach { repositoryMethod ->
                val repositoryMethodKey = "${repositoryMethod.repositoryMethod}${repositoryMethod.repositoryMethodDescriptor}"
                val callerMethodsMap = mutableMapOf<String, CallerMethodCallGraph>()
                
                repositoryMethod.callerMethods.forEach { callerMethod ->
                    val callerMethodKey = "${callerMethod.callerClass}.${callerMethod.callerMethod}+${callerMethod.callerLineStart}-${callerMethod.callerLineEnd}"
                    
                    val calledAggregateRootMethods = callerMethod.calledAggregateRootMethods.map { method ->
                        CalledAggregateRootMethod(
                            aggregateRootMethod = method.methodName,
                            aggregateRootMethodDescriptor = method.methodDescriptor,
                            requiredFields = method.requiredFields
                        )
                    }
                    
                    callerMethodsMap[callerMethodKey] = CallerMethodCallGraph(
                        methodClass = callerMethod.callerClass,
                        method = callerMethod.callerMethod,
                        methodDescriptor = callerMethod.callerMethodDescriptor,
                        repository = repositoryMethod.repositoryClass,
                        repositoryMethod = repositoryMethod.repositoryMethod,
                        repositoryMethodDescriptor = repositoryMethod.repositoryMethodDescriptor,
                        aggregateRoot = repositoryMethod.aggregateRootClass,
                        calledAggregateRootMethod = calledAggregateRootMethods,
                        requiredFields = callerMethod.requiredFields
                    )
                }
                
                repositoryMethodsMap[repositoryMethodKey] = RepositoryMethodCallGraph(
                    repositoryClass = repositoryMethod.repositoryClass,
                    repositoryMethod = repositoryMethod.repositoryMethod,
                    repositoryMethodDescriptor = repositoryMethod.repositoryMethodDescriptor,
                    callerMethods = callerMethodsMap
                )
            }
            
            callGraph[aggregateRootClass] = AggregateRootCallGraph(
                aggregateRootClass = aggregateRootClass,
                repositoryMethods = repositoryMethodsMap
            )
        }
        
        return callGraph
    }
    
    /**
     * 检查缓存是否需要更新
     */
    fun shouldUpdateCache(
        cacheFile: File,
        sourceFiles: List<File>
    ): Boolean {
        if (!cacheFile.exists()) {
            return true
        }
        
        val cacheLastModified = cacheFile.lastModified()
        
        // 检查源文件是否有更新
        for (sourceFile in sourceFiles) {
            if (sourceFile.lastModified() > cacheLastModified) {
                if (debugMode) {
                    println("[CacheGenerator] 检测到源文件更新: ${sourceFile.name}")
                }
                return true
            }
        }
        
        return false
    }
    
    /**
     * 读取现有缓存
     */
    fun readCache(cacheFile: File): CallAnalysisCache? {
        return try {
            if (cacheFile.exists()) {
                objectMapper.readValue(cacheFile, CallAnalysisCache::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            if (debugMode) {
                println("[CacheGenerator] 读取缓存文件失败: ${e.message}")
            }
            null
        }
    }
    
    /**
     * 获取缓存文件路径
     */
    fun getCacheFilePath(buildDir: File, sourceSetName: String): File {
        return File(buildDir, "generated/pragmaddd/$sourceSetName/resources/META-INF/pragma-ddd-analyzer/call-analysis.json")
    }
    
    /**
     * 清理过期缓存
     */
    fun cleanupCache(cacheFile: File) {
        try {
            if (cacheFile.exists()) {
                cacheFile.delete()
                if (debugMode) {
                    println("[CacheGenerator] 清理缓存文件: ${cacheFile.absolutePath}")
                }
            }
        } catch (e: Exception) {
            if (debugMode) {
                println("[CacheGenerator] 清理缓存文件失败: ${e.message}")
            }
        }
    }
}
