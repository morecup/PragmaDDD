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

/**
 * DDD 文档分析 Action，用于在编译完成后收集DDD类的文档信息
 */
open class DddDocumentationAnalysisAction @Inject constructor(
    private val objectFactory: ObjectFactory
) : Action<Task> {

    /**
     * 是否启用 DDD 文档分析
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
     * 输出文件 - 文档信息JSON
     */
    fun getDocumentationOutputFile(task: Task): File {
        val defaultPath = "build/generated/pragmaddd/$sourceSetName/resources/META-INF/pragma-ddd-analyzer/domain-documentation.json"
        return task.project.file(defaultPath)
    }

    override fun execute(task: Task) {
        if (!enabled.getOrElse(true)) {
            task.logger.info("[Pragma DDD] DDD 文档分析已禁用，跳过分析")
            return
        }

        task.logger.info("[Pragma DDD] 开始执行 DDD 文档分析...")
        
        try {
            // 获取编译输出目录和源代码目录
            val compiledClasses = getCompiledClassDirectories(task)
            val sourceDirectories = getSourceDirectories(task)
            
            if (compiledClasses.isEmpty()) {
                task.logger.warn("[Pragma DDD] 未找到编译后的 class 文件，跳过文档分析")
                return
            }

            // 执行文档分析
            val analyzer = DomainDocumentationAnalyzer()
            val results = mutableListOf<ClassDocumentationInfo>()

            compiledClasses.forEach { classDir ->
                if (classDir.exists() && classDir.isDirectory) {
                    // 为每个编译目录找到对应的源代码目录
                    val sourceDir = findCorrespondingSourceDirectory(classDir, sourceDirectories)
                    if (sourceDir != null) {
                        task.logger.debug("[Pragma DDD] 分析编译目录: ${classDir.absolutePath}, 源代码目录: ${sourceDir.absolutePath}")
                        val dirResults = analyzer.analyzeDirectory(classDir, sourceDir, sourceSetName)
                        results.addAll(dirResults)
                        task.logger.info("[Pragma DDD] 在目录 ${classDir.name} 中找到 ${dirResults.size} 个 DDD 类的文档信息")
                    } else {
                        task.logger.warn("[Pragma DDD] 未找到对应的源代码目录: ${classDir.absolutePath}")
                    }
                }
            }

            // 输出结果
            outputDocumentationResults(results, task)

            // 统计各种 DDD 类型的数量
            val aggregateRoots = results.count { it.domainObjectType == DomainObjectType.AGGREGATE_ROOT }
            val domainEntities = results.count { it.domainObjectType == DomainObjectType.DOMAIN_ENTITY }
            val valueObjects = results.count { it.domainObjectType == DomainObjectType.VALUE_OBJECT }
            
            task.logger.info("[Pragma DDD] DDD 文档分析完成，收集了 ${results.size} 个 DDD 类的文档信息：" +
                " AggregateRoot($aggregateRoots), DomainEntity($domainEntities), ValueObject($valueObjects)")

        } catch (e: Exception) {
            task.logger.error("[Pragma DDD] DDD 文档分析失败: ${e.message}", e)
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
     * 获取源代码目录
     */
    private fun getSourceDirectories(task: Task): List<File> {
        val project = task.project
        val directories = mutableListOf<File>()

        try {
            val javaExtension = project.extensions.findByType(org.gradle.api.plugins.JavaPluginExtension::class.java)
            if (javaExtension != null) {
                val sourceSet = javaExtension.sourceSets.findByName(sourceSetName)
                if (sourceSet != null) {
                    // 添加 Java 源代码目录
                    directories.addAll(sourceSet.java.srcDirs)
                    
                    // 尝试添加 Kotlin 源代码目录
                    try {
                        val kotlinSourceSet = sourceSet.extensions.findByName("kotlin")
                        if (kotlinSourceSet != null) {
                            val kotlinSrcDirs = kotlinSourceSet.javaClass.getMethod("getSrcDirs").invoke(kotlinSourceSet)
                            if (kotlinSrcDirs is Set<*>) {
                                directories.addAll(kotlinSrcDirs.filterIsInstance<File>())
                            }
                        }
                    } catch (e: Exception) {
                        // Kotlin 插件可能未应用，忽略错误
                    }
                }
            }
        } catch (e: Exception) {
            task.logger.warn("[Pragma DDD] 获取源代码目录失败: ${e.message}")
        }

        return directories.distinct().filter { it.exists() }
    }

    /**
     * 为编译目录找到对应的源代码目录
     */
    private fun findCorrespondingSourceDirectory(classDir: File, sourceDirectories: List<File>): File? {
        // 简单策略：返回第一个存在的源代码目录
        // 在更复杂的项目中，可能需要更智能的匹配逻辑
        return sourceDirectories.firstOrNull { it.exists() }
    }

    /**
     * 输出文档分析结果
     */
    private fun outputDocumentationResults(results: List<ClassDocumentationInfo>, task: Task) {
        val outputFile = getDocumentationOutputFile(task)
        outputFile.parentFile.mkdirs()

        // 按源集组织结果
        val sourceSetResult = SourceSetDocumentationResult(
            sourceSet = sourceSetName,
            classes = results
        )

        // 创建完整的分析结果
        val documentationResult = if (sourceSetName == "main") {
            DocumentationAnalysisResult(main = sourceSetResult)
        } else {
            DocumentationAnalysisResult(test = sourceSetResult)
        }

        // 输出为JSON格式
        val mapper = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper().apply {
            enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT)
        }
        outputFile.writeText(mapper.writeValueAsString(documentationResult))

        task.logger.info("[Pragma DDD] 文档分析结果已保存到: ${outputFile.absolutePath}")
    }

    /**
     * 将此 Action 添加到任务中
     */
    fun addToTask(task: Task) {
        val outputFile = getDocumentationOutputFile(task).parentFile
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

        task.doLast("pragma-ddd-documentation-analysis") {
            execute(task)
        }
        
        // 将 action 作为扩展添加到任务中，便于外部访问
        task.extensions.add("pragmaDddDocumentationAnalysis", this)
    }
}