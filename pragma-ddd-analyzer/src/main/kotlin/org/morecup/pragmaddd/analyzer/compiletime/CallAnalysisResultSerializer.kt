package org.morecup.pragmaddd.analyzer.compiletime

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.morecup.pragmaddd.analyzer.compiletime.model.CallAnalysisResult
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * 调用分析结果序列化器
 * 负责将分析结果序列化为JSON格式，以及从JSON文件读取分析结果
 */
class CallAnalysisResultSerializer {
    
    private val objectMapper = ObjectMapper().registerModule(kotlinModule()).apply {
        // 配置JSON输出格式
        writerWithDefaultPrettyPrinter()
        // 配置日期格式
        findAndRegisterModules()
    }
    
    /**
     * 将分析结果序列化到文件
     * @param result 分析结果
     * @param outputFile 输出文件
     */
    fun serialize(result: CallAnalysisResult, outputFile: File) {
        try {
            // 确保输出目录存在
            outputFile.parentFile?.mkdirs()
            
            // 写入JSON文件
            objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(outputFile, result)
                
            println("Call analysis results serialized to: ${outputFile.absolutePath}")
        } catch (e: Exception) {
            throw RuntimeException("Failed to serialize call analysis results to ${outputFile.absolutePath}", e)
        }
    }
    
    /**
     * 从文件反序列化分析结果
     * @param inputFile 输入文件
     * @return 分析结果
     */
    fun deserialize(inputFile: File): CallAnalysisResult {
        if (!inputFile.exists()) {
            throw IllegalArgumentException("Call analysis file not found: ${inputFile.absolutePath}")
        }
        
        return try {
            objectMapper.readValue<CallAnalysisResult>(inputFile)
        } catch (e: Exception) {
            throw RuntimeException("Failed to deserialize call analysis results from ${inputFile.absolutePath}", e)
        }
    }
    
    /**
     * 从JSON字符串反序列化分析结果
     * @param jsonString JSON字符串
     * @return 分析结果
     */
    fun deserializeFromString(jsonString: String): CallAnalysisResult {
        return try {
            objectMapper.readValue<CallAnalysisResult>(jsonString)
        } catch (e: Exception) {
            throw RuntimeException("Failed to deserialize call analysis results from JSON string", e)
        }
    }
    
    /**
     * 将分析结果序列化为JSON字符串
     * @param result 分析结果
     * @return JSON字符串
     */
    fun serializeToString(result: CallAnalysisResult): String {
        return try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result)
        } catch (e: Exception) {
            throw RuntimeException("Failed to serialize call analysis results to JSON string", e)
        }
    }
    
    /**
     * 验证JSON文件格式
     * @param file JSON文件
     * @return 验证结果和错误信息
     */
    fun validateJsonFile(file: File): ValidationResult {
        if (!file.exists()) {
            return ValidationResult(false, "File does not exist: ${file.absolutePath}")
        }
        
        return try {
            val result = deserialize(file)
            validateCallAnalysisResult(result)
        } catch (e: Exception) {
            ValidationResult(false, "Invalid JSON format: ${e.message}")
        }
    }
    
    /**
     * 验证分析结果内容
     */
    private fun validateCallAnalysisResult(result: CallAnalysisResult): ValidationResult {
        val errors = mutableListOf<String>()
        
        // 检查版本
        if (result.version.isBlank()) {
            errors.add("Version is required")
        }
        
        // 检查时间戳
        if (result.timestamp.isBlank()) {
            errors.add("Timestamp is required")
        }
        
        // 检查调用图
        if (result.callGraph.isEmpty()) {
            errors.add("Call graph is empty")
        }
        
        // 检查每个聚合根的数据完整性
        for ((aggregateRoot, analysis) in result.callGraph) {
            if (aggregateRoot.isBlank()) {
                errors.add("Aggregate root class name is required")
            }
            
            if (analysis.aggregateRootClass != aggregateRoot) {
                errors.add("Inconsistent aggregate root class name: $aggregateRoot vs ${analysis.aggregateRootClass}")
            }
            
            if (analysis.repositoryMethods.isEmpty()) {
                errors.add("No repository methods found for aggregate root: $aggregateRoot")
            }
            
            // 检查Repository方法
            for ((repositoryMethodKey, methodAnalysis) in analysis.repositoryMethods) {
                if (repositoryMethodKey.isBlank()) {
                    errors.add("Repository method key is required for aggregate root: $aggregateRoot")
                }
                
                if (methodAnalysis.methodDescriptor.isBlank()) {
                    errors.add("Method descriptor is required for repository method: $repositoryMethodKey")
                }
                
                if (methodAnalysis.callers.isEmpty()) {
                    errors.add("No callers found for repository method: $aggregateRoot.$repositoryMethodKey")
                }
                
                // 检查调用者
                for ((callerKey, callerAnalysis) in methodAnalysis.callers) {
                    if (callerKey.isBlank()) {
                        errors.add("Caller key is required")
                    }
                    
                    if (callerAnalysis.methodClass.isBlank()) {
                        errors.add("Method class is required for caller: $callerKey")
                    }
                    
                    if (callerAnalysis.method.isBlank()) {
                        errors.add("Method name is required for caller: $callerKey")
                    }
                    
                    if (callerAnalysis.repository.isBlank()) {
                        errors.add("Repository is required for caller: $callerKey")
                    }
                    
                    if (callerAnalysis.aggregateRoot != aggregateRoot) {
                        errors.add("Inconsistent aggregate root for caller $callerKey: expected $aggregateRoot, got ${callerAnalysis.aggregateRoot}")
                    }
                }
            }
        }
        
        return if (errors.isEmpty()) {
            ValidationResult(true, "Valid call analysis result")
        } else {
            ValidationResult(false, errors.joinToString("; "))
        }
    }
    
    /**
     * 合并多个分析结果
     * @param results 多个分析结果
     * @return 合并后的结果
     */
    fun mergeResults(results: List<CallAnalysisResult>): CallAnalysisResult {
        if (results.isEmpty()) {
            throw IllegalArgumentException("Cannot merge empty results list")
        }
        
        if (results.size == 1) {
            return results.first()
        }
        
        val mergedCallGraph = mutableMapOf<String, org.morecup.pragmaddd.analyzer.compiletime.model.AggregateRootAnalysis>()
        val latestTimestamp = results.maxByOrNull { it.timestamp }?.timestamp ?: ""
        
        for (result in results) {
            for ((aggregateRoot, analysis) in result.callGraph) {
                if (mergedCallGraph.containsKey(aggregateRoot)) {
                    // 合并同一聚合根的分析结果
                    val existingAnalysis = mergedCallGraph[aggregateRoot]!!
                    val mergedRepositoryMethods = existingAnalysis.repositoryMethods.toMutableMap()
                    
                    for ((methodKey, methodAnalysis) in analysis.repositoryMethods) {
                        if (mergedRepositoryMethods.containsKey(methodKey)) {
                            // 合并同一方法的调用者
                            val existingMethodAnalysis = mergedRepositoryMethods[methodKey]!!
                            val mergedCallers = existingMethodAnalysis.callers.toMutableMap()
                            mergedCallers.putAll(methodAnalysis.callers)
                            
                            mergedRepositoryMethods[methodKey] = existingMethodAnalysis.copy(
                                callers = mergedCallers
                            )
                        } else {
                            mergedRepositoryMethods[methodKey] = methodAnalysis
                        }
                    }
                    
                    mergedCallGraph[aggregateRoot] = existingAnalysis.copy(
                        repositoryMethods = mergedRepositoryMethods
                    )
                } else {
                    mergedCallGraph[aggregateRoot] = analysis
                }
            }
        }
        
        return CallAnalysisResult(
            version = "1.0",
            timestamp = latestTimestamp,
            callGraph = mergedCallGraph
        )
    }
    
    /**
     * 生成统计报告
     */
    fun generateStatistics(result: CallAnalysisResult): AnalysisStatistics {
        val totalAggregateRoots = result.callGraph.size
        val totalRepositoryMethods = result.callGraph.values.sumOf { it.repositoryMethods.size }
        val totalCallers = result.callGraph.values
            .flatMap { it.repositoryMethods.values }
            .sumOf { it.callers.size }
        
        val totalRequiredFields = result.callGraph.values
            .flatMap { it.repositoryMethods.values }
            .flatMap { it.callers.values }
            .flatMap { it.requiredFields }
            .toSet().size
        
        val aggregateRootToMethodCount = result.callGraph.mapValues { (_, analysis) ->
            analysis.repositoryMethods.size
        }
        
        val aggregateRootToCallerCount = result.callGraph.mapValues { (_, analysis) ->
            analysis.repositoryMethods.values.sumOf { it.callers.size }
        }
        
        return AnalysisStatistics(
            totalAggregateRoots = totalAggregateRoots,
            totalRepositoryMethods = totalRepositoryMethods,
            totalCallers = totalCallers,
            totalRequiredFields = totalRequiredFields,
            aggregateRootToMethodCount = aggregateRootToMethodCount,
            aggregateRootToCallerCount = aggregateRootToCallerCount,
            timestamp = result.timestamp
        )
    }
}

/**
 * 验证结果
 */
data class ValidationResult(
    val isValid: Boolean,
    val message: String
)

/**
 * 分析统计信息
 */
data class AnalysisStatistics(
    val totalAggregateRoots: Int,
    val totalRepositoryMethods: Int,
    val totalCallers: Int,
    val totalRequiredFields: Int,
    val aggregateRootToMethodCount: Map<String, Int>,
    val aggregateRootToCallerCount: Map<String, Int>,
    val timestamp: String
) {
    fun toFormattedString(): String {
        val sb = StringBuilder()
        sb.appendLine("=== 编译期调用分析统计报告 ===")
        sb.appendLine("分析时间: $timestamp")
        sb.appendLine("聚合根总数: $totalAggregateRoots")
        sb.appendLine("Repository方法总数: $totalRepositoryMethods")
        sb.appendLine("调用方总数: $totalCallers")
        sb.appendLine("需要字段总数: $totalRequiredFields")
        sb.appendLine()
        
        sb.appendLine("各聚合根Repository方法数量:")
        aggregateRootToMethodCount.forEach { (aggregateRoot, count) ->
            sb.appendLine("  ${aggregateRoot.substringAfterLast('.')}: $count")
        }
        sb.appendLine()
        
        sb.appendLine("各聚合根调用方数量:")
        aggregateRootToCallerCount.forEach { (aggregateRoot, count) ->
            sb.appendLine("  ${aggregateRoot.substringAfterLast('.')}: $count")
        }
        
        return sb.toString()
    }
}