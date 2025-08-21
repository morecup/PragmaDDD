package org.morecup.pragmaddd.analyzer

import org.gradle.api.Plugin
import org.gradle.api.Project

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
 *     outputFormat.set("JSON") // JSON 或 TXT
 *     outputFile.set("build/reports/pragma-ddd-analysis.json")
 *     aspectJMode.set(AspectJMode.ENABLED) // AspectJ 织入模式
 *     showWeaveInfo.set(false) // 是否显示 AspectJ 织入详细信息（默认false）
 * }
 * ```
 */
class PragmaDddAnalyzerPlugin : Plugin<Project> {

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
        // 创建扩展配置
        val extension = project.extensions.create("pragmaDddAnalyzer", PragmaDddAnalyzerExtension::class.java)

        // 注册分析任务
        val analyzeTask = project.tasks.register("analyzeDddClasses", AnalyzeDddClassesTask::class.java) { task ->
            task.group = "pragma-ddd"
            task.description = "分析 @AggregateRoot 注解的类的属性访问情况"
            task.extension = extension
        }



        // 创建自定义 AspectJ 任务配置器
        val customAspectJConfigurator = CustomAspectJTaskConfigurator()

        // 自动集成到构建流程中
        project.afterEvaluate {
            configureTaskDependencies(project, analyzeTask.get())

            // 根据配置选择是否启用 AspectJ
            when (extension.aspectJMode.getOrElse(AspectJMode.ENABLED)) {
                AspectJMode.ENABLED -> {
                    // 配置AspectJ依赖
                    configureAspectJDependencies(project)
                    // 使用自定义 AspectJ 任务，完全控制执行顺序
                    customAspectJConfigurator.configureCustomAspectJTask(project, analyzeTask.get(), extension)
                }
                AspectJMode.DISABLED -> {
                    // 不配置 AspectJ，仅运行分析任务
                    project.logger.info("AspectJ 织入功能已禁用")
                }
            }
        }
    }

    private fun configureTaskDependencies(project: Project, analyzeTask: AnalyzeDddClassesTask) {
        // 查找编译任务
        val compileKotlinTask = project.tasks.findByName("compileKotlin")
        val compileJavaTask = project.tasks.findByName("compileJava")
        val classesTask = project.tasks.findByName("classes")
        val processResourcesTask = project.tasks.findByName("processResources")

        // 关键修改：分析任务依赖于编译任务，但必须在 AspectJ 织入之前执行
        compileKotlinTask?.let { analyzeTask.dependsOn(it) }
        compileJavaTask?.let { analyzeTask.dependsOn(it) }

        // 让 processResources 任务依赖分析任务，确保 JSON 文件在资源处理前生成
        processResourcesTask?.dependsOn(analyzeTask)

        // 让 jar 任务也依赖分析任务，确保打包前完成分析
        project.tasks.findByName("jar")?.dependsOn(analyzeTask)

        // 如果有测试任务，也让它依赖分析任务
        project.tasks.findByName("test")?.dependsOn(analyzeTask)
    }

    private fun configureAspectJDependencies(project: Project) {
        try {
            // 1. 添加 AspectJ 工具依赖（用于编译）
            project.dependencies.add("implementation", "org.aspectj:aspectjtools:1.9.7")
            
            // 2. 添加 AspectJ 运行时依赖
            project.dependencies.add("implementation", "org.aspectj:aspectjrt:1.9.7")

            // 3. 添加 pragma-ddd-core 依赖（普通依赖）
            try {
                project.dependencies.add("implementation", project.project(":pragma-ddd-core"))
                project.logger.info("已自动配置 implementation 依赖: pragma-ddd-core")
            } catch (e: Exception) {
                val version = getPluginVersion()
                project.logger.info("本地 pragma-ddd-core 模块不存在，将使用外部依赖版本: $version")
                project.dependencies.add("implementation", "org.morecup.pragmaddd:pragma-ddd-core:$version")
            }

            // 4. 关键：添加 aspect 依赖（AspectJ 特有的配置）
            // 首先确保 aspect 配置存在
            val aspectConfiguration = project.configurations.findByName("aspect") 
                ?: project.configurations.create("aspect").apply {
                    // 配置aspect配置不传递依赖，避免包含不必要的传递依赖
                    isTransitive = false
                }
            
            try {
                project.dependencies.add("aspect", project.project(":pragma-ddd-aspect"))
                project.logger.info("已自动配置 aspect 依赖: pragma-ddd-aspect")
            } catch (e: Exception) {
                val version = getPluginVersion()
                project.logger.info("本地 pragma-ddd-aspect 模块不存在，将使用外部依赖版本: $version")
                project.dependencies.add("aspect", "org.morecup.pragmaddd:pragma-ddd-aspect:$version")
            }

            project.logger.info("已自动配置 AspectJ 依赖")

        } catch (e: Exception) {
            project.logger.error("配置 AspectJ 依赖时出错: ${e.message}", e)
            throw e
        }
    }

}