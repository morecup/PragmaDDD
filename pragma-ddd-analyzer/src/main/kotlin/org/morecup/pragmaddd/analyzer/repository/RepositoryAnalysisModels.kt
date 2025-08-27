package org.morecup.pragmaddd.analyzer.repository

/**
 * 聚合根信息
 */
data class AggregateRootInfo(
    val className: String,
    val fieldNames: List<String> = emptyList()
)

/**
 * Repository信息
 */
data class RepositoryInfo(
    val className: String,
    val repositoryType: RepositoryType,
    val genericAggregateRootType: String? = null,
    val annotationTargetType: String? = null,
    var targetAggregateRoot: String? = null
)

/**
 * Repository类型枚举
 */
enum class RepositoryType {
    GENERIC_INTERFACE,    // 继承DomainRepository<T>
    ANNOTATED,           // 使用@DomainRepository注解
    NAMING_CONVENTION    // 通过命名约定识别
}

/**
 * 方法调用信息
 */
data class MethodCallInfo(
    val repositoryClass: String,
    val methodName: String,
    val methodDescriptor: String
)

/**
 * Repository调用信息
 */
data class RepositoryCallInfo(
    val repository: String,
    val repositoryMethod: String, 
    val repositoryMethodDescriptor: String,
    val aggregateRoot: String?,
    val calledAggregateRootMethod: List<AggregateRootMethodCall>,
    val requiredFields: List<String>
)

/**
 * 聚合根方法调用信息
 */
data class AggregateRootMethodCall(
    val aggregateRootMethod: String,
    val aggregateRootMethodDescriptor: String,
    val requiredFields: List<String>
)

/**
 * 调用分析信息
 */
data class CallAnalysisInfo(
    val methodClass: String,
    val method: String,
    val methodDescriptor: String,
    val repositoryCalls: List<RepositoryCallInfo>
)

/**
 * Repository分析结果
 */
data class RepositoryAnalysisResult(
    val aggregateRoots: List<AggregateRootInfo>,
    val repositories: List<RepositoryInfo>,
    val callAnalysis: List<CallAnalysisInfo>
)