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
 *     enableCallAnalysis.set(true)                         // 启用编译期调用分析
 *     repositoryNamingRules.set(setOf(                     // Repository命名规则
 *         "{AggregateRoot}Repository",
 *         "I{AggregateRoot}Repository"
 *     ))
 *     includePackages.set(setOf("com.example.**"))         // 包含的包名模式
 *     excludePackages.set(setOf("com.example.test.**"))    // 排除的包名模式
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

    /**
     * 是否启用编译期调用分析
     * 默认值：true
     */
    abstract val enableCallAnalysis: Property<Boolean>

    /**
     * Repository命名规则
     * 用于通过命名约定识别Repository
     */
    abstract val repositoryNamingRules: SetProperty<String>

    /**
     * 包含的包名模式
     * 用于过滤分析范围
     */
    abstract val includePackages: SetProperty<String>

    /**
     * 排除的包名模式
     * 用于过滤分析范围
     */
    abstract val excludePackages: SetProperty<String>

    init {
        // 设置默认值
        verbose.convention(false)
        classPaths.convention(emptySet())
        enableCallAnalysis.convention(true)
        repositoryNamingRules.convention(setOf(
            "{AggregateRoot}Repository",
            "I{AggregateRoot}Repository",
            "{AggregateRoot}Repo"
        ))
        includePackages.convention(setOf("**"))
        excludePackages.convention(setOf(
            "java.**",
            "kotlin.**", 
            "org.springframework.**",
            "org.junit.**"
        ))
    }
}