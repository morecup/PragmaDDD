package org.morecup.pragmaddd.core.callanalysis

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.InputStream

/**
 * 编译期分析结果运行时工具类
 * 
 * 提供简化的API来获取预计算的字段访问信息
 */
object CompileTimeAnalysisUtils {
    
    private val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
    private var callAnalysisCache: Map<String, Any>? = null
    
    /**
     * 获取指定聚合根类型所需的字段集合
     * 
     * @param aggregateRootClass 聚合根类
     * @param callerClass 调用方类（可选，用于更精确的匹配）
     * @param callerMethod 调用方法（可选，用于更精确的匹配）
     * @return 需要加载的字段集合
     */
    fun getRequiredFields(
        aggregateRootClass: Class<*>,
        callerClass: Class<*>? = null,
        callerMethod: String? = null
    ): Set<String> {
        return getRequiredFields(
            aggregateRootClass.name,
            callerClass?.name,
            callerMethod
        )
    }
    
    /**
     * 获取指定聚合根类型所需的字段集合
     * 
     * @param aggregateRootClassName 聚合根类名
     * @param callerClassName 调用方类名（可选）
     * @param callerMethodName 调用方法名（可选）
     * @return 需要加载的字段集合
     */
    fun getRequiredFields(
        aggregateRootClassName: String,
        callerClassName: String? = null,
        callerMethodName: String? = null
    ): Set<String> {
        val analysisResult = getCallAnalysisResult()
        
        @Suppress("UNCHECKED_CAST")
        val callGraph = analysisResult["callGraph"] as? Map<String, Any> ?: return emptySet()
        
        @Suppress("UNCHECKED_CAST")
        val aggregateRootAnalysis = callGraph[aggregateRootClassName] as? Map<String, Any> ?: return emptySet()
        
        @Suppress("UNCHECKED_CAST")
        val methods = aggregateRootAnalysis["methods"] as? Map<String, Any> ?: return emptySet()
        
        val allFields = mutableSetOf<String>()
        
        // 如果提供了调用方信息，尝试精确匹配
        if (callerClassName != null && callerMethodName != null) {
            val exactFields = findExactMatch(methods, callerClassName, callerMethodName)
            if (exactFields.isNotEmpty()) {
                return exactFields
            }
        }
        
        // 否则返回所有相关字段的并集
        for ((_, methodAnalysis) in methods) {
            @Suppress("UNCHECKED_CAST")
            val methodAnalysisMap = methodAnalysis as? Map<String, Any> ?: continue
            
            @Suppress("UNCHECKED_CAST")
            val calls = methodAnalysisMap["calls"] as? Map<String, Any> ?: continue
            
            for ((_, callLocation) in calls) {
                @Suppress("UNCHECKED_CAST")
                val locationMap = callLocation as? Map<String, Any> ?: continue
                
                @Suppress("UNCHECKED_CAST")
                val requiredFields = locationMap["requiredFields"] as? List<String> ?: continue
                
                allFields.addAll(requiredFields)
            }
        }
        
        return allFields
    }
    
    /**
     * 查找精确匹配的字段集合
     */
    private fun findExactMatch(
        methods: Map<String, Any>,
        callerClassName: String,
        callerMethodName: String
    ): Set<String> {
        for ((_, methodAnalysis) in methods) {
            @Suppress("UNCHECKED_CAST")
            val methodAnalysisMap = methodAnalysis as? Map<String, Any> ?: continue
            
            @Suppress("UNCHECKED_CAST")
            val calls = methodAnalysisMap["calls"] as? Map<String, Any> ?: continue
            
            for ((_, callLocation) in calls) {
                @Suppress("UNCHECKED_CAST")
                val locationMap = callLocation as? Map<String, Any> ?: continue
                
                val methodClass = locationMap["methodClass"] as? String ?: continue
                val method = locationMap["method"] as? String ?: continue
                
                if (methodClass == callerClassName && method == callerMethodName) {
                    @Suppress("UNCHECKED_CAST")
                    val requiredFields = locationMap["requiredFields"] as? List<String> ?: continue
                    return requiredFields.toSet()
                }
            }
        }
        
        return emptySet()
    }
    
    /**
     * 获取调用分析结果
     */
    private fun getCallAnalysisResult(): Map<String, Any> {
        if (callAnalysisCache == null) {
            callAnalysisCache = loadCallAnalysisResult()
        }
        return callAnalysisCache ?: emptyMap()
    }
    
    /**
     * 加载调用分析结果
     */
    private fun loadCallAnalysisResult(): Map<String, Any> {
        return try {
            val resourcePath = "/META-INF/pragma-ddd-analyzer/call-analysis.json"
            val inputStream: InputStream? = CompileTimeAnalysisUtils::class.java.getResourceAsStream(resourcePath)
            
            if (inputStream != null) {
                inputStream.use { stream ->
                    objectMapper.readValue(stream)
                }
            } else {
                println("[CompileTimeAnalysisUtils] 警告: 未找到调用分析结果文件: $resourcePath")
                emptyMap()
            }
        } catch (e: Exception) {
            println("[CompileTimeAnalysisUtils] 警告: 加载调用分析结果失败: ${e.message}")
            emptyMap()
        }
    }
    
    /**
     * 清除缓存（主要用于测试）
     */
    fun clearCache() {
        callAnalysisCache = null
    }
    
    /**
     * 获取所有聚合根的调用分析信息
     */
    fun getAllAggregateRootAnalysis(): Map<String, Any> {
        val analysisResult = getCallAnalysisResult()
        
        @Suppress("UNCHECKED_CAST")
        return analysisResult["callGraph"] as? Map<String, Any> ?: emptyMap()
    }
    
    /**
     * 检查是否有可用的调用分析结果
     */
    fun hasAnalysisResult(): Boolean {
        return getCallAnalysisResult().isNotEmpty()
    }
}