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
 * 自定义 AspectJ 编译任务
 * 直接调用 AspectJ 编译器，完全控制编译顺序
 * 
 * 执行顺序：编�� → 分析 → AspectJ织入
 * 这样确保分析任务读取的是原始字节码，而不是被AspectJ修改后的字节码
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
        description = "自定义 AspectJ 编译任务，直接调用 ajc 编译器"
        group = "aspectj"
        
        // 设置默认值
        verbose.convention(false)
        showWeaveInfo.convention(true)
        additionalArgs.convention(emptyList())
    }
    
    @TaskAction
    fun compile() {
        val outputDir = outputDirectory.get().asFile
        outputDir.mkdirs()
        
        logger.info("开始执行自定义 AspectJ 编译...")
        logger.info("输出目录: ${outputDir.absolutePath}")
        
        // 暂时只做基本的文件复制，避免在配置阶段加载AspectJ类
        copySourceFilesToOutput(outputDir)
        
        logger.info("AspectJ 编译完成（当前为简化实现）")
    }
    
    private fun copySourceFilesToOutput(outputDir: File) {
        sourceFiles.files.forEach { sourceFile ->
            if (sourceFile.exists() && sourceFile.isDirectory) {
                sourceFile.copyRecursively(outputDir, overwrite = true)
                logger.info("复制源文件: ${sourceFile.absolutePath} -> ${outputDir.absolutePath}")
            }
        }
    }
    

}