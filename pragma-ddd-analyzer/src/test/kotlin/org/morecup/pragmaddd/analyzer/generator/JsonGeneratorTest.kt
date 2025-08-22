package org.morecup.pragmaddd.analyzer.generator

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.morecup.pragmaddd.analyzer.model.*
import java.io.File
import java.nio.file.Path

class JsonGeneratorTest {

    private lateinit var jsonGenerator: JsonGenerator
    private lateinit var objectMapper: ObjectMapper
    
    @TempDir
    lateinit var tempDir: Path

    @BeforeEach
    fun setUp() {
        jsonGenerator = JsonGeneratorImpl()
        objectMapper = ObjectMapper().apply {
            registerKotlinModule()
        }
    }

    @Test
    fun `should generate main sources JSON with correct structure`() {
        // Given
        val metadata = listOf(createSampleClassMetadata())

        // When
        val json = jsonGenerator.generateMainSourcesJson(metadata)

        // Then
        assertNotNull(json)
        assertTrue(jsonGenerator.validateJson(json))
        
        val jsonNode = objectMapper.readTree(json)
        assertEquals("main", jsonNode.get("sourceType").asText())
        assertTrue(jsonNode.has("generatedAt"))
        assertTrue(jsonNode.has("classes"))
        assertEquals(1, jsonNode.get("classes").size())
    }

    @Test
    fun `should generate test sources JSON with correct structure`() {
        // Given
        val metadata = listOf(createSampleClassMetadata())

        // When
        val json = jsonGenerator.generateTestSourcesJson(metadata)

        // Then
        assertNotNull(json)
        assertTrue(jsonGenerator.validateJson(json))
        
        val jsonNode = objectMapper.readTree(json)
        assertEquals("test", jsonNode.get("sourceType").asText())
        assertTrue(jsonNode.has("generatedAt"))
        assertTrue(jsonNode.has("classes"))
        assertEquals(1, jsonNode.get("classes").size())
    }

    @Test
    fun `should generate empty JSON for empty metadata list`() {
        // Given
        val emptyMetadata = emptyList<ClassMetadata>()

        // When
        val mainJson = jsonGenerator.generateMainSourcesJson(emptyMetadata)
        val testJson = jsonGenerator.generateTestSourcesJson(emptyMetadata)

        // Then
        assertTrue(jsonGenerator.validateJson(mainJson))
        assertTrue(jsonGenerator.validateJson(testJson))
        
        val mainNode = objectMapper.readTree(mainJson)
        val testNode = objectMapper.readTree(testJson)
        
        assertEquals(0, mainNode.get("classes").size())
        assertEquals(0, testNode.get("classes").size())
    }

    @Test
    fun `should write JSON to file successfully`() {
        // Given
        val json = """{"test": "data"}"""
        val outputPath = tempDir.resolve("test-output.json").toString()

        // When
        jsonGenerator.writeToFile(json, outputPath)

        // Then
        val file = File(outputPath)
        assertTrue(file.exists())
        assertEquals(json, file.readText())
    }

    @Test
    fun `should create parent directories when writing to file`() {
        // Given
        val json = """{"test": "data"}"""
        val outputPath = tempDir.resolve("nested/directory/test-output.json").toString()

        // When
        jsonGenerator.writeToFile(json, outputPath)

        // Then
        val file = File(outputPath)
        assertTrue(file.exists())
        assertTrue(file.parentFile.exists())
        assertEquals(json, file.readText())
    }

    @Test
    fun `should generate and write main sources JSON file`() {
        // Given
        val metadata = listOf(createSampleClassMetadata())
        val outputDirectory = tempDir.toString()
        val fileName = "custom-main.json"

        // When
        jsonGenerator.generateAndWriteMainSourcesJson(metadata, outputDirectory, fileName)

        // Then
        val file = File(outputDirectory, fileName)
        assertTrue(file.exists())
        
        val json = file.readText()
        assertTrue(jsonGenerator.validateJson(json))
        
        val jsonNode = objectMapper.readTree(json)
        assertEquals("main", jsonNode.get("sourceType").asText())
    }

    @Test
    fun `should generate and write test sources JSON file`() {
        // Given
        val metadata = listOf(createSampleClassMetadata())
        val outputDirectory = tempDir.toString()
        val fileName = "custom-test.json"

        // When
        jsonGenerator.generateAndWriteTestSourcesJson(metadata, outputDirectory, fileName)

        // Then
        val file = File(outputDirectory, fileName)
        assertTrue(file.exists())
        
        val json = file.readText()
        assertTrue(jsonGenerator.validateJson(json))
        
        val jsonNode = objectMapper.readTree(json)
        assertEquals("test", jsonNode.get("sourceType").asText())
    }

    @Test
    fun `should not create file for empty metadata`() {
        // Given
        val emptyMetadata = emptyList<ClassMetadata>()
        val outputDirectory = tempDir.toString()
        val fileName = "empty-main.json"

        // When
        jsonGenerator.generateAndWriteMainSourcesJson(emptyMetadata, outputDirectory, fileName)

        // Then
        val file = File(outputDirectory, fileName)
        assertFalse(file.exists())
    }

    @Test
    fun `should validate correct JSON format`() {
        // Given
        val validJson = """{"key": "value", "number": 123}"""
        val invalidJson = """{"key": "value", "number":}"""

        // When & Then
        assertTrue(jsonGenerator.validateJson(validJson))
        assertFalse(jsonGenerator.validateJson(invalidJson))
    }

    @Test
    fun `should validate AnalysisResult JSON structure`() {
        // Given
        val generator = jsonGenerator as JsonGeneratorImpl
        val metadata = listOf(createSampleClassMetadata())
        val validJson = jsonGenerator.generateMainSourcesJson(metadata)
        val invalidJson = """{"invalid": "structure"}"""

        // When & Then
        assertTrue(generator.validateAnalysisResultJson(validJson))
        assertFalse(generator.validateAnalysisResultJson(invalidJson))
    }

    @Test
    fun `should generate JSON schema`() {
        // Given
        val generator = jsonGenerator as JsonGeneratorImpl

        // When
        val schema = generator.generateJsonSchema()

        // Then
        assertNotNull(schema)
        assertTrue(jsonGenerator.validateJson(schema))
        
        val schemaNode = objectMapper.readTree(schema)
        assertEquals("http://json-schema.org/draft-07/schema#", schemaNode.get("\$schema").asText())
        assertEquals("DDD Analysis Result", schemaNode.get("title").asText())
        assertTrue(schemaNode.has("properties"))
        assertTrue(schemaNode.has("required"))
    }

    @Test
    fun `should handle complex metadata with all fields populated`() {
        // Given
        val complexMetadata = listOf(createComplexClassMetadata())

        // When
        val json = jsonGenerator.generateMainSourcesJson(complexMetadata)

        // Then
        assertTrue(jsonGenerator.validateJson(json))
        
        val jsonNode = objectMapper.readTree(json)
        val classNode = jsonNode.get("classes").get(0)
        
        assertEquals("com.example.ComplexClass", classNode.get("className").asText())
        assertEquals("com.example", classNode.get("packageName").asText())
        assertEquals("AGGREGATE_ROOT", classNode.get("annotationType").asText())
        assertTrue(classNode.get("properties").size() > 0)
        assertTrue(classNode.get("methods").size() > 0)
        assertNotNull(classNode.get("documentation"))
        assertTrue(classNode.get("annotations").size() > 0)
    }

    @Test
    fun `should format JSON with pretty printing by default`() {
        // Given
        val metadata = listOf(createSampleClassMetadata())

        // When
        val json = jsonGenerator.generateMainSourcesJson(metadata)

        // Then
        assertTrue(json.contains("\n"))
        assertTrue(json.contains("  ")) // Should contain indentation
    }

    @Test
    fun `should create JsonGenerator with custom formatting options`() {
        // Given
        val prettyPrintGenerator = JsonGeneratorImpl(prettyPrint = true, orderMapKeys = true)
        val compactGenerator = JsonGeneratorImpl(prettyPrint = false, orderMapKeys = false)
        val metadata = listOf(createSampleClassMetadata())

        // When
        val prettyJson = prettyPrintGenerator.generateMainSourcesJson(metadata)
        val compactJson = compactGenerator.generateMainSourcesJson(metadata)

        // Then
        assertTrue(prettyJson.contains("\n"))
        assertFalse(compactJson.contains("\n"))
        assertTrue(prettyPrintGenerator.validateJson(prettyJson))
        assertTrue(compactGenerator.validateJson(compactJson))
    }

    private fun createSampleClassMetadata(): ClassMetadata {
        return ClassMetadata(
            className = "com.example.SampleClass",
            packageName = "com.example",
            annotationType = DddAnnotationType.AGGREGATE_ROOT,
            properties = listOf(
                PropertyMetadata(
                    name = "id",
                    type = "String",
                    isPrivate = true,
                    isMutable = false,
                    documentation = null,
                    annotations = emptyList()
                )
            ),
            methods = listOf(
                MethodMetadata(
                    name = "getId",
                    parameters = emptyList(),
                    returnType = "String",
                    isPrivate = false,
                    methodCalls = emptyList(),
                    propertyAccesses = listOf(
                        PropertyAccessMetadata(
                            propertyName = "id",
                            accessType = PropertyAccessType.READ,
                            ownerClass = "com.example.SampleClass"
                        )
                    ),
                    documentation = null,
                    annotations = emptyList()
                )
            ),
            documentation = null,
            annotations = listOf(
                AnnotationMetadata(
                    name = "AggregateRoot",
                    parameters = emptyMap()
                )
            )
        )
    }

    private fun createComplexClassMetadata(): ClassMetadata {
        return ClassMetadata(
            className = "com.example.ComplexClass",
            packageName = "com.example",
            annotationType = DddAnnotationType.AGGREGATE_ROOT,
            properties = listOf(
                PropertyMetadata(
                    name = "id",
                    type = "String",
                    isPrivate = true,
                    isMutable = false,
                    documentation = DocumentationMetadata(
                        summary = "Unique identifier",
                        description = "The unique identifier for this entity",
                        parameters = emptyMap(),
                        returnDescription = null
                    ),
                    annotations = listOf(
                        AnnotationMetadata(
                            name = "Id",
                            parameters = emptyMap()
                        )
                    )
                ),
                PropertyMetadata(
                    name = "name",
                    type = "String",
                    isPrivate = false,
                    isMutable = true,
                    documentation = null,
                    annotations = emptyList()
                )
            ),
            methods = listOf(
                MethodMetadata(
                    name = "updateName",
                    parameters = listOf(
                        ParameterMetadata(
                            name = "newName",
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
                            accessType = PropertyAccessType.WRITE,
                            ownerClass = "com.example.ComplexClass"
                        )
                    ),
                    documentation = DocumentationMetadata(
                        summary = "Updates the name",
                        description = "Updates the name of this entity with validation",
                        parameters = mapOf("newName" to "The new name to set"),
                        returnDescription = null
                    ),
                    annotations = emptyList()
                )
            ),
            documentation = DocumentationMetadata(
                summary = "Complex class example",
                description = "A complex class demonstrating all metadata features",
                parameters = emptyMap(),
                returnDescription = null
            ),
            annotations = listOf(
                AnnotationMetadata(
                    name = "AggregateRoot",
                    parameters = mapOf("value" to "complex")
                )
            )
        )
    }
}