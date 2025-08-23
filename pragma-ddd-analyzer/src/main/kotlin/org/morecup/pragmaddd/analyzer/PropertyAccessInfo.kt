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
 * 属性访问信息
 */
data class PropertyAccessInfo(
    val className: String,
    val methodName: String,
    val methodDescriptor: String,
    val accessedProperties: Set<String>,
    val modifiedProperties: Set<String>,
    val calledMethods: Set<MethodCallInfo>,
    val lambdaExpressions: Set<LambdaInfo> = emptySet() // 方法中定义的Lambda表达式
)

/**
 * 类分析结果
 */
data class ClassAnalysisResult(
    val className: String,
    val isAggregateRoot: Boolean,
    val methods: List<PropertyAccessInfo>
)