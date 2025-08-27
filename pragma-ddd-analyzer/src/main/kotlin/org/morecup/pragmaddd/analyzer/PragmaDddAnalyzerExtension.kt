package org.morecup.pragmaddd.analyzer

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.provider.ListProperty



/**
 * Pragma DDD 分析器插件配置扩展
 *
 * 配置示例：
 * ```kotlin
 * pragmaDddAnalyzer {
 *     verbose.set(false)                                   // 不输出详细日志（推荐）
 *     classPaths.set(setOf("build/classes/kotlin/main"))   // 自定义类路径（通常不需要）
 *     
 *     // 编译期调用分析配置
 *     compileTimeAnalysis {
 *         enabled.set(true)                               // 启用编译期调用分析
 *         includePackages.set(listOf("com.example.**"))   // 包含的包名模式
 *         excludePackages.set(listOf("com.example.test.**")) // 排除的包名模式
 *         repositoryNamingRules.set(listOf(
 *             "{AggregateRoot}Repository",
 *             "I{AggregateRoot}Repository"
 *         ))
 *         cacheEnabled.set(true)                          // 启用增量分析缓存
 *         debugMode.set(false)                            // 调试模式
 *     }
 * }
 * ```
 *
 * 注意：
 * - domain-analyzer.json 输出路径：build/generated/pragmaddd/main/resources/META-INF/pragma-ddd-analyzer/domain-analyzer.json
 * - call-analysis.json 输出路径：build/generated/pragmaddd/main/resources/META-INF/pragma-ddd-analyzer/call-analysis.json
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
     * 编译期调用分析配置
     * 暂时注释，解决编译问题
     */
    // abstract val compileTimeAnalysis: CompileTimeAnalysisExtension

    init {
        // 设置默认值
        verbose.convention(false)
        classPaths.convention(emptySet())
    }
}

/**
 * 编译期调用分析配置扩展
 * 暂时注释，解决编译问题
 */
/*
abstract class CompileTimeAnalysisExtension {
    
    /**
     * 是否启用编译期调用分析
     * 默认值：true
     */
    abstract val enabled: Property<Boolean>
    
    /**
     * 包含的包名模式
     * 支持通配符：* 和 **
     * 默认值：["**"] (包含所有包)
     */
    abstract val includePackages: ListProperty<String>
    
    /**
     * 排除的包名模式
     * 支持通配符：* 和 **
     * 默认值：["**.test.**", "**.tests.**"]
     */
    abstract val excludePackages: ListProperty<String>
    
    /**
     * Repository命名规则
     * 支持占位符：{AggregateRoot}
     * 默认值：["{AggregateRoot}Repository", "I{AggregateRoot}Repository", "{AggregateRoot}Repo"]
     */
    abstract val repositoryNamingRules: ListProperty<String>
    
    /**
     * 是否启用增量分析缓存
     * 默认值：true
     */
    abstract val cacheEnabled: Property<Boolean>
    
    /**
     * 是否启用调试模式
     * 启用后会输出详细的分析过程信息
     * 默认值：false
     */
    abstract val debugMode: Property<Boolean>
    
    /**
     * 最大递归深度
     * 用于字段访问递归分析
     * 默认值：10
     */
    abstract val maxRecursionDepth: Property<Int>
    
    /**
     * 是否启用循环依赖检测
     * 默认值：true
     */
    abstract val enableCircularDependencyDetection: Property<Boolean>
    
    /**
     * 是否排除setter方法的字段访问
     * 默认值：true
     */
    abstract val excludeSetterMethods: Property<Boolean>
    
    init {
        // 设置默认值
        enabled.convention(true)
        includePackages.convention(listOf("**"))
        excludePackages.convention(listOf("**.test.**", "**.tests.**"))
        repositoryNamingRules.convention(listOf(
            "{AggregateRoot}Repository",
            "I{AggregateRoot}Repository", 
            "{AggregateRoot}Repo"
        ))
        cacheEnabled.convention(true)
        debugMode.convention(false)
        maxRecursionDepth.convention(10)
        enableCircularDependencyDetection.convention(true)
        excludeSetterMethods.convention(true)
    }
}
*/