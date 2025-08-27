package org.morecup.pragmaddd.analyzer.repository

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.File

/**
 * Repository分析结果写入器
 * 生成符合要求的JSON格式分析结果
 */
class RepositoryAnalysisResultWriter {
    
    private val objectMapper: ObjectMapper = jacksonObjectMapper().apply {
        configure(SerializationFeature.INDENT_OUTPUT, true)
        setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
    }
    
    /**
     * 写入分析结果到JSON文件
     */
    fun writeToFile(result: RepositoryAnalysisResult, outputFile: File) {
        val jsonResult = convertToJsonFormat(result)
        outputFile.parentFile?.mkdirs()
        objectMapper.writeValue(outputFile, jsonResult)
    }
    
    /**
     * 转换为目标JSON格式
     */
    private fun convertToJsonFormat(result: RepositoryAnalysisResult): JsonRepositoryAnalysisResult {
        val analysisMap = mutableMapOf<String, JsonClassAnalysis>()
        
        result.callAnalysis.forEach { callInfo ->
            val className = callInfo.methodClass
            val classAnalysis = analysisMap.getOrPut(className) {
                JsonClassAnalysis(
                    className = className,
                    methods = mutableMapOf()
                )
            }
            
            callInfo.repositoryCalls.forEach { repoCall ->
                val methodKey = "${callInfo.method}${callInfo.methodDescriptor}"
                val methodAnalysis = classAnalysis.methods.getOrPut(methodKey) {
                    JsonMethodAnalysis(
                        methodClass = callInfo.methodClass,
                        method = callInfo.method,
                        methodDescriptor = callInfo.methodDescriptor,
                        repositoryCalls = mutableListOf()
                    )
                }
                
                val jsonRepoCall = JsonRepositoryCall(
                    repository = repoCall.repository,
                    repositoryMethod = repoCall.repositoryMethod,
                    repositoryMethodDescriptor = repoCall.repositoryMethodDescriptor,
                    aggregateRoot = repoCall.aggregateRoot,
                    calledAggregateRootMethod = repoCall.calledAggregateRootMethod.map { methodCall ->
                        JsonAggregateRootMethodCall(
                            aggregateRootMethod = methodCall.aggregateRootMethod,
                            aggregateRootMethodDescriptor = methodCall.aggregateRootMethodDescriptor,
                            requiredFields = methodCall.requiredFields
                        )
                    },
                    requiredFields = repoCall.requiredFields
                )
                
                methodAnalysis.repositoryCalls.add(jsonRepoCall)
            }
        }
        
        return JsonRepositoryAnalysisResult(
            version = "1.0",
            timestamp = System.currentTimeMillis(),
            summary = JsonAnalysisSummary(
                totalClasses = analysisMap.size,
                totalMethods = analysisMap.values.sumOf { it.methods.size },
                totalRepositoryCalls = result.callAnalysis.sumOf { it.repositoryCalls.size },
                aggregateRoots = result.aggregateRoots.map { 
                    JsonAggregateRootSummary(it.className, it.fieldNames) 
                },
                repositories = result.repositories.map {
                    JsonRepositorySummary(it.className, it.repositoryType.name, it.targetAggregateRoot)
                }
            ),
            analysis = analysisMap
        )
    }
}

/**
 * JSON输出格式数据模型
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class JsonRepositoryAnalysisResult(
    val version: String,
    val timestamp: Long,
    val summary: JsonAnalysisSummary,
    val analysis: Map<String, JsonClassAnalysis>
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class JsonAnalysisSummary(
    val totalClasses: Int,
    val totalMethods: Int,
    val totalRepositoryCalls: Int,
    val aggregateRoots: List<JsonAggregateRootSummary>,
    val repositories: List<JsonRepositorySummary>
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class JsonAggregateRootSummary(
    val className: String,
    val fieldNames: List<String>
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class JsonRepositorySummary(
    val className: String,
    val repositoryType: String,
    val targetAggregateRoot: String?
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class JsonClassAnalysis(
    val className: String,
    val methods: MutableMap<String, JsonMethodAnalysis>
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class JsonMethodAnalysis(
    val methodClass: String,
    val method: String,
    val methodDescriptor: String,
    val repositoryCalls: MutableList<JsonRepositoryCall>
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class JsonRepositoryCall(
    val repository: String,
    val repositoryMethod: String,
    val repositoryMethodDescriptor: String,
    val aggregateRoot: String?,
    val calledAggregateRootMethod: List<JsonAggregateRootMethodCall>,
    val requiredFields: List<String>
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class JsonAggregateRootMethodCall(
    val aggregateRootMethod: String,
    val aggregateRootMethodDescriptor: String,
    val requiredFields: List<String>
)