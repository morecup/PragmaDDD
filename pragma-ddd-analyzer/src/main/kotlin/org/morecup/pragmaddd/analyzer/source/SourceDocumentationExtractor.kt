package org.morecup.pragmaddd.analyzer.source

import java.io.File

/**
 * 源码文档注释提取器接口
 */
interface SourceDocumentationExtractor {
    
    /**
     * 从源文件中提取文档注释信息
     * 
     * @param sourceFile 源文件
     * @return 文档注释信息，如果提取失败返回null
     */
    fun extractDocumentation(sourceFile: File): SourceDocumentationInfo?
    
    /**
     * 检查是否支持该文件类型
     */
    fun supports(sourceFile: File): Boolean
}

/**
 * 源码文档注释信息
 */
data class SourceDocumentationInfo(
    val className: String,                                    // 完整类名
    val classDocumentation: String? = null,                   // 类文档注释
    val fieldDocumentations: Map<String, String> = emptyMap(), // 字段名 -> 文档注释
    val methodDocumentations: Map<String, String> = emptyMap() // 方法签名 -> 文档注释
)

/**
 * 复合文档注释提取器
 * 支持多种源码类型的文档注释提取
 */
class CompositeSourceDocumentationExtractor : SourceDocumentationExtractor {
    
    private val extractors = listOf(
        JavaDocumentationExtractor(),
        KotlinDocumentationExtractor()
    )
    
    override fun extractDocumentation(sourceFile: File): SourceDocumentationInfo? {
        val extractor = extractors.find { it.supports(sourceFile) }
        return extractor?.extractDocumentation(sourceFile)
    }
    
    override fun supports(sourceFile: File): Boolean {
        return extractors.any { it.supports(sourceFile) }
    }
}

/**
 * 源码文件查找器
 */
class SourceFileFinder {
    
    /**
     * 根据类名查找对应的源文件
     * 
     * @param className 完整类名，如 "com.example.Order"
     * @param sourceDirs 源码目录列表
     * @return 找到的源文件，如果未找到返回null
     */
    fun findSourceFile(className: String, sourceDirs: List<File>): File? {
        val possiblePaths = listOf(
            "${className.replace('.', '/')}.java",
            "${className.replace('.', '/')}.kt"
        )
        
        for (sourceDir in sourceDirs) {
            if (!sourceDir.exists() || !sourceDir.isDirectory) continue
            
            for (path in possiblePaths) {
                val sourceFile = File(sourceDir, path)
                if (sourceFile.exists() && sourceFile.isFile) {
                    return sourceFile
                }
            }
        }
        
        return null
    }
    
    /**
     * 获取项目的源码目录
     * 
     * @param projectDir 项目根目录
     * @param sourceSetName 源集名称（main/test）
     * @return 源码目录列表
     */
    fun getSourceDirectories(projectDir: File, sourceSetName: String): List<File> {
        val sourceDirs = mutableListOf<File>()
        
        // 标准的 Maven/Gradle 源码目录结构
        listOf("java", "kotlin").forEach { lang ->
            val sourceDir = File(projectDir, "src/$sourceSetName/$lang")
            if (sourceDir.exists() && sourceDir.isDirectory) {
                sourceDirs.add(sourceDir)
            }
        }
        
        return sourceDirs
    }
}
