package org.morecup.pragmaddd.core.callanalysis

import com.fasterxml.jackson.databind.ObjectMapper
import java.io.InputStream

/**
 * 编译期分析工具类
 * 
 * 提供简化的运行时API，直接读取预计算的字段访问结果
 */
object CompileTimeAnalysisUtils {
    
    private val objectMapper = ObjectMapper()
    private var cache: CallAnalysisCache? = null
    private var cacheLoaded = false
    
    /**
     * 调用分析缓存数据
     */
    data class CallAnalysisCache(
        val version: String,
        val timestamp: String,
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
     * 获取聚合根所需的字段
     * 
     * @param aggregateRootClass 聚合根类
     * @return 需要的字段集合
     */
    fun getRequiredFields(aggregateRootClass: Class<*>): Set<String> {
        return getRequiredFields(aggregateRootClass.name)
    }
    
    /**
     * 获取聚合根所需的字段
     * 
     * @param aggregateRootClassName 聚合根类名
     * @return 需要的字段集合
     */
    fun getRequiredFields(aggregateRootClassName: String): Set<String> {
        val callGraph = getCallGraph(aggregateRootClassName) ?: return emptySet()
        
        // 获取当前调用栈
        val stackTrace = Thread.currentThread().stackTrace
        val callerInfo = findCallerInfo(stackTrace)
        
        if (callerInfo != null) {
            // 查找匹配的调用方法
            val matchingCaller = findMatchingCaller(callGraph, callerInfo)
            if (matchingCaller != null) {
                return matchingCaller.requiredFields
            }
        }
        
        // 如果找不到精确匹配，返回该聚合根的所有可能字段
        return getAllPossibleFields(callGraph)
    }
    
    /**
     * 获取Repository方法所需的字段
     * 
     * @param aggregateRootClass 聚合根类
     * @param repositoryMethod Repository方法名
     * @return 需要的字段集合
     */
    fun getRequiredFieldsForRepositoryMethod(
        aggregateRootClass: Class<*>,
        repositoryMethod: String
    ): Set<String> {
        return getRequiredFieldsForRepositoryMethod(aggregateRootClass.name, repositoryMethod)
    }
    
    /**
     * 获取Repository方法所需的字段
     * 
     * @param aggregateRootClassName 聚合根类名
     * @param repositoryMethod Repository方法名
     * @return 需要的字段集合
     */
    fun getRequiredFieldsForRepositoryMethod(
        aggregateRootClassName: String,
        repositoryMethod: String
    ): Set<String> {
        val callGraph = getCallGraph(aggregateRootClassName) ?: return emptySet()
        
        // 查找匹配的Repository方法
        val repositoryMethodGraph = callGraph.repositoryMethods.values
            .find { it.repositoryMethod == repositoryMethod }
        
        if (repositoryMethodGraph != null) {
            // 返回该Repository方法的所有调用方需要的字段的并集
            return repositoryMethodGraph.callerMethods.values
                .flatMap { it.requiredFields }
                .toSet()
        }
        
        return emptySet()
    }
    
    /**
     * 检查缓存是否可用
     */
    fun isCacheAvailable(): Boolean {
        loadCacheIfNeeded()
        return cache != null
    }
    
    /**
     * 获取缓存版本信息
     */
    fun getCacheVersion(): String? {
        loadCacheIfNeeded()
        return cache?.version
    }
    
    /**
     * 获取缓存时间戳
     */
    fun getCacheTimestamp(): String? {
        loadCacheIfNeeded()
        return cache?.timestamp
    }
    
    /**
     * 清理缓存（用于测试）
     */
    fun clearCache() {
        cache = null
        cacheLoaded = false
    }
    
    /**
     * 加载缓存（如果需要）
     */
    private fun loadCacheIfNeeded() {
        if (!cacheLoaded) {
            cache = loadCache()
            cacheLoaded = true
        }
    }
    
    /**
     * 加载缓存文件
     */
    private fun loadCache(): CallAnalysisCache? {
        return try {
            val inputStream = getResourceAsStream("META-INF/pragma-ddd-analyzer/call-analysis.json")
            if (inputStream != null) {
                objectMapper.readValue(inputStream, CallAnalysisCache::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            // 静默失败，返回null
            null
        }
    }
    
    /**
     * 获取资源流
     */
    private fun getResourceAsStream(path: String): InputStream? {
        return Thread.currentThread().contextClassLoader?.getResourceAsStream(path)
            ?: CompileTimeAnalysisUtils::class.java.classLoader.getResourceAsStream(path)
    }
    
    /**
     * 获取调用图
     */
    private fun getCallGraph(aggregateRootClassName: String): AggregateRootCallGraph? {
        loadCacheIfNeeded()
        return cache?.callGraph?.get(aggregateRootClassName)
    }
    
    /**
     * 查找调用方信息
     */
    private fun findCallerInfo(stackTrace: Array<StackTraceElement>): StackTraceElement? {
        // 跳过当前类和Repository实现类的方法
        for (i in 2 until stackTrace.size) {
            val element = stackTrace[i]
            val className = element.className
            
            // 跳过代理类、框架类等
            if (!className.contains("\$\$EnhancerByCGLIB\$\$") &&
                !className.contains("\$\$EnhancerBySpringCGLIB\$\$") &&
                !className.contains("\$\$FastClassByCGLIB\$\$") &&
                !className.contains("\$\$FastClassBySpringCGLIB\$\$") &&
                !className.startsWith("java.") &&
                !className.startsWith("org.springframework.") &&
                !className.startsWith("net.sf.cglib.") &&
                !className.startsWith("org.morecup.pragmaddd.core.callanalysis.")) {
                return element
            }
        }
        return null
    }
    
    /**
     * 查找匹配的调用方
     */
    private fun findMatchingCaller(
        callGraph: AggregateRootCallGraph,
        callerInfo: StackTraceElement
    ): CallerMethodCallGraph? {
        for (repositoryMethod in callGraph.repositoryMethods.values) {
            for (caller in repositoryMethod.callerMethods.values) {
                if (caller.methodClass == callerInfo.className &&
                    caller.method == callerInfo.methodName) {
                    return caller
                }
            }
        }
        return null
    }
    
    /**
     * 获取所有可能的字段
     */
    private fun getAllPossibleFields(callGraph: AggregateRootCallGraph): Set<String> {
        return callGraph.repositoryMethods.values
            .flatMap { it.callerMethods.values }
            .flatMap { it.requiredFields }
            .toSet()
    }
}
