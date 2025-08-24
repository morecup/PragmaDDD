package org.morecup.pragmaddd.analyzer

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.morecup.pragmaddd.analyzer.model.DetailedAnalysisResult
import java.io.File

/**
 * 详细分析结果写入器
 * 负责将详细的分析结果写入 JSON 文件
 */
class DetailedAnalysisResultWriter {
    
    private val objectMapper: ObjectMapper = jacksonObjectMapper().apply {
        enable(SerializationFeature.INDENT_OUTPUT)
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }
    
    /**
     * 将分析结果写入文件
     * 
     * @param result 分析结果
     * @param outputFile 输出文件
     */
    fun writeToFile(result: DetailedAnalysisResult, outputFile: File) {
        try {
            // 确保输出目录存在
            outputFile.parentFile?.mkdirs()
            
            // 写入 JSON 文件
            objectMapper.writeValue(outputFile, result)
            
            println("[Pragma DDD] 详细分析结果已写入: ${outputFile.absolutePath}")
            println("[Pragma DDD] 分析摘要:")
            println("  - 源集: ${result.sourceSetName}")
            println("  - 总类数: ${result.summary.totalClasses}")
            println("  - 聚合根: ${result.summary.aggregateRootCount}")
            println("  - 领域实体: ${result.summary.domainEntityCount}")
            println("  - 值对象: ${result.summary.valueObjectCount}")
            println("  - 总字段数: ${result.summary.totalFields}")
            println("  - 总方法数: ${result.summary.totalMethods}")
            println("  - 有文档的类: ${result.summary.classesWithDocumentation}")
            println("  - 有文档的字段: ${result.summary.fieldsWithDocumentation}")
            println("  - 有文档的方法: ${result.summary.methodsWithDocumentation}")
            
        } catch (e: Exception) {
            println("[Pragma DDD] 写入分析结果失败: ${e.message}")
            throw e
        }
    }
    
    /**
     * 将分析结果转换为 JSON 字符串
     * 
     * @param result 分析结果
     * @return JSON 字符串
     */
    fun toJsonString(result: DetailedAnalysisResult): String {
        return objectMapper.writeValueAsString(result)
    }
}
