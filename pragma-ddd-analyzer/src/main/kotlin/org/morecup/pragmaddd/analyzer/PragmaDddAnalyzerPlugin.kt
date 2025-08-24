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
 * - main源集输出路径：build/generated/pragmaddd/main/resources/META-INF/pragma-ddd-analyzer/domain-analyzer.json
 * - test源集输出路径：build/generated/pragmaddd/test/resources/META-INF/pragma-ddd-analyzer/domain-analyzer.json
 * - 处理 main 和 test SourceSet 的 compileJava 和 compileKotlin 任务
 */
class PragmaDddAnalyzerPlugin : Plugin<Project> {

    private lateinit var project: Project
    private lateinit var sourceSets: SourceSetContainer

    override fun apply(project: Project) {
        this.project = project

        // 配置 ProcessResources 任务
        configureProcessResourcesTasks()
        
        // 确保 Java 插件已应用
        project.plugins.apply(JavaBasePlugin::class.java)
        val javaPluginExtension = project.extensions.getByType(JavaPluginExtension::class.java)
        sourceSets = javaPluginExtension.sourceSets
        
        // 创建扩展配置
        val extension = project.extensions.create("pragmaDddAnalyzer", PragmaDddAnalyzerExtension::class.java)

        // 只配置 Java 和 Kotlin 插件的集成，不处理其他语言
        project.plugins.withType(JavaPlugin::class.java) { configurePlugin("java", extension) }
        project.plugins.withId("org.jetbrains.kotlin.jvm") { configurePlugin("kotlin", extension) }
        
        // 创建文档合并任务
        createDocumentationMergeTask(extension)
    }
    
    private fun configurePlugin(language: String, extension: PragmaDddAnalyzerExtension) {
        // 处理 main 和 test SourceSet
        listOf("main", "test").forEach { sourceSetName ->
            val sourceSet = sourceSets.findByName(sourceSetName)
            if (sourceSet != null) {
                // 获取对应语言的编译任务
                val compileTaskName = sourceSet.getCompileTaskName(language)
                project.tasks.named(compileTaskName) { compileTask ->
                    enhanceWithAnalysisAction(compileTask, extension, sourceSet.name)
                }
            }
        }
    }
    
    private fun configureProcessResourcesTasks() {
        project.tasks.withType(ProcessResources::class.java).configureEach { processResources ->
            processResources.duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            
            // 基于插件存在性来添加编译依赖
            project.plugins.withType(JavaPlugin::class.java) {
                processResources.dependsOn("compileJava")
                // 如果存在test源集，也添加testCompileJava依赖
                if (processResources.name == "processTestResources") {
                    processResources.dependsOn("compileTestJava")
                }
            }
            
            project.plugins.withId("org.jetbrains.kotlin.jvm") {
                processResources.dependsOn("compileKotlin")
                // 如果存在test源集，也添加compileTestKotlin依赖
                if (processResources.name == "processTestResources") {
                    processResources.dependsOn("compileTestKotlin")
                }
            }
        }
    }
    
    private fun enhanceWithAnalysisAction(compileTask: Task, extension: PragmaDddAnalyzerExtension, sourceSetName: String) {
        // 创建原有的 DDD 分析 Action
        val analysisAction = project.objects.newInstance(DddAnalysisAction::class.java)
        
        // 配置分析 Action
        analysisAction.extension = extension
        analysisAction.sourceSetName = sourceSetName
        
        // 创建新的 DDD 文档分析 Action
        val documentationAnalysisAction = project.objects.newInstance(DddDocumentationAnalysisAction::class.java)
        
        // 配置文档分析 Action
        documentationAnalysisAction.extension = extension
        documentationAnalysisAction.sourceSetName = sourceSetName
        
        // 获取编译输出目录
        when (compileTask) {
            is AbstractCompile -> {
                analysisAction.additionalInputPath.from(compileTask.destinationDirectory)
                documentationAnalysisAction.additionalInputPath.from(compileTask.destinationDirectory)
            }
            is KotlinJvmCompile -> {
                analysisAction.additionalInputPath.from(compileTask.destinationDirectory)
                documentationAnalysisAction.additionalInputPath.from(compileTask.destinationDirectory)
            }
        }
        
        // 将分析 Action 添加到编译任务
        analysisAction.addToTask(compileTask)
        documentationAnalysisAction.addToTask(compileTask)
        
        println("[Pragma DDD] 已为编译任务 ${compileTask.name} (${sourceSetName}源集) 配置 DDD 分析和文档分析")
    }
    
    /**
     * 创建文档合并任务
     */
    private fun createDocumentationMergeTask(extension: PragmaDddAnalyzerExtension) {
        val mergeTask = project.tasks.register("mergePragmaDddDocumentation") { task ->
            task.group = "pragma-ddd"
            task.description = "合并main和test源集的DDD文档分析结果"
            
            task.doLast {
                val merger = DocumentationResultMerger()
                
                val mainResultFile = project.file("build/generated/pragmaddd/main/resources/META-INF/pragma-ddd-analyzer/domain-documentation.json")
                val testResultFile = project.file("build/generated/pragmaddd/test/resources/META-INF/pragma-ddd-analyzer/domain-documentation.json")
                val mergedOutputFile = project.file("build/generated/pragmaddd/resources/META-INF/pragma-ddd-analyzer/domain-documentation-merged.json")
                
                if (merger.shouldMerge(mainResultFile, testResultFile)) {
                    merger.mergeResults(mainResultFile, testResultFile, mergedOutputFile)
                    task.logger.info("[Pragma DDD] 已合并main和test源集的文档分析结果到: ${mergedOutputFile.absolutePath}")
                } else if (mainResultFile.exists()) {
                    // 只有main源集结果，直接复制
                    mainResultFile.copyTo(mergedOutputFile, overwrite = true)
                    task.logger.info("[Pragma DDD] 已复制main源集的文档分析结果到: ${mergedOutputFile.absolutePath}")
                } else if (testResultFile.exists()) {
                    // 只有test源集结果，直接复制
                    testResultFile.copyTo(mergedOutputFile, overwrite = true)
                    task.logger.info("[Pragma DDD] 已复制test源集的文档分析结果到: ${mergedOutputFile.absolutePath}")
                } else {
                    task.logger.warn("[Pragma DDD] 未找到任何文档分析结果文件")
                }
            }
        }
        
        // 让合并任务依赖于编译任务
        project.afterEvaluate {
            val compileJavaTask = project.tasks.findByName("compileJava")
            val compileTestJavaTask = project.tasks.findByName("compileTestJava")
            val compileKotlinTask = project.tasks.findByName("compileKotlin")
            val compileTestKotlinTask = project.tasks.findByName("compileTestKotlin")
            
            listOfNotNull(compileJavaTask, compileTestJavaTask, compileKotlinTask, compileTestKotlinTask)
                .forEach { compileTask ->
                    mergeTask.configure { it.mustRunAfter(compileTask) }
                }
        }
    }

}