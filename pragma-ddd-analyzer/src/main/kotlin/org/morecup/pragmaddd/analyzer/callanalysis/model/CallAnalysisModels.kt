package org.morecup.pragmaddd.analyzer.callanalysis.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 编译期调用分析结果的数据模型
 */

/**
 * 聚合根方法调用信息
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class AggregateRootMethodCall(
    @JsonProperty("aggregateRootMethod")
    val aggregateRootMethod: String,
    
    @JsonProperty("aggregateRootMethodDescriptor")
    val aggregateRootMethodDescriptor: String,
    
    @JsonProperty("requiredFields")
    val requiredFields: Set<String>
)

/**
 * 方法调用位置信息
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class MethodCallLocation(
    @JsonProperty("methodClass")
    val methodClass: String,
    
    @JsonProperty("method")
    val method: String,
    
    @JsonProperty("methodDescriptor")
    val methodDescriptor: String,
    
    @JsonProperty("repository")
    val repository: String,
    
    @JsonProperty("repositoryMethod")
    val repositoryMethod: String,
    
    @JsonProperty("repositoryMethodDescriptor")
    val repositoryMethodDescriptor: String,
    
    @JsonProperty("aggregateRoot")
    val aggregateRoot: String,
    
    @JsonProperty("calledAggregateRootMethod")
    val calledAggregateRootMethod: List<AggregateRootMethodCall>,
    
    @JsonProperty("requiredFields")
    val requiredFields: Set<String>
)

/**
 * Repository方法调用分析结果
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class RepositoryMethodAnalysis(
    @JsonProperty("calls")
    val calls: Map<String, MethodCallLocation>
)

/**
 * 聚合根调用分析结果
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class AggregateRootCallAnalysis(
    @JsonProperty("methods")
    val methods: Map<String, RepositoryMethodAnalysis>
)

/**
 * 完整的调用图分析结果
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class CallGraphAnalysisResult(
    @JsonProperty("version")
    val version: String = "1.0",
    
    @JsonProperty("timestamp")
    val timestamp: String,
    
    @JsonProperty("callGraph")
    val callGraph: Map<String, AggregateRootCallAnalysis>
)

/**
 * Repository识别配置
 */
data class RepositoryIdentificationConfig(
    val includePackages: List<String> = listOf("**"),
    val excludePackages: List<String> = emptyList(),
    val repositoryNamingRules: List<String> = listOf(
        "{AggregateRoot}Repository",
        "I{AggregateRoot}Repository", 
        "{AggregateRoot}Repo"
    )
)

/**
 * Repository识别结果
 */
data class RepositoryInfo(
    val className: String,
    val aggregateRootClass: String,
    val identificationMethod: RepositoryIdentificationMethod
)

/**
 * Repository识别方法枚举
 */
enum class RepositoryIdentificationMethod {
    DOMAIN_REPOSITORY_INTERFACE,  // 继承 DomainRepository<T>
    DOMAIN_REPOSITORY_ANNOTATION, // 使用 @DomainRepository 注解
    NAMING_CONVENTION             // 命名约定推导
}

/**
 * 方法调用上下文
 */
data class MethodCallContext(
    val className: String,
    val methodName: String,
    val methodDescriptor: String,
    val startLine: Int = -1,
    val endLine: Int = -1,
    val repositoryCall: RepositoryCallInfo? = null,
    val aggregateRootMethodCalls: List<AggregateRootMethodCall> = emptyList(),
    val requiredFields: Set<String> = emptySet()
)

/**
 * Repository调用信息
 */
data class RepositoryCallInfo(
    val repositoryClass: String,
    val repositoryMethod: String,
    val repositoryMethodDescriptor: String,
    val aggregateRootClass: String
)