package org.morecup.pragmaddd.analyzer

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.File
import javax.inject.Inject

/**
 * 自定义 AspectJ 织入任务
 * 直接在原本的编译输出目录上进行 AspectJ 织入，确保正确的执行顺序
 * 
 * 执行顺序：编译 → 分析 → AspectJ织入
 * 这样确保：
 * 1. 分析任务读取的是原始字节码，而不是被AspectJ修改后的字节码
 * 2. AspectJ织入后的class文件直接替换原始文件，后续任务可以正常使用
 */
@CacheableTask
abstract class CustomAspectJTask @Inject constructor() : DefaultTask() {
    
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceFiles: ConfigurableFileCollection
    
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val aspectPath: ConfigurableFileCollection
    
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val classPath: ConfigurableFileCollection
    
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty
    
    @get:Input
    abstract val verbose: Property<Boolean>
    
    @get:Input
    abstract val showWeaveInfo: Property<Boolean>
    
    @get:Input
    @get:Optional
    abstract val additionalArgs: ListProperty<String>
    
    init {
        description = "自定义 AspectJ 织入任务，直接在原编译目录上进行织入"
        group = "aspectj"
        
        // 设置默认值
        verbose.convention(false)
        showWeaveInfo.convention(true)
        additionalArgs.convention(emptyList())
    }
    
    @TaskAction
    fun compile() {
        val outputDir = outputDirectory.get().asFile
        
        logger.warn("开始执行自定义 AspectJ 织入...")
        logger.warn("目标目录: ${outputDir.absolutePath}")
        
        // 检查目标目录是否存在编译后的 class 文件
        if (!outputDir.exists() || outputDir.listFiles()?.isEmpty() == true) {
            logger.warn("目标目录不存在或为空，跳过 AspectJ 织入")
            return
        }
        
        // 检查是否有aspect文件
        val aspectFiles = aspectPath.files
        if (aspectFiles.isEmpty()) {
            logger.warn("没有找到aspect文件，跳过AspectJ织入")
            return
        }
        
        logger.warn("找到 ${aspectFiles.size} 个aspect文件")
        aspectFiles.forEach { file ->
            logger.warn("  - ${file.absolutePath}")
        }
        
        try {
            // 直接在原本的编译输出目录上进行AspectJ织入
            performAspectJWeaving(outputDir)
            logger.warn("AspectJ 织入完成，已直接修改原始 class 文件")
        } catch (e: Exception) {
            logger.error("AspectJ 织入失败: ${e.message}", e)
            throw e
        }
    }
    
    private fun performAspectJWeaving(outputDir: File) {
        // 直接在原本的编译输出目录上进行 AspectJ 织入
        // 不需要复制文件，直接修改现有的 class 文件
        val ajcArgs = buildAjcArguments(outputDir)
        
        if (verbose.get()) {
            logger.info("AspectJ 编译参数: ${ajcArgs.joinToString(" ")}")
        }
        
        executeAjc(ajcArgs)
    }
    

    
    private fun buildAjcArguments(outputDir: File): List<String> {
        val args = mutableListOf<String>()
        
        // 输出目录 - 直接输出到原本的编译目录
        args.addAll(listOf("-d", outputDir.absolutePath))
        
        // 类路径
        val classPathFiles = classPath.files
        if (classPathFiles.isNotEmpty()) {
            args.addAll(listOf("-classpath", classPathFiles.joinToString(File.pathSeparator) { it.absolutePath }))
        }
        
        // Aspect 路径
        val aspectPathFiles = aspectPath.files
        if (aspectPathFiles.isNotEmpty()) {
            args.addAll(listOf("-aspectpath", aspectPathFiles.joinToString(File.pathSeparator) { it.absolutePath }))
        }
        
        // 编译选项
        if (verbose.get()) {
            args.add("-verbose")
        }
        
        if (showWeaveInfo.get()) {
            args.add("-showWeaveInfo")
        }
        
        // 源码兼容性
        args.addAll(listOf("-source", "11"))
        args.addAll(listOf("-target", "11"))
        
        // 额外参数
        args.addAll(additionalArgs.get())
        
        // 输入路径 - 直接使用原本的编译输出目录作为输入
        // 这样 AspectJ 会读取现有的 class 文件，进行织入，然后输出到同一个目录
        args.add("-inpath")
        args.add(outputDir.absolutePath)
        
        return args
    }
    
    private fun executeAjc(args: List<String>) {
        logger.info("执行 AspectJ 编译，参数: ${args.take(10).joinToString(" ")}...")
        
        try {
            // 在任务执行时才加载AspectJ类
            executeAjcDirect(args)
        } catch (e: Exception) {
            logger.warn("AspectJ 织入失败: ${e.message}")
            throw e
        }
    }
    
    private fun executeAjcDirect(args: List<String>)
    {
        // 使用 AspectJ 的 Main 类直接编译
        val ajcMainClass = try {
            // 尝试加载 AspectJ 编译器主类
            Class.forName("org.aspectj.tools.ajc.Main")
        } catch (e: ClassNotFoundException) {
            throw IllegalStateException("AspectJ 编译器 (aspectjtools) 未找到，请添加 aspectjtools 依赖", e)
        }
        
        val mainMethod = ajcMainClass.getMethod("main", Array<String>::class.java)

        // 捕获 System.exit 调用
        val originalSecurityManager = System.getSecurityManager()
        var exitCode = 0
        
        try {
            // 设置安全管理器防止 System.exit
            System.setSecurityManager(object : SecurityManager() {
                override fun checkExit(status: Int) {
                    exitCode = status
                    throw SecurityException("AspectJ 编译完成，退出码: $status")
                }
                override fun checkPermission(perm: java.security.Permission?) {}
            })

            mainMethod.invoke(null, args.toTypedArray())

        } catch (e: java.lang.reflect.InvocationTargetException) {
            // 处理反射调用中的异常
            val cause = e.cause
            if (cause is SecurityException) {
                // 这是我们预期的 SecurityException，AspectJ 调用了 System.exit
                if (exitCode != 0) {
                    throw RuntimeException("AspectJ 编译失败，退出码: $exitCode", cause)
                }
                // exitCode == 0 表示成功，正常继续
            } else {
                // 其他异常，重新抛出
                throw RuntimeException("AspectJ 编译过程中发生异常", cause ?: e)
            }
        } catch (e: SecurityException) {
            // 直接捕获的 SecurityException（虽然通常不会到这里）
            if (exitCode != 0) {
                throw RuntimeException("AspectJ 编译失败，退出码: $exitCode", e)
            }
        } finally {
            System.setSecurityManager(originalSecurityManager)
        }
        
        logger.info("AspectJ 编译成功完成")
    }
    

}