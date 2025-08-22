package org.morecup.pragmaddd.analyzer.generator

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.*
import org.morecup.pragmaddd.analyzer.error.ErrorReporter
import org.morecup.pragmaddd.analyzer.model.*
import java.io.File

class JsonGeneratorTest {
    
    @TempDir
    lateinit var tempDir: File
    
    private lateinit var errorReporter: ErrorReporter
    private lateinit var jsonGenerator: JsonGenerator
    private lateinit var objectMapper: ObjectMapper
    
    @BeforeEach
    fun setUp() {
        errorReporter = mock()
        jsonGenerator = JsonGeneratorImpl(errorReporter)
        objectMapper = ObjectMapper().registerKotlinModule()
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
    fun `should generate empty JSON for empty metadata list`() {
        // Given
        val emptyMetadata = emptyList<ClassMetadata>()

        // When
        val mainJson = jsonGenerator.generateMainSourcesJson(emptyMetadata)

        // Then
        assertTrue(jsonGenerator.validateJson(mainJson))
        
        val jsonNode = objectMapper.readTree(mainJson)
        assertEquals(0, jsonNode.get("classes").size())
    }

    @Test
    fun `should write JSON to file successfully`() {
        // Given
        val json = """{"test": "data"}"""
        val outputPath = File(tempDir, "test.json").absolutePath

        // When
        jsonGenerator.writeToFile(json, outputPath)

        // Then
        val file = File(outputPath)
        assertTrue(file.exists())
        assertEquals(json, file.readText())
    }

    @Test
    fun `should handle file write errors gracefully`() {
        // Given
        val json = """{"test": "data"}"""
        // Use a path with invalid characters that will definitely fail on Windows
        val invalidPath = "C:\\invalid<>path\\test.json"

        // When & Then
        assertDoesNotThrow {
            jsonGenerator.writeToFile(json, invalidPath)
        }
        
        verify(errorReporter).reportError(any())
    }

    @Test
    fun `should validate valid JSON`() {
        // Given
        val validJson = """{"key": "value", "number": 123}"""

        // When
        val isValid = jsonGenerator.validateJson(validJson)

        // Then
        assertTrue(isValid)
    }

    @Test
    fun `should reject invalid JSON`() {
        // Given
        val invalidJson = """{"key": "value", "number":}"""

        // When
        val isValid = jsonGenerator.validateJson(invalidJson)

        // Then
        assertFalse(isValid)
        verify(errorReporter).reportError(any())
    }

    @Test
    fun `should parse main sources JSON correctly`() {
        // Given
        val metadata = listOf(createSampleClassMetadata())
        val json = jsonGenerator.generateMainSourcesJson(metadata)

        // When
        val parsedMetadata = jsonGenerator.parseMainSourcesJson(json)

        // Then
        assertEquals(1, parsedMetadata.size)
        assertEquals(metadata[0].className, parsedMetadata[0].className)
        assertEquals(metadata[0].packageName, parsedMetadata[0].packageName)
    }

    @Test
    fun `should handle JSON parsing errors`() {
        // Given
        val invalidJson = """{"invalid": "structure"}"""

        // When
        val parsedMetadata = jsonGenerator.parseMainSourcesJson(invalidJson)

        // Then
        assertTrue(parsedMetadata.isEmpty())
        verify(errorReporter).reportError(any())
    }

    @Test
    fun `generateAndWriteMainSourcesJson should create file with correct content`() {
        // Given
        val metadata = listOf(createSampleClassMetadata())
        val outputDirectory = tempDir.absolutePath
        val fileName = "test-main.json"

        // When
        jsonGenerator.generateAndWriteMainSourcesJson(metadata, outputDirectory, fileName)

        // Then
        val file = File(tempDir, fileName)
        assertTrue(file.exists())
        
        val content = file.readText()
        assertTrue(jsonGenerator.validateJson(content))
        
        val jsonNode = objectMapper.readTree(content)
        assertEquals("main", jsonNode.get("sourceType").asText())
    }

    @Test
    fun `generateAndWriteMainSourcesJson should skip empty metadata`() {
        // Given
        val emptyMetadata = emptyList<ClassMetadata>()
        val outputDirectory = tempDir.absolutePath
        val fileName = "empty-main.json"

        // When
        jsonGenerator.generateAndWriteMainSourcesJson(emptyMetadata, outputDirectory, fileName)

        // Then
        val file = File(tempDir, fileName)
        assertFalse(file.exists()) // Should not create file for empty metadata
    }

    private fun createSampleClassMetadata(): ClassMetadata {
        return ClassMetadata(
            className = "TestClass",
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
                    propertyAccesses = emptyList(),
                    documentation = null,
                    annotations = emptyList()
                )
            ),
            documentation = null,
            annotations = emptyList()
        )
    }
}
