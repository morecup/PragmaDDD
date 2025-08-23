package org.morecup.pragmaddd.analyzer

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.internal.provider.ValueSupplier.ValueProducer.task
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

/**
 * Pragma DDD 分析器插件
 *
 * 使用方式：
 * ```kotlin
 * plugins {
 *     id("org.morecup.pragma-ddd-analyzer")
 * }
 *
 * // 可选配置
 * pragmaDddAnalyzer {
 *     verbose.set(true)
 * }
 * ```
 *
 * 注意：
 * - 分析会在编译时自动执行，无需手动运行任务
 * - 输出文件固定为 JSON 格式
 * - 输出路径固定为 build/generated/pragmaddd/main/resources/META-INF/pragma-ddd-analyzer/domain-analyzer.json
 * - 只处理 main SourceSet 的 compileJava 和 compileKotlin 任务
 */
class PragmaDddAnalyzerPlugin : Plugin<Project> {

    private lateinit var project: Project
    private lateinit var sourceSets: SourceSetContainer

    companion object {
        // 获取插件版本号
        private fun getPluginVersion(): String {
            return try {
                // 方法1: 尝试从 MANIFEST.MF 中读取版本
                val clazz = PragmaDddAnalyzerPlugin::class.java
                val packageInfo = clazz.`package`
                val manifestVersion = packageInfo?.implementationVersion

                if (!manifestVersion.isNullOrBlank()) {
                    return manifestVersion
                }

                // 方法2: 尝试从资源文件中读取版本
                val versionResource = clazz.getResourceAsStream("/pragma-ddd-analyzer-version.properties")
                if (versionResource != null) {
                    val props = java.util.Properties()
                    props.load(versionResource)
                    val resourceVersion = props.getProperty("version")
                    if (!resourceVersion.isNullOrBlank()) {
                        return resourceVersion
                    }
                }

                // 方法3: 使用默认版本
                "1.0.0"
            } catch (e: Exception) {
                // 如果无法获取版本，使用默认版本
                "1.0.0"
            }
        }
    }

    override fun apply(project: Project) {
        this.project = project

        project.tasks.withType(ProcessResources::class.java).configureEach { processResources ->
            processResources.duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            processResources.dependsOn("compileKotlin", "compileJava")
        }
        
        // 确保 Java 插件已应用
        project.plugins.apply(JavaBasePlugin::class.java)
        val javaPluginExtension = project.extensions.getByType(JavaPluginExtension::class.java)
        sourceSets = javaPluginExtension.sourceSets
        
        // 创建扩展配置
        val extension = project.extensions.create("pragmaDddAnalyzer", PragmaDddAnalyzerExtension::class.java)

        // 只为 main SourceSet 配置分析任务，不处理 test 相关任务
        val mainSourceSet = sourceSets.findByName("main")
        if (mainSourceSet != null) {
            configureSourceSetDefaults(mainSourceSet, extension)
        }

        // 只配置 Java 和 Kotlin 插件的集成，不处理其他语言
        project.plugins.withType(JavaPlugin::class.java) { configurePlugin("java", extension) }
        project.plugins.withId("org.jetbrains.kotlin.jvm") { configurePlugin("kotlin", extension) }
    }

    private fun configureSourceSetDefaults(sourceSet: SourceSet, extension: PragmaDddAnalyzerExtension) {
        // 不再需要创建分析任务，直接通过 DddAnalysisAction 处理
    }
    
    private fun configurePlugin(language: String, extension: PragmaDddAnalyzerExtension) {
        // 只处理 main SourceSet
        val mainSourceSet = sourceSets.findByName("main")
        if (mainSourceSet != null) {
            // 获取对应语言的编译任务
            val compileTaskName = mainSourceSet.getCompileTaskName(language)
            project.tasks.named(compileTaskName) { compileTask ->
                enhanceWithAnalysisAction(compileTask, extension, mainSourceSet.name)
            }
        }
    }
    
    private fun enhanceWithAnalysisAction(compileTask: Task, extension: PragmaDddAnalyzerExtension, sourceSetName: String) {
        // 创建 DDD 分析 Action
        val analysisAction = project.objects.newInstance(DddAnalysisAction::class.java)
        
        // 配置分析 Action
        analysisAction.extension = extension
        analysisAction.sourceSetName = sourceSetName
        
        // 获取编译输出目录
        when (compileTask) {
            is AbstractCompile -> {
                analysisAction.additionalInputPath.from(compileTask.destinationDirectory)
            }
            is KotlinJvmCompile -> {
                analysisAction.additionalInputPath.from(compileTask.destinationDirectory)
            }
        }
        
        // 将分析 Action 添加到编译任务
        analysisAction.addToTask(compileTask)
        
        println("[Pragma DDD] 已为编译任务 ${compileTask.name} 配置 DDD 分析")
    }



}