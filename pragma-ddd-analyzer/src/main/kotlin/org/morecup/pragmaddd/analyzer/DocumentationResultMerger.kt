package org.morecup.pragmaddd.analyzer

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File

/**
 * 文档分析结果合并器
 * 将main和test源集的分析结果合并到一个JSON文件中
 */
class DocumentationResultMerger {
    
    private val mapper = jacksonObjectMapper().apply {
        enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT)
    }
    
    /**
     * 合并main和test源集的文档分析结果
     */
    fun mergeResults(
        mainResultFile: File?,
        testResultFile: File?,
        outputFile: File
    ) {
        val mainResult = mainResultFile?.let { readDocumentationResult(it) }
        val testResult = testResultFile?.let { readDocumentationResult(it) }
        
        val mergedResult = DocumentationAnalysisResult(
            main = mainResult?.main,
            test = testResult?.test
        )
        
        // 确保输出目录存在
        outputFile.parentFile.mkdirs()
        
        // 写入合并后的结果
        outputFile.writeText(mapper.writeValueAsString(mergedResult))
    }
    
    /**
     * 从文件读取文档分析结果
     */
    private fun readDocumentationResult(file: File): DocumentationAnalysisResult? {
        return try {
            if (file.exists()) {
                mapper.readValue<DocumentationAnalysisResult>(file)
            } else {
                null
            }
        } catch (e: Exception) {
            println("读取文档分析结果失败: ${file.absolutePath}, 错误: ${e.message}")
            null
        }
    }
    
    /**
     * 检查是否需要合并（即是否存在多个源集的结果）
     */
    fun shouldMerge(mainResultFile: File?, testResultFile: File?): Boolean {
        val mainExists = mainResultFile?.exists() == true
        val testExists = testResultFile?.exists() == true
        return mainExists && testExists
    }
}