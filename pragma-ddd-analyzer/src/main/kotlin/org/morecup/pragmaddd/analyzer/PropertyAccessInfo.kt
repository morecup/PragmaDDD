package org.morecup.pragmaddd.analyzer

/**
 * Lambda 信息
 */
data class LambdaInfo(
    val className: String,
    val methodName: String,
    val methodDescriptor: String,
    val lambdaType: String, // 函数式接口类型，如 "java.util.function.Consumer"
    val capturedVariables: Set<String> = emptySet() // 捕获的变量
)

/**
 * 方法调用信息
 */
data class MethodCallInfo(
    val className: String,
    val methodName: String,
    val methodDescriptor: String,
    val callCount: Int = 1,
    val associatedLambdas: Set<LambdaInfo> = emptySet() // 关联的Lambda表达式
)

/**
 * 外部属性访问信息（针对带有DDD注解的类）
 */
data class ExternalPropertyAccessInfo(
    val targetClassName: String, // 被访问的类名
    val propertyName: String,    // 属性名
    val accessType: PropertyAccessType, // 访问类型
    val hasAggregateRootAnnotation: Boolean = false,
    val hasDomainEntityAnnotation: Boolean = false,
    val hasValueObjectAnnotation: Boolean = false
)

/**
 * 属性访问类型
 */
enum class PropertyAccessType {
    READ,  // 读取属性
    WRITE  // 写入属性
}

/**
 * 属性访问信息
 */
data class PropertyAccessInfo(
    val className: String,
    val methodName: String,
    val methodDescriptor: String,
    val accessedProperties: Set<String>,
    val modifiedProperties: Set<String>,
    val calledMethods: Set<MethodCallInfo>,
    val lambdaExpressions: Set<LambdaInfo> = emptySet(), // 方法中定义的Lambda表达式
    val externalPropertyAccesses: Set<ExternalPropertyAccessInfo> = emptySet() // 对外部DDD实体的属性访问
)

/**
 * 类分析结果
 */
data class ClassAnalysisResult(
    val className: String,
    val isAggregateRoot: Boolean,
    val methods: List<PropertyAccessInfo>
)