package org.morecup.pragmaddd.analyzer.kotlin

import org.morecup.pragmaddd.analyzer.model.*

/**
 * Kotlin元数据注解提取器
 * 简化版实现，专注于核心功能
 */
class KotlinMetadataAnnotationExtractor {

    /**
     * 分析Kotlin字段的可空性
     * 基于字段描述符和注解进行推断
     */
    fun analyzeKotlinFieldNullability(
        fieldName: String,
        descriptor: String,
        annotations: List<AnnotationInfo>
    ): Boolean? {
        // 检查是否有明确的可空性注解
        val hasNotNull = annotations.any { annotation ->
            annotation.name.endsWith("NotNull") ||
            annotation.name.endsWith("NonNull") ||
            annotation.name.endsWith("Nonnull")
        }

        val hasNullable = annotations.any { annotation ->
            annotation.name.endsWith("Nullable") ||
            annotation.name.endsWith("CheckForNull")
        }

        return when {
            hasNotNull -> false  // 明确标记为非空
            hasNullable -> true  // 明确标记为可空
            // 对于Kotlin类，基于类型描述符进行推断
            descriptor.startsWith("Ljava/lang/") -> null  // 基本包装类型，可空性未知
            else -> null  // 其他情况，可空性未知
        }
    }
}

