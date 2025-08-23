package org.morecup.pragmaddd.analyzer

import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import java.io.File
import javax.inject.Inject
import kotlin.collections.filter

/**
 * DDD 分析 Action，用于在编译完成后立即执行领域模型分析
 * 
 * 类似于 AspectJ 的 AjcAction，但专门用于 DDD 分析
 */
open class DddAnalysisAction @Inject constructor(
    private val objectFactory: ObjectFactory
) : Action<Task> {

    /**
     * 是否启用 DDD 分析
     */
    @get:Input
    @get:Optional
    val enabled: Property<Boolean> = objectFactory.property(Boolean::class.java).convention(true)

    /**
     * 分析的类路径
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val classpath: ConfigurableFileCollection = objectFactory.fileCollection()

    /**
     * 额外的输入路径（编译输出目录）
     */
    @get:InputFiles  
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val additionalInputPath: ConfigurableFileCollection = objectFactory.fileCollection()

    /**
     * DDD 分析扩展配置
     */
    @get:Input
    @get:Optional
    lateinit var extension: PragmaDddAnalyzerExtension

    /**
     * SourceSet 名称
     */
    @get:Input
    var sourceSetName: String = "main"

    /**
     * 输出文件 - 固定路径和格式
     */
    fun getOutputFile(task: Task): File {
        val defaultPath = "build/generated/pragmaddd/$sourceSetName/resources/META-INF/pragma-ddd-analyzer/domain-analyzer.json"
        return task.project.file(defaultPath)
    }

    override fun execute(task: Task) {
        if (!enabled.getOrElse(true)) {
            task.logger.info("[Pragma DDD] DDD 分析已禁用，跳过分析")
            return
        }

        task.logger.info("[Pragma DDD] 开始执行 DDD 分析...")
        
        try {
            // 获取编译输出目录
            val compiledClasses = getCompiledClassDirectories(task)
            
            if (compiledClasses.isEmpty()) {
                task.logger.warn("[Pragma DDD] 未找到编译后的 class 文件，跳过分析")
                return
            }

            // 执行分析
            val analyzer = AggregateRootAnalyzer()
            val results = mutableListOf<ClassAnalysisResult>()

            compiledClasses.forEach { dir ->
                if (dir.exists() && dir.isDirectory) {
                    task.logger.debug("[Pragma DDD] 分析目录: ${dir.absolutePath}")
                    val dirResults = analyzer.analyzeDirectory(dir)
                    results.addAll(dirResults)
                    task.logger.info("[Pragma DDD] 在目录 ${dir.name} 中找到 ${dirResults.size} 个 @AggregateRoot 类")
                }
            }

            // 输出结果
            outputResults(results, task)

            task.logger.info("[Pragma DDD] DDD 分析完成，找到 ${results.size} 个 @AggregateRoot 类")

        } catch (e: Exception) {
            task.logger.error("[Pragma DDD] DDD 分析失败: ${e.message}", e)
            throw e
        }
    }

    /**
     * 获取编译后的 class 文件目录
     */
    private fun getCompiledClassDirectories(task: Task): List<File> {
        val directories = mutableListOf<File>()

        // 从编译任务获取输出目录
        when (task) {
            is AbstractCompile -> {
                directories.add(task.destinationDirectory.asFile.get())
            }
            is KotlinJvmCompile -> {
                directories.add(task.destinationDirectory.asFile.get())
            }
        }

        // 添加额外的输入路径
        directories.addAll(additionalInputPath.files)

        return directories.distinct()
    }

    /**
     * 输出分析结果 - 固定为JSON格式
     */
    private fun outputResults(results: List<ClassAnalysisResult>, task: Task) {
        val outputFile = getOutputFile(task)
        outputFile.parentFile.mkdirs()

        // 固定为JSON格式输出
        val mapper = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper().apply {
            enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT)
        }
        outputFile.writeText(mapper.writeValueAsString(results))

        task.logger.info("[Pragma DDD] 分析结果已保存到: ${outputFile.absolutePath}")
    }



    /**
     * 将此 Action 添加到任务中
     */
    fun addToTask(task: Task) {

        val outputFile = getOutputFile(task).parentFile
        outputFile.parentFile.mkdirs()

        // 声明输出文件
        task.outputs.dir(outputFile)

        // 将生成的资源目录添加到项目的资源源码集中
        try {
            val project = task.project
            val javaExtension = project.extensions.findByType(org.gradle.api.plugins.JavaPluginExtension::class.java)
            if (javaExtension != null) {
                val sourceSet = javaExtension.sourceSets.findByName(sourceSetName)
                if (sourceSet != null) {
                    val generatedResourcesDir = project.file("build/generated/pragmaddd/$sourceSetName/resources")
                    sourceSet.resources.srcDir(generatedResourcesDir)
                }
            }
        } catch (e: Exception) {
            task.logger.error("[Pragma DDD] 无法配置资源目录: ${e.message}")
        }

        task.doLast("pragma-ddd-analysis", this)

        val actions = task.actions.toMutableList()
        // 查找 ajc action（使用反射访问私有类）
        val ajcActions = findAjcActions(actions)

        ajcActions?.let { 
            // 将ajcActions移动到列表最后
            actions.removeAll(it)
            actions.addAll(it)
            task.actions = actions
        }
        
        // 将 action 作为扩展添加到任务中，便于外部访问
        task.extensions.add("pragmaDddAnalysis", this)

        // 注意：不注册输入输出，避免循环依赖
        // Gradle 会自动检测和处理 doLast 中的操作
    }

    /**
     * 使用反射方式查找 ajc action
     */
    private fun findAjcActions(actions: MutableList<Action<in Task>>): List<Action<in Task>>? {
        return try {
            // 获取 AbstractTask.TaskActionWrapper 类
            val taskActionWrapperClass = Class.forName("org.gradle.api.internal.AbstractTask\$TaskActionWrapper")
            // 获取 maybeActionName 字段
            val maybeActionNameField = taskActionWrapperClass.getDeclaredField("maybeActionName")
            maybeActionNameField.isAccessible = true

            actions.filter { action ->
                if (taskActionWrapperClass.isInstance(action)) {
                    val maybeActionName = maybeActionNameField.get(action) as String?
                    maybeActionName == "ajc"
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            // 如果反射失败，则返回 null
            null
        }
    }
}