package org.morecup.pragmaddd.core.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * 编译期分析工具类
 * 提供简化的运行时API，用于读取编译期生成的Repository调用分析结果
 */
object CompileTimeAnalysisUtils {
    
    private val objectMapper: ObjectMapper = jacksonObjectMapper()
    
    // 缓存分析结果避免重复加载
    private val analysisCache = ConcurrentHashMap<String, RepositoryCallAnalysisResult>()
    
    // 当前调用栈跟踪缓存，避免重复分析
    private val currentCallCache = ConcurrentHashMap<String, List<String>>()
    
    /**
     * 获取指定聚合根类型的必需字段
     * 基于当前调用栈自动识别调用方法
     * 
     * @param aggregateRootClass 聚合根类型
     * @return 必需的字段列表
     */
    fun getRequiredFields(aggregateRootClass: Class<*>): List<String> {
        val callerInfo = getCurrentCallerInfo()
        return getRequiredFields(aggregateRootClass, callerInfo.className, callerInfo.methodName, callerInfo.methodDescriptor)
    }
    
    /**
     * 获取指定聚合根类型和调用方法的必需字段
     * 
     * @param aggregateRootClass 聚合根类型
     * @param callerClassName 调用方类名
     * @param callerMethodName 调用方方法名
     * @param callerMethodDescriptor 调用方方法描述符（可选）
     * @return 必需的字段列表
     */
    fun getRequiredFields(
        aggregateRootClass: Class<*>, 
        callerClassName: String, 
        callerMethodName: String,
        callerMethodDescriptor: String? = null
    ): List<String> {
        val cacheKey = "$callerClassName.$callerMethodName${callerMethodDescriptor ?: ""}"
        
        return currentCallCache.getOrPut(cacheKey) {
            try {
                val analysisResult = loadAnalysisResult()
                findRequiredFieldsForCall(
                    analysisResult, 
                    aggregateRootClass.name, 
                    callerClassName, 
                    callerMethodName,
                    callerMethodDescriptor
                )
            } catch (e: Exception) {
                println("[CompileTimeAnalysisUtils] 获取必需字段失败，回退到空列表: ${e.message}")
                emptyList()
            }
        }
    }
    
    /**
     * 获取Repository方法的字段需求映射
     * 
     * @param repositoryClass Repository类型
     * @param repositoryMethodName Repository方法名
     * @return 字段需求映射
     */
    fun getRepositoryFieldMapping(repositoryClass: Class<*>, repositoryMethodName: String): Map<String, List<String>> {
        return try {
            val analysisResult = loadAnalysisResult()
            val repositoryClassName = repositoryClass.name
            
            val resultMap = mutableMapOf<String, List<String>>()
            
            analysisResult.analysis.values.forEach { classAnalysis ->
                classAnalysis.methods.values.forEach { methodAnalysis ->
                    methodAnalysis.repositoryCalls.filter { call ->
                        call.repository == repositoryClassName && call.repositoryMethod == repositoryMethodName
                    }.forEach { call ->
                        val key = "${methodAnalysis.methodClass}.${methodAnalysis.method}"
                        val fields = call.requiredFields ?: emptyList()
                        resultMap[key] = fields
                    }
                }
            }
            
            resultMap
        } catch (e: Exception) {
            println("[CompileTimeAnalysisUtils] 获取Repository字段映射失败: ${e.message}")
            emptyMap()
        }
    }
    
    /**
     * 清除分析缓存（用于测试或热重载）
     */
    fun clearCache() {
        analysisCache.clear()
        currentCallCache.clear()
    }
    
    /**
     * 检查分析结果是否可用
     */
    fun isAnalysisAvailable(): Boolean {
        return try {
            loadAnalysisResult()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 获取分析结果摘要信息
     */
    fun getAnalysisSummary(): AnalysisSummaryInfo? {
        return try {
            val analysisResult = loadAnalysisResult()
            AnalysisSummaryInfo(
                totalClasses = analysisResult.summary.totalClasses,
                totalMethods = analysisResult.summary.totalMethods,
                totalRepositoryCalls = analysisResult.summary.totalRepositoryCalls,
                aggregateRootCount = analysisResult.summary.aggregateRoots.size,
                repositoryCount = analysisResult.summary.repositories.size
            )
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 加载分析结果
     */
    private fun loadAnalysisResult(): RepositoryCallAnalysisResult {
        val cacheKey = "main" // 假设使用main sourceSet
        
        return analysisCache.getOrPut(cacheKey) {
            val resourcePath = "META-INF/pragma-ddd-analyzer/repository-call-analysis.json"
            val inputStream = Thread.currentThread().contextClassLoader.getResourceStream(resourcePath)
                ?: throw IllegalStateException("Repository调用分析结果文件未找到: $resourcePath")
            
            inputStream.use { stream ->
                objectMapper.readValue<RepositoryCallAnalysisResult>(stream)
            }
        }
    }
    
    /**
     * 从分析结果中查找特定调用的必需字段
     */
    private fun findRequiredFieldsForCall(
        analysisResult: RepositoryCallAnalysisResult,
        aggregateRootClassName: String,
        callerClassName: String,
        callerMethodName: String,
        callerMethodDescriptor: String?
    ): List<String> {
        val classAnalysis = analysisResult.analysis[callerClassName] ?: return emptyList()
        
        val matchingMethods = if (callerMethodDescriptor != null) {
            // 精确匹配方法描述符
            listOfNotNull(classAnalysis.methods["$callerMethodName$callerMethodDescriptor"])
        } else {
            // 按方法名模糊匹配
            classAnalysis.methods.values.filter { it.method == callerMethodName }
        }
        
        val allRequiredFields = mutableSetOf<String>()
        
        matchingMethods.forEach { methodAnalysis ->
            methodAnalysis.repositoryCalls.forEach { repoCall ->
                if (repoCall.aggregateRoot == aggregateRootClassName) {
                    repoCall.requiredFields?.let { allRequiredFields.addAll(it) }
                }
            }
        }
        
        return allRequiredFields.toList().sorted()
    }
    
    /**
     * 获取当前调用者信息
     */
    private fun getCurrentCallerInfo(): CallerInfo {
        val stackTrace = Thread.currentThread().stackTrace
        
        // 跳过当前方法和getRequiredFields方法，找到真正的调用者
        for (i in 3 until stackTrace.size) {
            val element = stackTrace[i]
            val className = element.className
            
            // 跳过一些框架类和代理类
            if (!isFrameworkClass(className)) {
                return CallerInfo(
                    className = className,
                    methodName = element.methodName,
                    methodDescriptor = null, // 运行时无法获取精确的方法描述符
                    lineNumber = element.lineNumber
                )
            }
        }
        
        throw IllegalStateException("无法确定调用者信息")
    }
    
    /**
     * 检查是否是框架类（需要跳过的类）
     */
    private fun isFrameworkClass(className: String): Boolean {
        return className.startsWith("org.morecup.pragmaddd.core.repository.CompileTimeAnalysisUtils") ||
               className.contains("\$\$EnhancerBy") || // CGLIB代理
               className.startsWith("java.") ||
               className.startsWith("kotlin.") ||
               className.startsWith("org.springframework.") ||
               className.startsWith("net.sf.cglib.")
    }
}

/**
 * 调用者信息
 */
private data class CallerInfo(
    val className: String,
    val methodName: String,
    val methodDescriptor: String?,
    val lineNumber: Int
)

/**
 * 分析摘要信息
 */
data class AnalysisSummaryInfo(
    val totalClasses: Int,
    val totalMethods: Int,
    val totalRepositoryCalls: Int,
    val aggregateRootCount: Int,
    val repositoryCount: Int
)

/**
 * 扩展函数：为ClassLoader添加getResourceStream方法
 */
private fun ClassLoader.getResourceStream(name: String): InputStream? {
    return getResourceAsStream(name)
}

/**
 * Repository调用分析结果数据模型（简化版本）
 * 对应analyzer模块中的JSON输出格式
 */
private data class RepositoryCallAnalysisResult(
    val version: String,
    val timestamp: Long,
    val summary: JsonAnalysisSummary,
    val analysis: Map<String, JsonClassAnalysis>
)

private data class JsonAnalysisSummary(
    val totalClasses: Int,
    val totalMethods: Int,
    val totalRepositoryCalls: Int,
    val aggregateRoots: List<JsonAggregateRootSummary>,
    val repositories: List<JsonRepositorySummary>
)

private data class JsonAggregateRootSummary(
    val className: String,
    val fieldNames: List<String>
)

private data class JsonRepositorySummary(
    val className: String,
    val repositoryType: String,
    val targetAggregateRoot: String?
)

private data class JsonClassAnalysis(
    val className: String,
    val methods: Map<String, JsonMethodAnalysis>
)

private data class JsonMethodAnalysis(
    val methodClass: String,
    val method: String,
    val methodDescriptor: String,
    val repositoryCalls: List<JsonRepositoryCall>
)

private data class JsonRepositoryCall(
    val repository: String,
    val repositoryMethod: String,
    val repositoryMethodDescriptor: String,
    val aggregateRoot: String?,
    val calledAggregateRootMethod: List<JsonAggregateRootMethodCall>?,
    val requiredFields: List<String>?
)

private data class JsonAggregateRootMethodCall(
    val aggregateRootMethod: String,
    val aggregateRootMethodDescriptor: String,
    val requiredFields: List<String>
)