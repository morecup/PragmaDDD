package org.morecup.pragmaddd.analyzer

import org.gradle.api.provider.ListProperty
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
 *
 *     // 编译期调用关系分析配置
 *     compileTimeAnalysis {
 *         enabled.set(true)                                // 启用编译期调用关系分析
 *         includePackages.set(listOf("com.example.**"))   // 包含的包名模式
 *         excludePackages.set(listOf("com.example.test.**")) // 排除的包名模式
 *         repositoryNamingRules.set(listOf(               // Repository命名规则
 *             "{AggregateRoot}Repository",
 *             "I{AggregateRoot}Repository",
 *             "{AggregateRoot}Repo"
 *         ))
 *         debugMode.set(false)                            // 调试模式
 *     }
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
     * 编译期调用关系分析配置
     */
    abstract val compileTimeAnalysis: Property<CompileTimeAnalysisExtension>

    init {
        // 设置默认值
        verbose.convention(false)
        classPaths.convention(emptySet())
    }
}

/**
 * 编译期调用关系分析配置扩展
 */
abstract class CompileTimeAnalysisExtension {

    /**
     * 是否启用编译期调用关系分析
     * 默认值：true
     */
    abstract val enabled: Property<Boolean>

    /**
     * 包含的包名模式列表
     * 支持通配符，如 "com.example.**"
     * 默认值：空列表（包含所有包）
     */
    abstract val includePackages: ListProperty<String>

    /**
     * 排除的包名模式列表
     * 支持通配符，如 "com.example.test.**"
     * 默认值：常见的测试和框架包
     */
    abstract val excludePackages: ListProperty<String>

    /**
     * Repository命名规则列表
     * 使用 {AggregateRoot} 作为聚合根类名的占位符
     * 默认值：常见的Repository命名模式
     */
    abstract val repositoryNamingRules: ListProperty<String>

    /**
     * 是否启用调试模式
     * 启用后会输出详细的分析过程信息
     * 默认值：false
     */
    abstract val debugMode: Property<Boolean>

    init {
        enabled.convention(true)
        includePackages.convention(emptyList<String>())
        excludePackages.convention(listOf<String>(
            "java.**",
            "javax.**",
            "kotlin.**",
            "org.springframework.**",
            "org.junit.**",
            "org.mockito.**",
            "net.sf.cglib.**",
            "org.aspectj.**"
        ))
        repositoryNamingRules.convention(listOf<String>(
            "{AggregateRoot}Repository",
            "I{AggregateRoot}Repository",
            "{AggregateRoot}Repo"
        ))
        debugMode.convention(false)
    }
}