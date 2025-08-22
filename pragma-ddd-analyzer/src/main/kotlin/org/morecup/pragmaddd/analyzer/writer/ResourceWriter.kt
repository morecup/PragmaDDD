package org.morecup.pragmaddd.analyzer.writer

import org.morecup.pragmaddd.analyzer.model.ClassMetadata
import java.io.File
import java.nio.file.Path

/**
 * 资源写入器接口 - 定义将 JSON 文件写入 META-INF 目录的契约
 */
interface ResourceWriter {
    
    /**
     * 将主源码 JSON 写入 META-INF 目录
     * 
     * @param json JSON 内容
     * @param outputDirectory 输出目录路径
     * @param fileName 文件名，默认为 "ddd-analysis-main.json"
     */
    fun writeMainSourcesJson(
        json: String,
        outputDirectory: String,
        fileName: String = "ddd-analysis-main.json"
    )
    
    /**
     * 将主源码 JSON 写入固定的目录路径
     * 路径固定为: build/generated/pragmaddd/main/resources/META-INF/pragma-ddd-analyzer/domain-analyzer.json
     * 
     * @param json JSON 内容
     */
    fun writeMainSourcesJsonToFixedPath(json: String)
    
    /**
     * 将 JSON 文件写入指定的资源目录
     * 
     * @param json JSON 内容
     * @param resourcePath 资源路径（相对于 META-INF）
     * @param outputDirectory 输出目录路径
     */
    fun writeJsonToResource(
        json: String,
        resourcePath: String,
        outputDirectory: String
    )
    
    /**
     * 创建 META-INF 目录结构
     * 
     * @param outputDirectory 输出目录路径
     * @return 创建的 META-INF 目录路径
     */
    fun createMetaInfDirectory(outputDirectory: String): Path
    
    /**
     * 验证输出目录是否可写
     * 
     * @param outputDirectory 输出目录路径
     * @return 是否可写
     */
    fun isOutputDirectoryWritable(outputDirectory: String): Boolean
    
    /**
     * 获取资源文件的完整路径
     * 
     * @param outputDirectory 输出目录路径
     * @param resourcePath 资源路径（相对于 META-INF）
     * @return 完整的文件路径
     */
    fun getResourceFilePath(outputDirectory: String, resourcePath: String): Path
}

/**
 * 资源写入器实现 - 将 JSON 文件写入 META-INF 目录以便打包到 JAR 中
 */
class ResourceWriterImpl : ResourceWriter {
    
    companion object {
        private const val META_INF_DIR = "META-INF"
        private const val DDD_ANALYSIS_DIR = "pragma-ddd-analyzer"
        
        // Fixed paths that cannot be configured by users
        private const val FIXED_OUTPUT_BASE = "build/generated/pragmaddd/main/resources"
        private const val FIXED_FILENAME = "domain-analyzer.json"
        private const val FIXED_COMPLETE_PATH = "$FIXED_OUTPUT_BASE/$META_INF_DIR/$DDD_ANALYSIS_DIR/$FIXED_FILENAME"
    }
    
    /**
     * 将主源码 JSON 写入 META-INF 目录
     * Note: This method now ignores the parameters and always uses the fixed path
     * Fixed path: build/generated/pragmaddd/main/resources/META-INF/pragma-ddd-analyzer/domain-analyzer.json
     */
    override fun writeMainSourcesJson(
        json: String,
        outputDirectory: String,
        fileName: String
    ) {
        // Always use fixed path - parameters are ignored to prevent user configuration
        writeMainSourcesJsonToFixedPath(json)
    }
    
    /**
     * 将主源码 JSON 写入固定的目录路径
     * 路径固定为: build/generated/pragmaddd/main/resources/META-INF/pragma-ddd-analyzer/domain-analyzer.json
     * 此路径不允许用户配置
     */
    override fun writeMainSourcesJsonToFixedPath(json: String) {
        val fixedOutputDirectory = FIXED_OUTPUT_BASE
        val resourcePath = "$DDD_ANALYSIS_DIR/$FIXED_FILENAME"
        println("DDD Analyzer: writeMainSourcesJsonToFixedPath called")
        println("DDD Analyzer: fixedOutputDirectory = $fixedOutputDirectory")
        println("DDD Analyzer: resourcePath = $resourcePath")
        println("DDD Analyzer: Full path will be: $fixedOutputDirectory/META-INF/$resourcePath")
        writeJsonToResource(json, resourcePath, fixedOutputDirectory)
    }
    
    /**
     * 将 JSON 文件写入指定的资源目录
     */
    override fun writeJsonToResource(
        json: String,
        resourcePath: String,
        outputDirectory: String
    ) {
        println("DDD Analyzer ResourceWriter: writeJsonToResource called")
        println("DDD Analyzer ResourceWriter: outputDirectory = $outputDirectory")
        println("DDD Analyzer ResourceWriter: resourcePath = $resourcePath")
        
        val metaInfDir = createMetaInfDirectory(outputDirectory)
        println("DDD Analyzer ResourceWriter: metaInfDir = $metaInfDir")
        
        val fullResourcePath = metaInfDir.resolve(resourcePath)
        println("DDD Analyzer ResourceWriter: fullResourcePath = $fullResourcePath")
        
        // 确保父目录存在
        val parentDir = fullResourcePath.parent?.toFile()
        if (parentDir != null) {
            println("DDD Analyzer ResourceWriter: Creating parent directory: ${parentDir.absolutePath}")
            val created = parentDir.mkdirs()
            println("DDD Analyzer ResourceWriter: Parent directory creation result: $created")
            println("DDD Analyzer ResourceWriter: Parent directory exists: ${parentDir.exists()}")
        }
        
        // 写入 JSON 文件
        try {
            val targetFile = fullResourcePath.toFile()
            println("DDD Analyzer ResourceWriter: Writing to file: ${targetFile.absolutePath}")
            targetFile.writeText(json)
            println("DDD Analyzer ResourceWriter: File written successfully, size: ${targetFile.length()} bytes")
        } catch (e: Exception) {
            println("DDD Analyzer ResourceWriter: Error writing file: ${e.message}")
            throw e
        }
    }
    
    /**
     * 创建 META-INF 目录结构
     */
    override fun createMetaInfDirectory(outputDirectory: String): Path {
        println("DDD Analyzer ResourceWriter: createMetaInfDirectory called with: $outputDirectory")
        
        val outputDir = File(outputDirectory)
        println("DDD Analyzer ResourceWriter: outputDir = ${outputDir.absolutePath}")
        println("DDD Analyzer ResourceWriter: outputDir exists = ${outputDir.exists()}")
        
        val metaInfDir = outputDir.resolve(META_INF_DIR)
        println("DDD Analyzer ResourceWriter: metaInfDir = ${metaInfDir.absolutePath}")
        
        // 创建目录结构
        val created = metaInfDir.mkdirs()
        println("DDD Analyzer ResourceWriter: metaInfDir.mkdirs() result = $created")
        println("DDD Analyzer ResourceWriter: metaInfDir exists after mkdirs = ${metaInfDir.exists()}")
        
        return metaInfDir.toPath()
    }
    
    /**
     * 验证输出目录是否可写
     */
    override fun isOutputDirectoryWritable(outputDirectory: String): Boolean {
        return try {
            val outputDir = File(outputDirectory)
            
            // 如果目录不存在，尝试创建
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }
            
            // 检查是否可写
            outputDir.canWrite()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 获取资源文件的完整路径
     */
    override fun getResourceFilePath(outputDirectory: String, resourcePath: String): Path {
        val metaInfDir = File(outputDirectory).resolve(META_INF_DIR)
        return metaInfDir.resolve(resourcePath).toPath()
    }
    
    /**
     * 清理指定目录下的旧 JSON 文件
     * 
     * @param outputDirectory 输出目录路径
     */
    fun cleanupOldJsonFiles(outputDirectory: String) {
        try {
            val dddAnalysisDir = getResourceFilePath(outputDirectory, DDD_ANALYSIS_DIR).toFile()
            if (dddAnalysisDir.exists() && dddAnalysisDir.isDirectory) {
                dddAnalysisDir.listFiles { _, name -> 
                    name.endsWith(".json") 
                }?.forEach { file ->
                    file.delete()
                }
            }
        } catch (e: Exception) {
            // 忽略清理错误，不影响主要功能
        }
    }
    
    /**
     * 获取 META-INF 目录中的所有 JSON 文件
     * 
     * @param outputDirectory 输出目录路径
     * @return JSON 文件列表
     */
    fun listJsonFiles(outputDirectory: String): List<File> {
        return try {
            val dddAnalysisDir = getResourceFilePath(outputDirectory, DDD_ANALYSIS_DIR).toFile()
            if (dddAnalysisDir.exists() && dddAnalysisDir.isDirectory) {
                dddAnalysisDir.listFiles { _, name -> 
                    name.endsWith(".json") 
                }?.toList() ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 验证 JSON 文件是否成功写入
     * 
     * @param outputDirectory 输出目录路径
     * @param resourcePath 资源路径
     * @return 文件是否存在且可读
     */
    fun verifyJsonFileWritten(outputDirectory: String, resourcePath: String): Boolean {
        return try {
            val filePath = getResourceFilePath(outputDirectory, resourcePath)
            val file = filePath.toFile()
            file.exists() && file.canRead() && file.length() > 0
        } catch (e: Exception) {
            false
        }
    }
}