package org.morecup.pragmaddd.analyzer

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

/**
 * Pragma DDD 分析器插件配置扩展
 * 
 * 配置示例：
 * ```kotlin
 * pragmaDddAnalyzer {
 *     verbose.set(true)                                    // 启用详细输出
 *     outputFormat.set("JSON")                             // 输出格式：JSON 或 TXT
 *     outputFile.set("build/reports/ddd-analysis.json")    // 自定义输出文件
 *     classPaths.set(setOf("build/classes/kotlin/main"))   // 自定义类路径（通常不需要）
 *     enableAspectJ.set(true)                              // 启用 AspectJ 织入功能
 * }
 * ```
 */
abstract class PragmaDddAnalyzerExtension {
    
    /**
     * 输出分析结果的文件路径
     * 默认值：build/reports/pragma-ddd-analysis.json
     */
    abstract val outputFile: Property<String>
    
    /**
     * 要分析的类路径目录
     * 默认会自动检测 Kotlin 和 Java 的编译输出目录
     */
    abstract val classPaths: SetProperty<String>
    
    /**
     * 是否输出详细日志
     * 默认值：false
     */
    abstract val verbose: Property<Boolean>
    
    /**
     * 输出格式
     * 支持的格式：JSON, TXT
     * 默认值：JSON
     */
    abstract val outputFormat: Property<String>
    
    /**
     * 是否启用 AspectJ 织入功能
     * 启用后会自动配置 AspectJ 插件和依赖
     * 默认值：true
     */
    abstract val enableAspectJ: Property<Boolean>
    
    init {
        // 设置默认值 - 将 JSON 文件生成到 resources 目录，这样会包含在 JAR 中
        outputFile.convention("build/resources/main/META-INF/pragma-ddd-analysis.json")
        verbose.convention(false)
        outputFormat.convention("JSON")
        classPaths.convention(emptySet())
        enableAspectJ.convention(true)
    }
}