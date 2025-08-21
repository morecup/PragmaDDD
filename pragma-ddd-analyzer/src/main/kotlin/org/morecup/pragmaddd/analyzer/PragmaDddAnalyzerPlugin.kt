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
 *     enableAspectJ.set(true) // 启用 AspectJ 织入功能
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
        
        // 注册 AspectJ 配置任务
        val aspectJConfigTask = project.tasks.register("configureAspectJ", AspectJConfigurationTask::class.java) { task ->
            task.group = "pragma-ddd"
            task.description = "配置 AspectJ 织入功能"
            task.extension = extension
        }
        
        // 自动集成到构建流程中
        project.afterEvaluate {
            configureTaskDependencies(project, analyzeTask.get(), aspectJConfigTask.get())
            configureAspectJIfEnabled(project, extension)
        }
    }

    private fun configureTaskDependencies(project: Project, analyzeTask: AnalyzeDddClassesTask, aspectJConfigTask: AspectJConfigurationTask) {
        // 查找编译任务
        val compileKotlinTask = project.tasks.findByName("compileKotlin")
        val compileJavaTask = project.tasks.findByName("compileJava")
        val classesTask = project.tasks.findByName("classes")
        val processResourcesTask = project.tasks.findByName("processResources")

        // 分析任务依赖于编译任务
        compileKotlinTask?.let { analyzeTask.dependsOn(it) }
        compileJavaTask?.let { analyzeTask.dependsOn(it) }

        // 让 processResources 任务依赖分析任务，确保 JSON 文件在资源处理前生成
        processResourcesTask?.dependsOn(analyzeTask)

        // 让 classes 任务依赖分析任务，这样 build 时会自动运行分析
        classesTask?.dependsOn(analyzeTask)

        // 查找可能的 AspectJ 相关任务
        val aspectjTask = project.tasks.findByName("compileAspect")
            ?: project.tasks.findByName("aspectjWeave")
            ?: project.tasks.findByName("aspectjCompile")

        // 如果有 AspectJ 任务，让它依赖于分析任务
        aspectjTask?.dependsOn(analyzeTask)

        // 让 jar 任务也依赖分析任务，确保打包前完成分析
        project.tasks.findByName("jar")?.dependsOn(analyzeTask)

        // 如果有测试任务，也让它依赖分析任务
        project.tasks.findByName("test")?.dependsOn(analyzeTask)

        // AspectJ 配置任务应该在编译之前运行
        compileKotlinTask?.dependsOn(aspectJConfigTask)
        compileJavaTask?.dependsOn(aspectJConfigTask)
    }

    private fun configureAspectJIfEnabled(project: Project, extension: PragmaDddAnalyzerExtension) {
        if (extension.enableAspectJ.getOrElse(true)) {
            configureAspectJ(project)
        }
    }
    
    private fun configureAspectJ(project: Project) {
        try {
            // 1. 确保插件仓库可用
            configurePluginRepositories(project)
            
            // 2. 应用 AspectJ 插件
            project.pluginManager.apply("io.freefair.aspectj.post-compile-weaving")
            
            // 3. 添加 AspectJ 运行时依赖
            project.dependencies.add("implementation", "org.aspectj:aspectjrt:1.9.7")
            
            // 4. 添加 pragma-ddd-core 依赖（普通依赖）
            try {
                project.dependencies.add("implementation", project.project(":pragma-ddd-core"))
                project.logger.info("已自动配置 implementation 依赖: pragma-ddd-core")
            } catch (e: Exception) {
                val version = getPluginVersion()
                project.logger.info("本地 pragma-ddd-core 模块不存在，将使用外部依赖版本: $version")
                project.dependencies.add("implementation", "org.morecup.pragmaddd:pragma-ddd-core:$version")
            }
            
            // 5. 关键：添加 aspect 依赖（AspectJ 特有的配置）
            try {
                project.dependencies.add("aspect", project.project(":pragma-ddd-aspect"))
                project.logger.info("已自动配置 aspect 依赖: pragma-ddd-aspect")
            } catch (e: Exception) {
                val version = getPluginVersion()
                project.logger.info("本地 pragma-ddd-aspect 模块不存在，将使用外部依赖版本: $version")
                project.dependencies.add("aspect", "org.morecup.pragmaddd:pragma-ddd-aspect:$version")
            }
            
            // 6. 配置 AspectJ 编译选项
            configureAspectJCompileOptions(project)
            
            if (project.logger.isInfoEnabled) {
                project.logger.info("已自动配置 AspectJ 织入功能，包括 aspect() 依赖")
            }
            
        } catch (e: Exception) {
            project.logger.error("配置 AspectJ 时出错: ${e.message}", e)
            throw e
        }
    }
    
    private fun configureAspectJCompileOptions(project: Project) {
        // 配置 AspectJ 编译选项
        project.afterEvaluate {
            // 查找 AspectJ 相关任务并配置
            val aspectjTasks = project.tasks.withType(org.gradle.api.Task::class.java).filter { task ->
                task.javaClass.name.contains("AspectJ", ignoreCase = true)
            }
            
            aspectjTasks.forEach { task ->
                project.logger.info("配置 AspectJ 任务: ${task.name}")
                
                // 这里可以添加更多的 AspectJ 配置
                // 比如设置编译参数、类路径等
            }
            
            // 确保 aspect 类在类路径中
            val compileClasspath = project.configurations.findByName("compileClasspath")
            val runtimeClasspath = project.configurations.findByName("runtimeClasspath")
            
            compileClasspath?.let { classpath ->
                project.logger.info("AspectJ 编译类路径已配置")
            }
        }
    }
    
    private fun configurePluginRepositories(project: Project) {
        // AspectJ 插件依赖已经在插件的 build.gradle.kts 中预先声明
        // 这样用户项目就能自动找到 AspectJ 插件
        project.logger.info("AspectJ 插件依赖已预配置")
    }
}