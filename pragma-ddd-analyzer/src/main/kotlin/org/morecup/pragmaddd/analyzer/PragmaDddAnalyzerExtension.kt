package org.morecup.pragmaddd.analyzer

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

/**
 * AspectJ 织入模式
 */
enum class AspectJMode {
    /**
     * 使用自定义 AspectJ 任务，完全控制执行顺序（推荐）
     */
    ENABLED,
    
    /**
     * 禁用 AspectJ 织入功能
     */
    DISABLED
}

/**
 * Pragma DDD 分析器插件配置扩展
 *
 * 配置示例：
 * ```kotlin
 * pragmaDddAnalyzer {
 *     verbose.set(false)                                   // 不输出详细日志（推荐）
 *     outputFormat.set("JSON")                             // 输出格式：JSON 或 TXT
 *     outputFile.set("build/reports/ddd-analysis.json")    // 自定义输出文件
 *     classPaths.set(setOf("build/classes/kotlin/main"))   // 自定义类路径（通常不需要）
 *     aspectJMode.set(AspectJMode.ENABLED)                 // AspectJ 织入模式
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
     * 包括AspectJ编译的详细过程、文件处理信息等
     * 注意：启用此选项会产生大量日志输出
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
     * AspectJ 织入模式
     * - ENABLED: 使用自定义 AspectJ 任务，完全控制执行顺序（推荐）
     * - DISABLED: 禁用 AspectJ 织入功能
     * 默认值：ENABLED
     */
    abstract val aspectJMode: Property<AspectJMode>

    /**
     * 是否显示 AspectJ 织入详细信息
     * 启用此选项会显示哪些类被织入了哪些切面
     * 默认值：false
     */
    abstract val showWeaveInfo: Property<Boolean>

    init {
        // 设置默认值 - 将 JSON 文件生成到 resources 目录，这样会包含在 JAR 中
        outputFile.convention("build/resources/main/META-INF/pragma-ddd-analysis.json")
        verbose.convention(false)
        outputFormat.convention("JSON")
        classPaths.convention(emptySet())
        aspectJMode.convention(AspectJMode.ENABLED)
        showWeaveInfo.convention(false)
    }
}