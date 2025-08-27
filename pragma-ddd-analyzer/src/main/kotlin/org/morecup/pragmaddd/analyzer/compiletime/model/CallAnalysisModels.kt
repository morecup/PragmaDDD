package org.morecup.pragmaddd.analyzer.compiletime.model

import com.fasterxml.jackson.annotation.JsonInclude

/**
 * 编译期调用分析完整结果
 */
data class CallAnalysisResult(
    val version: String = "1.0",
    val timestamp: String,
    val callGraph: Map<String, AggregateRootAnalysis>
)

/**
 * 聚合根分析结果
 */
data class AggregateRootAnalysis(
    val aggregateRootClass: String,
    val repositoryMethods: Map<String, RepositoryMethodAnalysis>
)

/**
 * Repository方法分析结果
 */
data class RepositoryMethodAnalysis(
    val methodDescriptor: String,
    val callers: Map<String, CallerMethodAnalysis>
)

/**
 * 调用方方法分析结果
 */
data class CallerMethodAnalysis(
    val methodClass: String,
    val method: String,
    val methodDescriptor: String,
    val sourceLines: String, // "15-20"格式，表示源码行号范围
    val repository: String,
    val repositoryMethod: String,
    val repositoryMethodDescriptor: String,
    val aggregateRoot: String,
    val calledAggregateRootMethods: List<CalledMethodInfo>,
    val requiredFields: Set<String>
)

/**
 * 被调用的聚合根方法信息
 */
data class CalledMethodInfo(
    val aggregateRootMethod: String,
    val aggregateRootMethodDescriptor: String,
    val requiredFields: Set<String>
)

/**
 * 方法调用信息（用于中间分析）
 */
data class MethodInfo(
    val className: String,
    val methodName: String,
    val descriptor: String,
    val sourceLines: Pair<Int, Int>? = null // 开始行号和结束行号
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MethodInfo) return false
        return className == other.className && 
               methodName == other.methodName && 
               descriptor == other.descriptor
    }

    override fun hashCode(): Int {
        return className.hashCode() * 31 + methodName.hashCode() * 31 + descriptor.hashCode()
    }
}

/**
 * Repository方法调用信息
 */
data class RepositoryCallInfo(
    val callerMethod: MethodInfo,
    val repositoryClass: String,
    val repositoryMethod: String,
    val repositoryMethodDescriptor: String,
    val aggregateRootClass: String
)

/**
 * 调用图数据结构
 */
class CallGraph {
    private val repositoryCalls = mutableListOf<RepositoryCallInfo>()
    private val methodCalls = mutableMapOf<MethodInfo, MutableSet<MethodInfo>>()

    fun addRepositoryCall(callInfo: RepositoryCallInfo) {
        repositoryCalls.add(callInfo)
    }

    fun addMethodCall(caller: MethodInfo, called: MethodInfo) {
        methodCalls.computeIfAbsent(caller) { mutableSetOf() }.add(called)
    }

    fun getRepositoryCalls(): List<RepositoryCallInfo> = repositoryCalls.toList()

    fun getMethodCalls(method: MethodInfo): Set<MethodInfo> = 
        methodCalls[method] ?: emptySet()

    fun getAllCallers(): Set<MethodInfo> = methodCalls.keys

    fun getAllCalledMethods(): Set<MethodInfo> = 
        methodCalls.values.flatten().toSet()
}

/**
 * Repository识别配置
 */
data class RepositoryIdentificationConfig(
    val namingRules: List<String> = listOf(
        "{AggregateRoot}Repository",
        "I{AggregateRoot}Repository", 
        "{AggregateRoot}Repo"
    ),
    val includePackages: List<String> = listOf("**"),
    val excludePackages: List<String> = listOf("**.test.**", "**.tests.**")
)

/**
 * 聚合根-Repository映射关系
 */
data class AggregateRootRepositoryMapping(
    val aggregateRootClass: String,
    val repositoryClass: String,
    val matchType: RepositoryMatchType
)

/**
 * Repository匹配类型
 */
enum class RepositoryMatchType {
    GENERIC_INTERFACE,      // DomainRepository<T>泛型接口
    ANNOTATION,             // @DomainRepository注解
    NAMING_CONVENTION       // 命名约定
}

/**
 * 字段访问分析配置
 */
data class FieldAccessAnalysisConfig(
    val maxRecursionDepth: Int = 10,
    val enableCircularDependencyDetection: Boolean = true,
    val excludeSetterMethods: Boolean = true, // 排除setter方法的字段访问
    val excludePrivateMethods: Boolean = false
)

/**
 * 编译期分析配置
 */
data class CompileTimeAnalysisConfig(
    val repositoryConfig: RepositoryIdentificationConfig = RepositoryIdentificationConfig(),
    val fieldAccessConfig: FieldAccessAnalysisConfig = FieldAccessAnalysisConfig(),
    val cacheEnabled: Boolean = true,
    val debugMode: Boolean = false
)