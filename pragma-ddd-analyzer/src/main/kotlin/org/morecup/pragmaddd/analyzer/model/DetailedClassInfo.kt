package org.morecup.pragmaddd.analyzer.model

import com.fasterxml.jackson.annotation.JsonInclude

/**
 * 注解详细信息
 */
data class AnnotationInfo(
    val name: String,                           // 注解名称，如 "AggregateRoot"
    val descriptor: String,                     // 注解描述符，如 "Lorg/morecup/pragmaddd/core/annotation/AggregateRoot;"
    val visible: Boolean = true,                // 是否在运行时可见
    val parameters: Map<String, Any?> = emptyMap() // 注解参数（支持 null 值）
)

/**
 * 修饰符信息
 */
data class ModifierInfo(
    val access: Int,                           // ASM访问标志
    val isPublic: Boolean = false,
    val isPrivate: Boolean = false,
    val isProtected: Boolean = false,
    val isStatic: Boolean = false,
    val isFinal: Boolean = false,
    val isAbstract: Boolean = false,
    val isSynthetic: Boolean = false,
    val isInterface: Boolean = false,
    val isEnum: Boolean = false,
    val isAnnotation: Boolean = false
)

/**
 * 字段详细信息
 */
data class DetailedFieldInfo(
    val name: String,                          // 字段名
    val descriptor: String,                    // 字段类型描述符
    val signature: String? = null,             // 泛型签名
    val value: Any? = null,                    // 默认值
    val modifiers: ModifierInfo,               // 修饰符信息
    val annotations: List<AnnotationInfo> = emptyList(), // 字段上的注解
    val documentation: String? = null,         // 文档注释（Javadoc/KDoc）
    val isNullable: Boolean? = null            // 可空性信息：true=可空，false=非空，null=未知
)

/**
 * 方法参数信息
 */
data class ParameterInfo(
    val name: String? = null,                  // 参数名（如果可用）
    val descriptor: String,                    // 参数类型描述符
    val annotations: List<AnnotationInfo> = emptyList() // 参数注解
)

/**
 * 方法详细信息（增强版，包含原有的属性访问分析）
 */
data class DetailedMethodInfo(
    val name: String,                          // 方法名
    val descriptor: String,                    // 方法描述符
    val signature: String? = null,             // 泛型签名
    val modifiers: ModifierInfo,               // 修饰符信息
    val parameters: List<ParameterInfo> = emptyList(), // 参数信息
    val exceptions: List<String> = emptyList(), // 异常类型
    val annotations: List<AnnotationInfo> = emptyList(), // 方法注解
    val documentation: String? = null,         // 文档注释（Javadoc/KDoc）
    val returnType: String? = null,            // 返回类型（从descriptor解析）

    // 原有的属性访问分析功能
    val accessedProperties: Set<String> = emptySet(),           // 访问的属性
    val modifiedProperties: Set<String> = emptySet(),           // 修改的属性
    val calledMethods: Set<MethodCallInfo> = emptySet(),        // 调用的方法
    val lambdaExpressions: Set<LambdaInfo> = emptySet(),        // Lambda表达式
    val externalPropertyAccesses: Set<ExternalPropertyAccessInfo> = emptySet() // 外部属性访问
)

/**
 * 类详细信息（增强版，包含原有的属性访问分析）
 */
data class DetailedClassInfo(
    val className: String,                     // 完整类名
    val simpleName: String,                    // 简单类名
    val packageName: String,                   // 包名
    val modifiers: ModifierInfo,               // 类修饰符
    val superClass: String? = null,            // 父类
    val interfaces: List<String> = emptyList(), // 实现的接口
    val signature: String? = null,             // 泛型签名
    val sourceFile: String? = null,            // 源文件名
    val annotations: List<AnnotationInfo> = emptyList(), // 类注解
    val fields: List<DetailedFieldInfo> = emptyList(),   // 字段信息
    val methods: List<DetailedMethodInfo> = emptyList(), // 方法信息
    val documentation: String? = null,         // 类文档注释（Javadoc/KDoc）
    val domainObjectType: DomainObjectType? = null,      // DDD领域对象类型
    val sourceSetName: String? = null,         // 源集名称（main/test）

    // 原有的属性访问分析功能（从PropertyAccessInfo合并过来）
    val propertyAccessAnalysis: List<PropertyAccessInfo> = emptyList() // 原有的方法级属性访问分析
)

/**
 * 领域对象类型枚举
 */
enum class DomainObjectType {
    AGGREGATE_ROOT,
    DOMAIN_ENTITY,
    VALUE_OBJECT
}

/**
 * 属性访问类型
 */
enum class PropertyAccessType {
    READ,  // 读取属性
    WRITE  // 写入属性
}

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
    val targetDomainObjectType: DomainObjectType? = null // 目标类的DDD注解类型
)

/**
 * 属性访问信息（原有的分析结果）
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
 * 分析结果汇总
 */
data class DetailedAnalysisResult(
    val sourceSetName: String,                 // 源集名称
    val analysisTimestamp: Long = System.currentTimeMillis(), // 分析时间戳
    val classes: List<DetailedClassInfo> = emptyList(),       // 分析的类信息
    val summary: AnalysisSummary                              // 分析摘要
)

/**
 * 分析摘要
 */
data class AnalysisSummary(
    val totalClasses: Int = 0,                 // 总类数
    val aggregateRootCount: Int = 0,           // 聚合根数量
    val domainEntityCount: Int = 0,            // 领域实体数量
    val valueObjectCount: Int = 0,             // 值对象数量
    val totalFields: Int = 0,                  // 总字段数
    val totalMethods: Int = 0,                 // 总方法数
    val classesWithDocumentation: Int = 0,     // 有文档注释的类数量
    val fieldsWithDocumentation: Int = 0,      // 有文档注释的字段数量
    val methodsWithDocumentation: Int = 0      // 有文档注释的方法数量
)
