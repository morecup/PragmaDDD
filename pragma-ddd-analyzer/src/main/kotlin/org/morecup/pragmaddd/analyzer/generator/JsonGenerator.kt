package org.morecup.pragmaddd.analyzer.generator

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.morecup.pragmaddd.analyzer.model.AnalysisResult
import org.morecup.pragmaddd.analyzer.model.ClassMetadata
import org.morecup.pragmaddd.analyzer.error.AnalysisError
import org.morecup.pragmaddd.analyzer.error.ErrorReporter
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * JSON 生成器接口 - 定义 JSON 生成和序列化的契约
 */
interface JsonGenerator {
    /**
     * 生成主源码的 JSON
     */
    fun generateMainSourcesJson(metadata: List<ClassMetadata>): String
    
    /**
     * 生成测试源码的 JSON
     */
    fun generateTestSourcesJson(metadata: List<ClassMetadata>): String
    
    /**
     * 将 JSON 写入文件
     */
    fun writeToFile(json: String, outputPath: String)
    
    /**
     * 生成并写入主源码 JSON 文件
     */
    fun generateAndWriteMainSourcesJson(
        metadata: List<ClassMetadata>,
        outputDirectory: String,
        fileName: String = "ddd-analysis-main.json"
    )
    
    /**
     * 生成并写入测试源码 JSON 文件
     */
    fun generateAndWriteTestSourcesJson(
        metadata: List<ClassMetadata>,
        outputDirectory: String,
        fileName: String = "ddd-analysis-test.json"
    )
    
    /**
     * 验证 JSON 格式
     */
    fun validateJson(json: String): Boolean
}

/**
 * JSON 生成器实现 - 使用 Jackson 生成分析结果的 JSON 文件
 */
class JsonGeneratorImpl(
    private val errorReporter: ErrorReporter? = null
) : JsonGenerator {
    
    private val objectMapper = ObjectMapper().apply {
        registerKotlinModule()
        enable(SerializationFeature.INDENT_OUTPUT)
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
    }
    
    /**
     * 配置 JSON 格式化选项的构造函数
     */
    constructor(
        errorReporter: ErrorReporter? = null,
        prettyPrint: Boolean = true, 
        orderMapKeys: Boolean = true
    ) : this(errorReporter) {
        if (prettyPrint) {
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT)
        } else {
            objectMapper.disable(SerializationFeature.INDENT_OUTPUT)
        }
        
        if (orderMapKeys) {
            objectMapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
        } else {
            objectMapper.disable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
        }
    }
    
    /**
     * 生成主源码的 JSON
     */
    override fun generateMainSourcesJson(metadata: List<ClassMetadata>): String {
        return try {
            val result = AnalysisResult(
                generatedAt = getCurrentTimestamp(),
                sourceType = "main",
                classes = metadata
            )
            objectMapper.writeValueAsString(result)
        } catch (e: Exception) {
            errorReporter?.reportError(
                AnalysisError.JsonGenerationError(
                    message = "Failed to generate main sources JSON: ${e.message}",
                    cause = e
                )
            )
            throw e
        }
    }
    
    /**
     * 生成测试源码的 JSON
     */
    override fun generateTestSourcesJson(metadata: List<ClassMetadata>): String {
        return try {
            val result = AnalysisResult(
                generatedAt = getCurrentTimestamp(),
                sourceType = "test",
                classes = metadata
            )
            objectMapper.writeValueAsString(result)
        } catch (e: Exception) {
            errorReporter?.reportError(
                AnalysisError.JsonGenerationError(
                    message = "Failed to generate test sources JSON: ${e.message}",
                    cause = e
                )
            )
            throw e
        }
    }
    
    /**
     * 将 JSON 写入文件
     */
    override fun writeToFile(json: String, outputPath: String) {
        try {
            val file = File(outputPath)
            
            // 确保父目录存在
            file.parentFile?.mkdirs()
            
            file.writeText(json)
        } catch (e: Exception) {
            errorReporter?.reportError(
                AnalysisError.OutputGenerationError(
                    outputPath = outputPath,
                    message = "Failed to write JSON to file: ${e.message}",
                    cause = e
                )
            )
            throw e
        }
    }
    
    /**
     * 生成并写入主源码 JSON 文件
     */
    override fun generateAndWriteMainSourcesJson(
        metadata: List<ClassMetadata>,
        outputDirectory: String,
        fileName: String
    ) {
        try {
            if (metadata.isNotEmpty()) {
                val json = generateMainSourcesJson(metadata)
                val outputPath = File(outputDirectory, fileName).absolutePath
                writeToFile(json, outputPath)
            }
        } catch (e: Exception) {
            errorReporter?.reportError(
                AnalysisError.OutputGenerationError(
                    outputPath = File(outputDirectory, fileName).absolutePath,
                    message = "Failed to generate and write main sources JSON: ${e.message}",
                    cause = e
                )
            )
            // Re-throw to prevent silent failures
            throw e
        }
    }
    
    /**
     * 生成并写入测试源码 JSON 文件
     */
    override fun generateAndWriteTestSourcesJson(
        metadata: List<ClassMetadata>,
        outputDirectory: String,
        fileName: String
    ) {
        try {
            if (metadata.isNotEmpty()) {
                val json = generateTestSourcesJson(metadata)
                val outputPath = File(outputDirectory, fileName).absolutePath
                writeToFile(json, outputPath)
            }
        } catch (e: Exception) {
            errorReporter?.reportError(
                AnalysisError.OutputGenerationError(
                    outputPath = File(outputDirectory, fileName).absolutePath,
                    message = "Failed to generate and write test sources JSON: ${e.message}",
                    cause = e
                )
            )
            // Re-throw to prevent silent failures
            throw e
        }
    }
    
    /**
     * 获取当前时间戳
     */
    private fun getCurrentTimestamp(): String {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }
    
    /**
     * 验证 JSON 格式
     */
    override fun validateJson(json: String): Boolean {
        return try {
            objectMapper.readTree(json)
            true
        } catch (e: Exception) {
            errorReporter?.reportError(
                AnalysisError.JsonGenerationError(
                    message = "JSON validation failed: ${e.message}",
                    cause = e
                )
            )
            false
        }
    }
    
    /**
     * 验证 JSON 是否符合 AnalysisResult 结构
     */
    fun validateAnalysisResultJson(json: String): Boolean {
        return try {
            objectMapper.readValue(json, AnalysisResult::class.java)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 生成 JSON Schema 描述
     */
    fun generateJsonSchema(): String {
        val schema = mapOf(
            "\$schema" to "http://json-schema.org/draft-07/schema#",
            "title" to "DDD Analysis Result",
            "type" to "object",
            "properties" to mapOf(
                "generatedAt" to mapOf(
                    "type" to "string",
                    "format" to "date-time",
                    "description" to "Timestamp when the analysis was generated"
                ),
                "sourceType" to mapOf(
                    "type" to "string",
                    "enum" to listOf("main", "test"),
                    "description" to "Type of source code analyzed"
                ),
                "classes" to mapOf(
                    "type" to "array",
                    "items" to mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "className" to mapOf("type" to "string"),
                            "packageName" to mapOf("type" to "string"),
                            "annotationType" to mapOf(
                                "type" to "string",
                                "enum" to listOf("AGGREGATE_ROOT", "DOMAIN_ENTITY", "VALUE_OBJ")
                            ),
                            "properties" to mapOf("type" to "array"),
                            "methods" to mapOf("type" to "array"),
                            "documentation" to mapOf("type" to "object"),
                            "annotations" to mapOf("type" to "array")
                        ),
                        "required" to listOf("className", "packageName", "annotationType", "properties", "methods", "annotations")
                    )
                )
            ),
            "required" to listOf("generatedAt", "sourceType", "classes")
        )
        
        return objectMapper.writeValueAsString(schema)
    }
}