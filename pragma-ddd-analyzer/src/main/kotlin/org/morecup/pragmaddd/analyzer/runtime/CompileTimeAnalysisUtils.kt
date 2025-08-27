package org.morecup.pragmaddd.analyzer.runtime

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.morecup.pragmaddd.analyzer.compiletime.model.CallAnalysisResult
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * 编译期分析运行时工具类
 * 提供简化的API用于在运行时获取预计算的字段访问信息
 */
object CompileTimeAnalysisUtils {
    
    private val objectMapper = ObjectMapper().registerModule(kotlinModule())
    private val analysisResultsCache = ConcurrentHashMap<String, CallAnalysisResult>()
    private var initialized = false
    
    /**
     * 初始化分析结果缓存
     * 通常在应用启动时调用
     */
    fun initialize(callAnalysisResourcePath: String = "/META-INF/pragma-ddd-analyzer/call-analysis.json") {
        if (initialized) return
        
        try {
            val inputStream = javaClass.getResourceAsStream(callAnalysisResourcePath)
            if (inputStream != null) {
                val analysisResult: CallAnalysisResult = objectMapper.readValue(inputStream)
                analysisResultsCache["default"] = analysisResult
                initialized = true
                println("[CompileTimeAnalysisUtils] Initialized with analysis results from: $callAnalysisResourcePath")
            } else {
                println("[CompileTimeAnalysisUtils] Analysis results not found at: $callAnalysisResourcePath")
            }
        } catch (e: Exception) {
            println("[CompileTimeAnalysisUtils] Failed to initialize: ${e.message}")
        }
    }
    
    /**
     * 获取指定聚合根在当前调用上下文中需要的字段
     * @param aggregateRootClass 聚合根类
     * @param callerStackOffset 调用者堆栈偏移量，默认为2（跳过当前方法和直接调用者）
     * @return 需要的字段集合
     */
    fun getRequiredFields(
        aggregateRootClass: Class<*>,
        callerStackOffset: Int = 2
    ): Set<String> {
        if (!initialized) {
            initialize()
        }
        
        val stackTrace = Thread.currentThread().stackTrace
        if (stackTrace.size <= callerStackOffset) {
            return emptySet()
        }
        
        val caller = identifyBusinessCaller(stackTrace, callerStackOffset)
        return findRequiredFieldsForCaller(aggregateRootClass.name, caller)
    }
    
    /**
     * 获取指定聚合根在当前调用上下文中需要的字段（Kotlin版本）
     */
    fun getRequiredFields(
        aggregateRootClass: KClass<*>,
        callerStackOffset: Int = 2
    ): Set<String> {
        return getRequiredFields(aggregateRootClass.java, callerStackOffset)
    }
    
    /**
     * 根据方法信息直接获取需要的字段
     * @param aggregateRootClass 聚合根类名
     * @param callerClass 调用者类名
     * @param callerMethod 调用者方法名
     * @param repositoryMethod Repository方法名（可选）
     * @return 需要的字段集合
     */
    fun getRequiredFieldsByMethod(
        aggregateRootClass: String,
        callerClass: String,
        callerMethod: String,
        repositoryMethod: String? = null
    ): Set<String> {
        if (!initialized) {
            initialize()
        }
        
        val analysisResult = analysisResultsCache["default"] ?: return emptySet()
        val aggregateAnalysis = analysisResult.callGraph[aggregateRootClass] ?: return emptySet()
        
        for ((_, methodAnalysis) in aggregateAnalysis.repositoryMethods) {
            // 如果指定了Repository方法，则只查找匹配的方法
            if (repositoryMethod != null && !methodAnalysis.toString().contains(repositoryMethod)) {
                continue
            }
            
            for ((callerKey, callerAnalysis) in methodAnalysis.callers) {
                if (callerAnalysis.methodClass == callerClass && callerAnalysis.method == callerMethod) {
                    return callerAnalysis.requiredFields
                }
            }
        }
        
        return emptySet()
    }
    
    /**
     * 获取所有已分析的聚合根类名
     */
    fun getAllAnalyzedAggregateRoots(): Set<String> {
        if (!initialized) {
            initialize()
        }
        
        return analysisResultsCache["default"]?.callGraph?.keys ?: emptySet()
    }
    
    /**
     * 获取指定聚合根的所有Repository方法
     */
    fun getRepositoryMethods(aggregateRootClass: String): Set<String> {
        if (!initialized) {
            initialize()
        }
        
        val analysisResult = analysisResultsCache["default"] ?: return emptySet()
        val aggregateAnalysis = analysisResult.callGraph[aggregateRootClass] ?: return emptySet()
        
        return aggregateAnalysis.repositoryMethods.keys
    }
    
    /**
     * 检查是否存在分析结果
     */
    fun hasAnalysisResults(): Boolean {
        return initialized && analysisResultsCache.isNotEmpty()
    }
    
    /**
     * 获取分析结果的详细信息（用于调试）
     */
    fun getAnalysisInfo(): AnalysisInfo {
        if (!initialized) {
            initialize()
        }
        
        val analysisResult = analysisResultsCache["default"]
        return if (analysisResult != null) {
            AnalysisInfo(
                version = analysisResult.version,
                timestamp = analysisResult.timestamp,
                aggregateRootCount = analysisResult.callGraph.size,
                repositoryMethodCount = analysisResult.callGraph.values.sumOf { it.repositoryMethods.size },
                callerCount = analysisResult.callGraph.values
                    .flatMap { it.repositoryMethods.values }
                    .sumOf { it.callers.size }
            )
        } else {
            AnalysisInfo("unknown", "unknown", 0, 0, 0)
        }
    }
    
    /**
     * 清除缓存（主要用于测试）
     */
    fun clearCache() {
        analysisResultsCache.clear()
        initialized = false
    }
    
    /**
     * 手动加载分析结果（用于自定义加载）
     */
    fun loadAnalysisResults(analysisResult: CallAnalysisResult, key: String = "default") {
        analysisResultsCache[key] = analysisResult
        initialized = true
    }
    
    private fun identifyBusinessCaller(
        stackTrace: Array<StackTraceElement>,
        offset: Int
    ): StackTraceElement? {
        for (i in offset until stackTrace.size) {
            val element = stackTrace[i]
            val className = element.className
            
            // 跳过框架类和代理类
            if (isBusinessClass(className)) {
                return element
            }
        }
        return null
    }
    
    private fun isBusinessClass(className: String): Boolean {
        return !className.contains("\$\$EnhancerByCGLIB\$\$") &&
               !className.contains("\$\$FastClassByCGLIB\$\$") &&
               !className.contains("\$Proxy") &&
               !className.startsWith("java.") &&
               !className.startsWith("kotlin.") &&
               !className.startsWith("org.springframework.") &&
               !className.startsWith("org.gradle.") &&
               !className.startsWith("sun.") &&
               !className.startsWith("com.sun.") &&
               !className.contains("\$") // 简化：排除内部类
    }
    
    private fun findRequiredFieldsForCaller(
        aggregateRootClass: String,
        caller: StackTraceElement?
    ): Set<String> {
        if (caller == null) return emptySet()
        
        val analysisResult = analysisResultsCache["default"] ?: return emptySet()
        val aggregateAnalysis = analysisResult.callGraph[aggregateRootClass] ?: return emptySet()
        
        for ((_, methodAnalysis) in aggregateAnalysis.repositoryMethods) {
            for ((callerKey, callerAnalysis) in methodAnalysis.callers) {
                if (matchesCaller(callerAnalysis, caller)) {
                    return callerAnalysis.requiredFields
                }
            }
        }
        
        return emptySet()
    }
    
    private fun matchesCaller(
        callerAnalysis: org.morecup.pragmaddd.analyzer.compiletime.model.CallerMethodAnalysis,
        caller: StackTraceElement
    ): Boolean {
        return callerAnalysis.methodClass == caller.className &&
               callerAnalysis.method == caller.methodName &&
               // 可选：检查行号范围
               isWithinLineRange(caller.lineNumber, callerAnalysis.sourceLines)
    }
    
    private fun isWithinLineRange(lineNumber: Int, sourceLines: String): Boolean {
        if (lineNumber < 0 || sourceLines == "unknown") return true
        
        return try {
            val parts = sourceLines.split("-")
            if (parts.size == 2) {
                val startLine = parts[0].toInt()
                val endLine = parts[1].toInt()
                lineNumber in startLine..endLine
            } else {
                true // 无法解析行号范围，认为匹配
            }
        } catch (e: NumberFormatException) {
            true
        }
    }
}

/**
 * 分析信息
 */
data class AnalysisInfo(
    val version: String,
    val timestamp: String,
    val aggregateRootCount: Int,
    val repositoryMethodCount: Int,
    val callerCount: Int
) {
    override fun toString(): String {
        return "AnalysisInfo(version='$version', timestamp='$timestamp', " +
               "aggregateRoots=$aggregateRootCount, repositoryMethods=$repositoryMethodCount, " +
               "callers=$callerCount)"
    }
}

/**
 * 字段访问上下文
 * 用于更精确的字段访问控制
 */
data class FieldAccessContext(
    val aggregateRootClass: String,
    val repositoryClass: String,
    val repositoryMethod: String,
    val callerClass: String,
    val callerMethod: String,
    val requiredFields: Set<String>
) {
    /**
     * 检查指定字段是否被需要
     */
    fun isFieldRequired(fieldName: String): Boolean {
        return requiredFields.contains(fieldName) ||
               requiredFields.any { it.endsWith(".$fieldName") }
    }
    
    /**
     * 获取嵌套对象的需要字段
     */
    fun getNestedRequiredFields(objectName: String): Set<String> {
        val prefix = "$objectName."
        return requiredFields
            .filter { it.startsWith(prefix) }
            .map { it.removePrefix(prefix) }
            .toSet()
    }
}

/**
 * 字段访问上下文构建器
 */
object FieldAccessContextBuilder {
    
    /**
     * 从当前调用上下文构建字段访问上下文
     */
    fun fromCurrentContext(
        aggregateRootClass: Class<*>,
        repositoryClass: Class<*>,
        repositoryMethod: String
    ): FieldAccessContext? {
        val stackTrace = Thread.currentThread().stackTrace
        val caller = identifyBusinessCaller(stackTrace)
        
        if (caller != null) {
            val requiredFields = CompileTimeAnalysisUtils.getRequiredFieldsByMethod(
                aggregateRootClass.name,
                caller.className,
                caller.methodName,
                repositoryMethod
            )
            
            return FieldAccessContext(
                aggregateRootClass = aggregateRootClass.name,
                repositoryClass = repositoryClass.name,
                repositoryMethod = repositoryMethod,
                callerClass = caller.className,
                callerMethod = caller.methodName,
                requiredFields = requiredFields
            )
        }
        
        return null
    }
    
    private fun identifyBusinessCaller(stackTrace: Array<StackTraceElement>): StackTraceElement? {
        for (i in 2 until stackTrace.size) { // 跳过当前方法和直接调用者
            val element = stackTrace[i]
            val className = element.className
            
            if (!className.contains("\$\$EnhancerByCGLIB\$\$") &&
                !className.contains("\$Proxy") &&
                !className.startsWith("java.") &&
                !className.startsWith("kotlin.") &&
                !className.startsWith("org.springframework.")) {
                return element
            }
        }
        return null
    }
}