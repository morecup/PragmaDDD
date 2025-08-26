package org.morecup.pragmaddd.analyzer.callgraph

import com.fasterxml.jackson.databind.ObjectMapper
import org.morecup.pragmaddd.analyzer.model.DetailedAnalysisResult
import org.morecup.pragmaddd.analyzer.model.DetailedClassInfo
import org.morecup.pragmaddd.analyzer.model.PropertyAccessInfo
import java.io.File

/**
 * 字段访问分析器
 * 
 * 基于现有domain-analyzer.json结果，分析Repository方法调用链中访问的聚合根属性，
 * 处理方法嵌套调用和循环依赖
 */
class FieldAccessAnalyzer(
    private val debugMode: Boolean = false
) {
    
    private val objectMapper = ObjectMapper()
    
    /**
     * 字段访问分析结果
     */
    data class FieldAccessResult(
        val repositoryMethodCalls: Map<String, RepositoryMethodFieldAccess>
    )
    
    /**
     * Repository方法字段访问信息
     */
    data class RepositoryMethodFieldAccess(
        val repositoryClass: String,
        val repositoryMethod: String,
        val repositoryMethodDescriptor: String,
        val aggregateRootClass: String,
        val callerMethods: List<CallerMethodFieldAccess>
    )
    
    /**
     * 调用方方法字段访问信息
     */
    data class CallerMethodFieldAccess(
        val callerClass: String,
        val callerMethod: String,
        val callerMethodDescriptor: String,
        val callerLineStart: Int,
        val callerLineEnd: Int,
        val calledAggregateRootMethods: List<AggregateRootMethodAccess>,
        val requiredFields: Set<String>
    )
    
    /**
     * 聚合根方法访问信息
     */
    data class AggregateRootMethodAccess(
        val methodName: String,
        val methodDescriptor: String,
        val requiredFields: Set<String>
    )
    
    /**
     * 分析字段访问
     */
    fun analyzeFieldAccess(
        callGraphResult: CallGraphAnalyzer.CallGraphResult,
        domainAnalysisFile: File
    ): FieldAccessResult {
        if (debugMode) {
            println("[FieldAccessAnalyzer] 开始分析字段访问")
        }
        
        // 读取domain-analyzer.json
        val domainAnalysis = loadDomainAnalysis(domainAnalysisFile)
        if (domainAnalysis == null) {
            if (debugMode) {
                println("[FieldAccessAnalyzer] 无法读取domain-analyzer.json文件")
            }
            return FieldAccessResult(emptyMap())
        }
        
        // 构建聚合根方法访问映射
        val aggregateRootMethodMap = buildAggregateRootMethodMap(domainAnalysis)
        
        // 分析每个Repository方法调用
        val repositoryMethodCalls = mutableMapOf<String, RepositoryMethodFieldAccess>()
        
        // 按Repository方法分组
        val groupedCalls = callGraphResult.repositoryCalls.groupBy { 
            "${it.repositoryClass}.${it.repositoryMethod}${it.repositoryMethodDescriptor}"
        }
        
        groupedCalls.forEach { (repositoryMethodKey, calls) ->
            val firstCall = calls.first()
            val callerMethods = mutableListOf<CallerMethodFieldAccess>()
            
            calls.forEach { call ->
                val callerMethodKey = "${call.callerClass}.${call.callerMethod}${call.callerMethodDescriptor}"
                
                // 分析该调用方法中调用的聚合根方法
                val calledAggregateRootMethods = analyzeCalledAggregateRootMethods(
                    call,
                    callGraphResult.aggregateRootMethodCalls,
                    aggregateRootMethodMap
                )
                
                // 汇总所有需要的字段
                val requiredFields = calledAggregateRootMethods.flatMap { it.requiredFields }.toSet()
                
                callerMethods.add(CallerMethodFieldAccess(
                    callerClass = call.callerClass,
                    callerMethod = call.callerMethod,
                    callerMethodDescriptor = call.callerMethodDescriptor,
                    callerLineStart = call.callerLineStart,
                    callerLineEnd = call.callerLineEnd,
                    calledAggregateRootMethods = calledAggregateRootMethods,
                    requiredFields = requiredFields
                ))
                
                if (debugMode) {
                    println("[FieldAccessAnalyzer] 分析调用方法: $callerMethodKey")
                    println("[FieldAccessAnalyzer] 需要字段: $requiredFields")
                }
            }
            
            repositoryMethodCalls[repositoryMethodKey] = RepositoryMethodFieldAccess(
                repositoryClass = firstCall.repositoryClass,
                repositoryMethod = firstCall.repositoryMethod,
                repositoryMethodDescriptor = firstCall.repositoryMethodDescriptor,
                aggregateRootClass = firstCall.aggregateRootClass,
                callerMethods = callerMethods
            )
        }
        
        if (debugMode) {
            println("[FieldAccessAnalyzer] 字段访问分析完成")
            println("[FieldAccessAnalyzer] Repository方法数量: ${repositoryMethodCalls.size}")
        }
        
        return FieldAccessResult(repositoryMethodCalls)
    }
    
    /**
     * 加载domain-analyzer.json
     */
    private fun loadDomainAnalysis(file: File): DetailedAnalysisResult? {
        return try {
            if (file.exists()) {
                objectMapper.readValue(file, DetailedAnalysisResult::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            if (debugMode) {
                println("[FieldAccessAnalyzer] 读取domain-analyzer.json失败: ${e.message}")
            }
            null
        }
    }
    
    /**
     * 构建聚合根方法访问映射
     */
    private fun buildAggregateRootMethodMap(domainAnalysis: DetailedAnalysisResult): Map<String, PropertyAccessInfo> {
        val methodMap = mutableMapOf<String, PropertyAccessInfo>()
        
        domainAnalysis.classes.forEach { classInfo ->
            classInfo.propertyAccessAnalysis.forEach { methodInfo ->
                val key = "${methodInfo.className}.${methodInfo.methodName}${methodInfo.methodDescriptor}"
                methodMap[key] = methodInfo
            }
        }
        
        return methodMap
    }
    
    /**
     * 分析调用的聚合根方法
     */
    private fun analyzeCalledAggregateRootMethods(
        repositoryCall: CallGraphAnalyzer.MethodCallInfo,
        aggregateRootMethodCalls: Map<String, List<CallGraphAnalyzer.AggregateRootMethodCall>>,
        aggregateRootMethodMap: Map<String, PropertyAccessInfo>
    ): List<AggregateRootMethodAccess> {
        val callerMethodKey = "${repositoryCall.callerClass}.${repositoryCall.callerMethod}${repositoryCall.callerMethodDescriptor}"
        val result = mutableListOf<AggregateRootMethodAccess>()
        val visited = mutableSetOf<String>()
        
        // 查找该调用方法中调用的聚合根方法
        aggregateRootMethodCalls[repositoryCall.aggregateRootClass]?.forEach { aggregateCall ->
            if (aggregateCall.callerClass == repositoryCall.callerClass &&
                aggregateCall.callerMethod == repositoryCall.callerMethod &&
                aggregateCall.callerMethodDescriptor == repositoryCall.callerMethodDescriptor) {
                
                val aggregateMethodKey = "${repositoryCall.aggregateRootClass}.${aggregateCall.methodName}${aggregateCall.methodDescriptor}"
                
                // 递归分析字段访问
                val requiredFields = analyzeMethodFieldAccess(
                    aggregateMethodKey,
                    aggregateRootMethodMap,
                    visited
                )
                
                result.add(AggregateRootMethodAccess(
                    methodName = aggregateCall.methodName,
                    methodDescriptor = aggregateCall.methodDescriptor,
                    requiredFields = requiredFields
                ))
            }
        }
        
        return result
    }
    
    /**
     * 递归分析方法字段访问
     */
    private fun analyzeMethodFieldAccess(
        methodKey: String,
        aggregateRootMethodMap: Map<String, PropertyAccessInfo>,
        visited: MutableSet<String>
    ): Set<String> {
        // 防止循环依赖
        if (visited.contains(methodKey)) {
            return emptySet()
        }
        
        visited.add(methodKey)
        
        val methodInfo = aggregateRootMethodMap[methodKey]
        if (methodInfo == null) {
            visited.remove(methodKey)
            return emptySet()
        }
        
        val requiredFields = mutableSetOf<String>()
        
        // 添加直接访问的属性（只包括读取的属性）
        requiredFields.addAll(methodInfo.accessedProperties)
        
        // 递归分析调用的方法
        methodInfo.calledMethods.forEach { calledMethod ->
            val calledMethodKey = "${calledMethod.className}.${calledMethod.methodName}${calledMethod.methodDescriptor}"
            val nestedFields = analyzeMethodFieldAccess(calledMethodKey, aggregateRootMethodMap, visited)
            requiredFields.addAll(nestedFields)
        }
        
        visited.remove(methodKey)
        return requiredFields
    }
}
