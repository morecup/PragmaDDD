package org.morecup.pragmaddd.analyzer.model

import com.fasterxml.jackson.annotation.JsonInclude

/**
 * 类元数据
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ClassMetadata(
    val className: String,
    val packageName: String,
    val annotationType: DddAnnotationType,
    val properties: List<PropertyMetadata>,
    val methods: List<MethodMetadata>,
    val documentation: DocumentationMetadata?,
    val annotations: List<AnnotationMetadata>
)

/**
 * 属性元数据
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class PropertyMetadata(
    val name: String,
    val type: String,
    val isPrivate: Boolean,
    val isMutable: Boolean,
    val documentation: DocumentationMetadata?,
    val annotations: List<AnnotationMetadata>
)

/**
 * 方法元数据
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class MethodMetadata(
    val name: String,
    val parameters: List<ParameterMetadata>,
    val returnType: String,
    val isPrivate: Boolean,
    val methodCalls: List<MethodCallMetadata>,
    val propertyAccesses: List<PropertyAccessMetadata>,
    val documentation: DocumentationMetadata?,
    val annotations: List<AnnotationMetadata>
)

/**
 * 参数元数据
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ParameterMetadata(
    val name: String,
    val type: String,
    val annotations: List<AnnotationMetadata> = emptyList()
)

/**
 * 方法调用元数据
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class MethodCallMetadata(
    val targetMethod: String,
    val receiverType: String?,
    val parameters: List<String>
)

/**
 * 属性访问元数据
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class PropertyAccessMetadata(
    val propertyName: String,
    val accessType: PropertyAccessType,
    val ownerClass: String?
)

/**
 * 文档元数据
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class DocumentationMetadata(
    val summary: String?,
    val description: String?,
    val parameters: Map<String, String> = emptyMap(),
    val returnDescription: String?
)

/**
 * 注解元数据
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class AnnotationMetadata(
    val name: String,
    val parameters: Map<String, Any> = emptyMap()
)

/**
 * 输出根对象
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class AnalysisResult(
    val generatedAt: String,
    val sourceType: String,
    val classes: List<ClassMetadata>
)

/**
 * DDD 注解类型枚举
 */
enum class DddAnnotationType {
    AGGREGATE_ROOT,
    DOMAIN_ENTITY,
    VALUE_OBJ
}

/**
 * 属性访问类型枚举
 */
enum class PropertyAccessType {
    READ,  // 属性读取操作
    WRITE  // 属性写入操作
}