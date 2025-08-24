package org.morecup.pragmaddd.analyzer

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.morecup.pragmaddd.analyzer.model.DetailedAnalysisResult
import org.morecup.pragmaddd.analyzer.model.DetailedClassInfo
import org.morecup.pragmaddd.analyzer.model.AnalysisSummary
import java.io.File

/**
 * 详细分析结果写入器
 * 负责将详细的分析结果写入 JSON 文件，支持增量合并策略
 */
class DetailedAnalysisResultWriter {
    
    private val objectMapper: ObjectMapper = jacksonObjectMapper().apply {
        enable(SerializationFeature.INDENT_OUTPUT)
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }
    
    /**
     * 将分析结果写入文件（支持增量合并）
     * 
     * @param result 新的分析结果
     * @param outputFile 输出文件
     */
    fun writeToFile(result: DetailedAnalysisResult, outputFile: File) {
        try {
            // 确保输出目录存在
            outputFile.parentFile?.mkdirs()
            
            // 读取现有结果并合并
            val mergedResult = mergeWithExistingResult(result, outputFile)
            
            // 写入合并后的 JSON 文件
            objectMapper.writeValue(outputFile, mergedResult)
            
            println("[Pragma DDD] 详细分析结果已写入: ${outputFile.absolutePath}")
            println("[Pragma DDD] 合并后的分析摘要:")
            println("  - 源集: ${mergedResult.sourceSetName}")
            println("  - 总类数: ${mergedResult.summary.totalClasses}")
            println("  - 聚合根: ${mergedResult.summary.aggregateRootCount}")
            println("  - 领域实体: ${mergedResult.summary.domainEntityCount}")
            println("  - 值对象: ${mergedResult.summary.valueObjectCount}")
            println("  - 总字段数: ${mergedResult.summary.totalFields}")
            println("  - 总方法数: ${mergedResult.summary.totalMethods}")
            println("  - 有文档的类: ${mergedResult.summary.classesWithDocumentation}")
            println("  - 有文档的字段: ${mergedResult.summary.fieldsWithDocumentation}")
            println("  - 有文档的方法: ${mergedResult.summary.methodsWithDocumentation}")
            
        } catch (e: Exception) {
            println("[Pragma DDD] 写入分析结果失败: ${e.message}")
            throw e
        }
    }
    
    /**
     * 将新的分析结果与现有文件中的结果合并
     * 
     * @param newResult 新的分析结果
     * @param outputFile 输出文件
     * @return 合并后的分析结果
     */
    private fun mergeWithExistingResult(newResult: DetailedAnalysisResult, outputFile: File): DetailedAnalysisResult {
        // 如果文件不存在，直接返回新结果
        if (!outputFile.exists()) {
            println("[Pragma DDD] 输出文件不存在，创建新的分析结果")
            return newResult
        }
        
        return try {
            // 读取现有结果
            val existingResult = objectMapper.readValue(outputFile, DetailedAnalysisResult::class.java)
            println("[Pragma DDD] 读取到现有分析结果，包含 ${existingResult.classes.size} 个类")
            
            // 合并类信息（去重，以类名为键）
            val mergedClasses = mergeClassInfos(existingResult.classes, newResult.classes)
            println("[Pragma DDD] 合并后共有 ${mergedClasses.size} 个类")
            
            // 创建合并后的摘要
            val mergedSummary = createMergedSummary(mergedClasses)
            
            // 返回合并后的结果
            DetailedAnalysisResult(
                sourceSetName = newResult.sourceSetName, // 使用新结果的源集名称
                analysisTimestamp = System.currentTimeMillis(), // 更新时间戳
                classes = mergedClasses,
                summary = mergedSummary
            )
            
        } catch (e: Exception) {
            println("[Pragma DDD] 读取现有分析结果失败: ${e.message}，将使用新结果覆盖")
            newResult
        }
    }
    
    /**
     * 合并类信息列表，去重并保留最新的类信息
     * 
     * @param existingClasses 现有的类信息
     * @param newClasses 新的类信息
     * @return 合并后的类信息列表
     */
    private fun mergeClassInfos(
        existingClasses: List<DetailedClassInfo>, 
        newClasses: List<DetailedClassInfo>
    ): List<DetailedClassInfo> {
        // 使用Map来去重，以完整类名为键
        val classMap = mutableMapOf<String, DetailedClassInfo>()
        
        // 先添加现有的类
        existingClasses.forEach { classInfo ->
            classMap[classInfo.className] = classInfo
        }
        
        // 再添加新的类（会覆盖同名的现有类）
        newClasses.forEach { classInfo ->
            classMap[classInfo.className] = classInfo
        }
        
        return classMap.values.toList().sortedBy { it.className }
    }
    
    /**
     * 根据合并后的类信息创建分析摘要
     * 
     * @param classes 合并后的类信息列表
     * @return 分析摘要
     */
    private fun createMergedSummary(classes: List<DetailedClassInfo>): AnalysisSummary {
        return AnalysisSummary(
            totalClasses = classes.size,
            aggregateRootCount = classes.count { it.domainObjectType == org.morecup.pragmaddd.analyzer.model.DomainObjectType.AGGREGATE_ROOT },
            domainEntityCount = classes.count { it.domainObjectType == org.morecup.pragmaddd.analyzer.model.DomainObjectType.DOMAIN_ENTITY },
            valueObjectCount = classes.count { it.domainObjectType == org.morecup.pragmaddd.analyzer.model.DomainObjectType.VALUE_OBJECT },
            totalFields = classes.sumOf { it.fields.size },
            totalMethods = classes.sumOf { it.methods.size },
            classesWithDocumentation = classes.count { !it.documentation.isNullOrBlank() },
            fieldsWithDocumentation = classes.sumOf { classInfo ->
                classInfo.fields.count { !it.documentation.isNullOrBlank() }
            },
            methodsWithDocumentation = classes.sumOf { classInfo ->
                classInfo.methods.count { !it.documentation.isNullOrBlank() }
            }
        )
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
