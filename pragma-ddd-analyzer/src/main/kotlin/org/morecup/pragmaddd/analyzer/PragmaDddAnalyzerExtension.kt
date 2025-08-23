package org.morecup.pragmaddd.analyzer

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty



/**
 * Pragma DDD 分析器插件配置扩展
 *
 * 配置示例：
 * ```kotlin
 * pragmaDddAnalyzer {
 *     verbose.set(false)                                   // 不输出详细日志（推荐）
 *     classPaths.set(setOf("build/classes/kotlin/main"))   // 自定义类路径（通常不需要）
 * }
 * ```
 *
 * 注意：输出文件固定为 JSON 格式，路径固定为 build/generated/pragmaddd/main/resources/META-INF/pragma-ddd-analyzer/domain-analyzer.json
 */
abstract class PragmaDddAnalyzerExtension {

    /**
     * 要分析的类路径目录
     * 默认会自动检测 Kotlin 和 Java 的编译输出目录
     */
    abstract val classPaths: SetProperty<String>

    /**
     * 是否输出详细日志
     * 包括文件处理信息等
     * 注意：启用此选项会产生大量日志输出
     * 默认值：false
     */
    abstract val verbose: Property<Boolean>



    init {
        // 设置默认值
        verbose.convention(false)
        classPaths.convention(emptySet())
    }
}