package org.morecup.pragmaddd.analyzer

/**
 * 注解信息
 */
data class AnnotationInfo(
    val name: String,                           // 注解名称，如 "AggregateRoot"
    val descriptor: String,                     // 注解描述符，如 "Lorg/morecup/pragmaddd/core/annotation/AggregateRoot;"
    val parameters: Map<String, Any> = emptyMap() // 注解参数
)

/**
 * 属性信息
 */
data class PropertyInfo(
    val name: String,                           // 属性名
    val type: String,                           // 属性类型
    val descriptor: String,                     // 属性描述符
    val access: Int,                            // 访问修饰符
    val signature: String? = null,              // 泛型签名
    val value: Any? = null,                     // 默认值
    val documentation: String? = null,          // 文档注释
    val annotations: List<AnnotationInfo> = emptyList() // 属性上的注解
)

/**
 * 方法信息
 */
data class MethodInfo(
    val name: String,                           // 方法名
    val descriptor: String,                     // 方法描述符
    val access: Int,                            // 访问修饰符
    val signature: String? = null,              // 泛型签名
    val exceptions: List<String> = emptyList(), // 异常类型
    val documentation: String? = null,          // 文档注释
    val annotations: List<AnnotationInfo> = emptyList() // 方法上的注解
)

/**
 * 类文档信息
 */
data class ClassDocumentationInfo(
    val className: String,                      // 类名
    val packageName: String,                    // 包名
    val access: Int,                            // 访问修饰符
    val signature: String? = null,              // 泛型签名
    val superName: String? = null,              // 父类名
    val interfaces: List<String> = emptyList(), // 实现的接口
    val documentation: String? = null,          // 类文档注释
    val annotations: List<AnnotationInfo> = emptyList(), // 类上的注解
    val properties: List<PropertyInfo> = emptyList(),    // 属性信息
    val methods: List<MethodInfo> = emptyList(),         // 方法信息
    val domainObjectType: DomainObjectType,     // 领域对象类型
    val sourceSet: String                       // 源集名称 (main/test)
)

/**
 * 源集文档分析结果
 */
data class SourceSetDocumentationResult(
    val sourceSet: String,                      // 源集名称
    val classes: List<ClassDocumentationInfo>   // 类信息列表
)

/**
 * 完整的文档分析结果
 */
data class DocumentationAnalysisResult(
    val main: SourceSetDocumentationResult? = null,    // main源集结果
    val test: SourceSetDocumentationResult? = null     // test源集结果
)