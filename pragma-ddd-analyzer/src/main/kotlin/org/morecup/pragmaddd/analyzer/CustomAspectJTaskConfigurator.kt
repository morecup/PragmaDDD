package org.morecup.pragmaddd.analyzer

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File

/**
 * 自定义 AspectJ 任务配置器
 * 负责创建和配置自定义的 AspectJ 编译任务
 */
class CustomAspectJTaskConfigurator {

    fun configureCustomAspectJTask(
        project: Project,
        analyzeTask: AnalyzeDddClassesTask,
        extension: PragmaDddAnalyzerExtension
    ) {
        if (extension.aspectJMode.getOrElse(AspectJMode.ENABLED) != AspectJMode.ENABLED) {
            project.logger.warn("AspectJ 功能已禁用，跳过自定义 AspectJ 任务配置")
            return
        }

        // 创建自定义 AspectJ 任务
        val customAspectJTask = project.tasks.register("customAspectJWeave", CustomAspectJTask::class.java) { task ->
            task.group = "aspectj"
            task.description = "使用自定义 AspectJ 任务进行织入，确保正确的执行顺序"

            configureTaskInputsAndOutputs(project, task)
            configureTaskOptions(task, extension)
        }

        // 配置任务依赖关系
        configureTaskDependencies(project, customAspectJTask.get(), analyzeTask)

        project.logger.warn("✓ 已配置自定义 AspectJ 任务: customAspectJWeave")
    }

    private fun configureTaskInputsAndOutputs(project: Project, task: CustomAspectJTask) {
        // 配置源文件（编译后的 class 文件）
        task.sourceFiles.from(project.provider { getCompiledClassesDirs(project) })

        // 配置 aspect 路径 - 使用延迟配置
        task.aspectPath.from(project.provider { 
            try {
                getAspectPathFiles(project)
            } catch (e: Exception) {
                project.logger.warn("延迟获取 aspect 路径失败: ${e.message}")
                emptyList<File>()
            }
        })

        // 配置类路径 - 使用延迟配置
        task.classPath.from(project.provider {
            try {
                getClassPathFiles(project)
            } catch (e: Exception) {
                project.logger.warn("延迟获取类路径失败: ${e.message}")
                emptyList<File>()
            }
        })

        // 配置输出目录 - 直接使用原本的编译输出目录，而不是单独的目录
        // 这样 AspectJ 织入后的 class 文件会直接替换原本的 class 文件
        val mainOutputDir = getMainCompiledClassesDir(project)
        task.outputDirectory.set(mainOutputDir)
    }

    private fun configureTaskOptions(task: CustomAspectJTask, extension: PragmaDddAnalyzerExtension) {
        task.verbose.set(extension.verbose.getOrElse(false))
        task.showWeaveInfo.set(true)

        // 可以根据需要添加更多配置选项
        task.additionalArgs.set(listOf(
            "-Xlint:ignore",  // 忽略一些警告
            "-proceedOnError" // 遇到错误时继续处理
        ))
    }

    private fun configureTaskDependencies(
        project: Project,
        customAspectJTask: CustomAspectJTask,
        analyzeTask: AnalyzeDddClassesTask
    ) {
        // 关键：确保执行顺序 编译 → 分析 → AspectJ织入

        // 1. 自定义 AspectJ 任务必须在分析任务之后执行
        customAspectJTask.dependsOn(analyzeTask)
        customAspectJTask.mustRunAfter(analyzeTask)

        // 2. 自定义 AspectJ 任务依赖于编译任务
        val compileTasks = getCompileTasks(project)
        compileTasks.forEach { compileTask ->
            customAspectJTask.dependsOn(compileTask)
        }

        // 3. 禁用原有的 AspectJ 插件任务（如果存在）
        disableOriginalAspectJTasks(project)

        // 4. 让其他任务依赖于自定义 AspectJ 任务
        configureDownstreamTaskDependencies(project, customAspectJTask)

        project.logger.info("✓ 配置任务执行顺序: 编译 → 分析 → 自定义AspectJ织入")
        project.logger.info("✓ 这样确保分析任务读取原始字节码，AspectJ在分析后进行织入")
    }

    private fun getCompiledClassesDirs(project: Project): List<File> {
        val dirs = mutableListOf<File>()

        // Kotlin 编译输出
        dirs.add(File(project.buildDir, "classes/kotlin/main"))

        // Java 编译输出
        dirs.add(File(project.buildDir, "classes/java/main"))

        return dirs.filter { it.exists() }
    }

    private fun getMainCompiledClassesDir(project: Project): File {
        // 优先使用 Kotlin 编译输出目录，如果不存在则使用 Java 编译输出目录
        val kotlinDir = File(project.buildDir, "classes/kotlin/main")
        val javaDir = File(project.buildDir, "classes/java/main")
        
        return when {
            kotlinDir.exists() -> kotlinDir
            javaDir.exists() -> javaDir
            else -> kotlinDir // 默认使用 Kotlin 目录，即使不存在也会被创建
        }
    }

    private fun getAspectPathFiles(project: Project): List<File> {
        val files = mutableListOf<File>()

        try {
            // 首先尝试从本地项目模块获取
            try {
                val aspectProject = project.project(":pragma-ddd-aspect")
                val aspectJar = File(aspectProject.buildDir, "libs/pragma-ddd-aspect.jar")
                if (aspectJar.exists()) {
                    files.add(aspectJar)
                    project.logger.info("找到本地 aspect JAR: ${aspectJar.absolutePath}")
                }
            } catch (e: Exception) {
                project.logger.debug("本地 pragma-ddd-aspect 模块不存在: ${e.message}")
            }

            // 然后从 aspect 配置中获取，但只获取真正的aspect文件
            val aspectConfiguration = project.configurations.findByName("aspect")
            if (aspectConfiguration != null) {
                val aspectFiles = aspectConfiguration.incoming.files.files.filter { file ->
                    // 只包含真正的aspect文件，排除AspectJ运行时和其他依赖
                    val fileName = file.name.lowercase()
                    val isAspectFile = when {
                        // 包含我们自己的aspect模块
                        fileName.contains("pragma-ddd-aspect") -> true
                        // 排除AspectJ运行时库
                        fileName.contains("aspectjrt") -> false
                        // 排除Kotlin标准库
                        fileName.contains("kotlin-stdlib") -> false
                        // 排除注解库
                        fileName.contains("annotations") -> false
                        // 排除其他常见的非aspect依赖
                        fileName.contains("slf4j") -> false
                        fileName.contains("logback") -> false
                        fileName.contains("jackson") -> false
                        fileName.contains("spring") -> false
                        // 默认情况下，如果文件名包含aspect相关关键词，则认为是aspect文件
                        fileName.contains("aspect") -> true
                        // 其他情况默认不包含
                        else -> false
                    }
                    
                    if (isAspectFile) {
                        project.logger.info("包含 aspect 文件: ${file.absolutePath}")
                    } else {
                        project.logger.debug("排除非 aspect 文件: ${file.absolutePath}")
                    }
                    
                    isAspectFile
                }
                
                files.addAll(aspectFiles)
                project.logger.info("从 aspect 配置过滤后获取到 ${aspectFiles.size} 个真正的aspect文件")
            }
        } catch (e: Exception) {
            project.logger.warn("获取 aspect 路径时出错: ${e.message}")
        }

        // 去重并记录最终结果
        val uniqueFiles = files.distinct()
        project.logger.warn("最终找到 ${uniqueFiles.size} 个aspect文件:")
        uniqueFiles.forEach { file ->
            project.logger.warn("  - ${file.absolutePath}")
        }

        return uniqueFiles
    }

    private fun getClassPathFiles(project: Project): List<File> {
        val files = mutableListOf<File>()

        try {
            // 获取运行时类路径
            val runtimeClasspath = project.configurations.findByName("runtimeClasspath")
            if (runtimeClasspath != null) {
                val classpathFiles = runtimeClasspath.incoming.files.files
                files.addAll(classpathFiles)
                project.logger.info("从 runtimeClasspath 获取到 ${classpathFiles.size} 个文件")
            }
        } catch (e: Exception) {
            project.logger.warn("获取类路径时出错: ${e.message}")
            
            // 备用方案：使用编译类路径
            try {
                val compileClasspath = project.configurations.findByName("compileClasspath")
                if (compileClasspath != null) {
                    val classpathFiles = compileClasspath.incoming.files.files
                    files.addAll(classpathFiles)
                    project.logger.info("从 compileClasspath 获取到 ${classpathFiles.size} 个文件")
                }
            } catch (e2: Exception) {
                project.logger.warn("获取编译类路径也失败: ${e2.message}")
            }
        }

        return files
    }

    private fun getCompileTasks(project: Project): List<Task> {
        val tasks = mutableListOf<Task>()

        // Kotlin 编译任务
        project.tasks.withType(KotlinCompile::class.java).forEach { task ->
            if (task.name.contains("main", ignoreCase = true)) {
                tasks.add(task)
            }
        }

        // Java 编译任务
        project.tasks.withType(JavaCompile::class.java).forEach { task ->
            if (task.name.contains("main", ignoreCase = true)) {
                tasks.add(task)
            }
        }

        return tasks
    }

    private fun disableOriginalAspectJTasks(project: Project) {
        // 查找并禁用原有的 AspectJ 插件任务
        val aspectJTasks = project.tasks.matching { task ->
            val taskClass = task.javaClass.name
            val taskName = task.name

            (taskClass.contains("AspectJ", ignoreCase = true) &&
             taskClass.contains("Weave", ignoreCase = true)) ||
            taskName.contains("aspectjPostCompileWeave", ignoreCase = true)
        }

        aspectJTasks.configureEach { task ->
            task.enabled = false
            project.logger.info("✓ 已禁用原有 AspectJ 任务: ${task.name}")
        }
    }

    private fun configureDownstreamTaskDependencies(project: Project, customAspectJTask: CustomAspectJTask) {
        // 让测试任务依赖于自定义 AspectJ 任务
        project.tasks.matching { task ->
            task.name.contains("test", ignoreCase = true) &&
            !task.name.contains("testAspect", ignoreCase = true)
        }.configureEach { task ->
            task.dependsOn(customAspectJTask)
        }

        // 让 jar 任务依赖于自定义 AspectJ 任务
        project.tasks.matching { task ->
            task.name == "jar" || task.name == "bootJar"
        }.configureEach { task ->
            task.dependsOn(customAspectJTask)
        }
    }
}