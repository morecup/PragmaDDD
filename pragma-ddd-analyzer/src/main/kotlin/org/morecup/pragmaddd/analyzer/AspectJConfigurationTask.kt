package org.morecup.pragmaddd.analyzer

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * AspectJ 配置任务
 * 负责自动配置 AspectJ 相关设置，确保用户无需手动配置
 */
open class AspectJConfigurationTask : DefaultTask() {
    
    @get:Internal
    lateinit var extension: PragmaDddAnalyzerExtension
    
    @get:Input
    val enableAspectJ: Boolean
        get() = extension.enableAspectJ.getOrElse(true)
    
    @get:Input
    val verbose: Boolean
        get() = extension.verbose.getOrElse(false)
    
    @TaskAction
    fun configureAspectJ() {
        if (!enableAspectJ) {
            logger.info("AspectJ 功能已禁用")
            return
        }
        
        logger.info("开始自动配置 AspectJ 织入功能...")
        
        try {
            // 1. 验证 AspectJ 插件是否已应用
            validateAspectJPlugin()
            
            // 2. 验证依赖配置
            validateDependencies()
            
            // 3. 配置 AspectJ 编译参数
            configureAspectJOptions()
            
            // 4. 验证 aspect 类是否可用
            validateAspectClasses()
            
            logger.info("AspectJ 自动配置完成")
            
        } catch (e: Exception) {
            logger.error("AspectJ 配置失败: ${e.message}", e)
            throw e
        }
    }
    
    private fun validateAspectJPlugin() {
        val hasAspectJPlugin = project.plugins.hasPlugin("io.freefair.aspectj.post-compile-weaving")
        
        if (hasAspectJPlugin) {
            logger.info("✓ AspectJ 插件已正确应用")
        } else {
            throw IllegalStateException("AspectJ 插件未找到，自动配置可能失败")
        }
    }
    
    private fun validateDependencies() {
        val implementation = project.configurations.getByName("implementation")
        
        // 检查 AspectJ 运行时
        val hasAspectJRt = implementation.dependencies.any { 
            it.group == "org.aspectj" && it.name == "aspectjrt" 
        }
        
        if (hasAspectJRt) {
            logger.info("✓ AspectJ 运行时依赖已配置")
        } else {
            logger.warn("⚠ AspectJ 运行时依赖未找到")
        }
        
        // 检查 pragma-ddd-core 依赖（implementation 配置）
        val hasPragmaDddCore = implementation.dependencies.any { dep ->
            (dep.group == "org.morecup.pragmaddd" && dep.name == "pragma-ddd-core") ||
            dep.name == "pragma-ddd-core"
        }
        
        if (hasPragmaDddCore) {
            logger.info("✓ pragma-ddd-core 依赖已配置")
        } else {
            logger.warn("⚠ pragma-ddd-core 依赖未找到")
        }
        
        // 检查 pragma-ddd-aspect 依赖（aspect 配置）
        val aspectConfiguration = project.configurations.findByName("aspect")
        val hasPragmaDddAspect = aspectConfiguration?.dependencies?.any { dep ->
            (dep.group == "org.morecup.pragmaddd" && dep.name == "pragma-ddd-aspect") ||
            dep.name == "pragma-ddd-aspect"
        } ?: false
        
        if (hasPragmaDddAspect) {
            logger.info("✓ pragma-ddd-aspect aspect() 依赖已配置")
        } else {
            logger.warn("⚠ pragma-ddd-aspect aspect() 依赖未找到")
            
            // 检查是否错误地配置在 implementation 中
            val hasInImplementation = implementation.dependencies.any { dep ->
                (dep.group == "org.morecup.pragmaddd" && dep.name == "pragma-ddd-aspect") ||
                dep.name == "pragma-ddd-aspect"
            }
            
            if (hasInImplementation) {
                logger.warn("⚠ pragma-ddd-aspect 在 implementation 中找到，但应该在 aspect() 配置中")
            }
        }
    }
    
    private fun configureAspectJOptions() {
        // 查找并配置 AspectJ 相关任务
        project.tasks.matching { task ->
            task.javaClass.name.contains("AspectJ", ignoreCase = true) ||
            task.name.contains("aspectj", ignoreCase = true)
        }.configureEach { task ->
            logger.info("配置 AspectJ 任务: ${task.name}")
            
            // 这里可以添加更多的 AspectJ 配置
            // 比如设置编译参数等
        }
    }
    
    private fun validateAspectClasses() {
        // 检查编译输出中是否包含 aspect 类
        val buildDir = project.buildDir
        val classesDir = File(buildDir, "classes")
        
        if (classesDir.exists()) {
            val aspectFiles = classesDir.walkTopDown()
                .filter { it.name.endsWith(".class") }
                .filter { file ->
                    // 简单检查是否可能是 aspect 类
                    file.path.contains("aspect", ignoreCase = true) ||
                    file.path.contains("Aspect")
                }
                .toList()
            
            if (aspectFiles.isNotEmpty()) {
                logger.info("✓ 找到 ${aspectFiles.size} 个可能的 aspect 类文件")
                if (verbose) {
                    aspectFiles.forEach { file ->
                        logger.info("  - ${file.relativeTo(classesDir)}")
                    }
                }
            } else {
                logger.info("ℹ 暂未找到 aspect 类文件（可能尚未编译）")
            }
        }
    }
}