package org.morecup.pragmaddd.analyzer

/**
 * 方法调用信息
 */
data class MethodCallInfo(
    val className: String,
    val methodName: String,
    val methodDescriptor: String,
    val callCount: Int = 1
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
    val calledMethods: Set<MethodCallInfo>
)

/**
 * 类分析结果
 */
data class ClassAnalysisResult(
    val className: String,
    val isAggregateRoot: Boolean,
    val methods: List<PropertyAccessInfo>
)