package org.morecup.pragmaddd.analyzer.standalone

import org.morecup.pragmaddd.analyzer.generator.JsonGeneratorImpl
import org.morecup.pragmaddd.analyzer.model.*
import java.io.File

/**
 * 独立分析器 - 用于演示和测试 JSON 生成功能
 */
class StandaloneAnalyzer {
    
    private val jsonGenerator = JsonGeneratorImpl()
    
    /**
     * 分析示例类并生成 JSON
     */
    fun analyzeExampleClasses(): String {
        val exampleClasses = createExampleMetadata()
        return jsonGenerator.generateMainSourcesJson(exampleClasses)
    }
    
    /**
     * 创建示例元数据
     */
    private fun createExampleMetadata(): List<ClassMetadata> {
        return listOf(
            ClassMetadata(
                className = "User",
                packageName = "com.example.demo.domain",
                annotationType = DddAnnotationType.AGGREGATE_ROOT,
                properties = listOf(
                    PropertyMetadata(
                        name = "id",
                        type = "String",
                        isPrivate = true,
                        isMutable = false,
                        documentation = DocumentationMetadata(
                            summary = "User unique identifier",
                            description = null,
                            parameters = emptyMap(),
                            returnDescription = null
                        ),
                        annotations = emptyList()
                    ),
                    PropertyMetadata(
                        name = "name",
                        type = "String",
                        isPrivate = true,
                        isMutable = true,
                        documentation = null,
                        annotations = emptyList()
                    ),
                    PropertyMetadata(
                        name = "email",
                        type = "String",
                        isPrivate = true,
                        isMutable = true,
                        documentation = null,
                        annotations = emptyList()
                    )
                ),
                methods = listOf(
                    MethodMetadata(
                        name = "updateProfile",
                        parameters = listOf(
                            ParameterMetadata(
                                name = "newName",
                                type = "String",
                                annotations = emptyList()
                            ),
                            ParameterMetadata(
                                name = "newEmail",
                                type = "String",
                                annotations = emptyList()
                            )
                        ),
                        returnType = "Unit",
                        isPrivate = false,
                        methodCalls = listOf(
                            MethodCallMetadata(
                                targetMethod = "isNotBlank",
                                receiverType = "String",
                                parameters = emptyList()
                            )
                        ),
                        propertyAccesses = listOf(
                            PropertyAccessMetadata(
                                propertyName = "name",
                                accessType = PropertyAccessType.GET,
                                ownerClass = "com.example.demo.domain.User"
                            ),
                            PropertyAccessMetadata(
                                propertyName = "name",
                                accessType = PropertyAccessType.SET,
                                ownerClass = "com.example.demo.domain.User"
                            ),
                            PropertyAccessMetadata(
                                propertyName = "email",
                                accessType = PropertyAccessType.SET,
                                ownerClass = "com.example.demo.domain.User"
                            )
                        ),
                        documentation = DocumentationMetadata(
                            summary = "Update user profile information",
                            description = "Accesses and modifies multiple properties",
                            parameters = mapOf(
                                "newName" to "New user name",
                                "newEmail" to "New email address"
                            ),
                            returnDescription = null
                        ),
                        annotations = emptyList()
                    )
                ),
                documentation = DocumentationMetadata(
                    summary = "User aggregate root example",
                    description = null,
                    parameters = emptyMap(),
                    returnDescription = null
                ),
                annotations = listOf(
                    AnnotationMetadata(
                        name = "AggregateRoot",
                        parameters = emptyMap()
                    )
                )
            )
        )
    }
    
    /**
     * 生成并保存 JSON 文件
     */
    fun generateJsonFile(outputPath: String = "build/generated/ddd-analysis/example-analysis.json") {
        val json = analyzeExampleClasses()
        jsonGenerator.writeToFile(json, outputPath)
        println("Generated DDD analysis JSON at: $outputPath")
    }
}

/**
 * 主函数 - 用于测试
 */
fun main() {
    val analyzer = StandaloneAnalyzer()
    analyzer.generateJsonFile()
    
    // 输出 JSON 内容到控制台
    println("\nGenerated JSON:")
    println(analyzer.analyzeExampleClasses())
}