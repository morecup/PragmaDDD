package org.morecup.pragmaddd.analyzer.integration

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.io.TempDir
import org.morecup.pragmaddd.analyzer.PragmaDddAnalyzerPlugin
import org.morecup.pragmaddd.analyzer.compiler.DddAnalysisIrGenerationExtension
import org.morecup.pragmaddd.analyzer.generator.JsonGeneratorImpl
import org.morecup.pragmaddd.analyzer.writer.ResourceWriterImpl
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File
import java.nio.file.Path

/**
 * End-to-end integration tests for test source analysis
 * Tests the complete workflow from test class analysis to JSON generation and JAR packaging
 */
class TestSourceEndToEndTest {
    
    @TempDir
    lateinit var tempDir: Path
    
    private lateinit var project: Project
    private lateinit var plugin: PragmaDddAnalyzerPlugin
    private lateinit var outputDir: File
    private val objectMapper = ObjectMapper().registerKotlinModule()
    
    @BeforeEach
    fun setup() {
        project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .build()
        
        plugin = PragmaDddAnalyzerPlugin()
        outputDir = tempDir.resolve("build/resources/main").toFile()
        outputDir.mkdirs()
    }
    
    @Test
    fun `complete workflow should generate separate JSON files for main and test sources`() {
        // Given
        plugin.apply(project)
        val extension = project.extensions.getByType(org.morecup.pragmaddd.analyzer.PragmaDddAnalyzerExtension::class.java)
        extension.outputDirectory.set(outputDir.absolutePath)
        extension.includeTestSources.set(true)
        extension.jsonFileNaming.set("test-analysis")
        
        // Create test source files in temp directory
        createTestSourceFiles()
        
        // When - simulate compilation and analysis
        val jsonGenerator = JsonGeneratorImpl()
        val resourceWriter = ResourceWriterImpl()
        
        // Create mock metadata for main and test sources
        val mainMetadata = listOf(
            createMockClassMetadata("com.example.User", "AGGREGATE_ROOT"),
            createMockClassMetadata("com.example.Order", "DOMAIN_ENTITY")
        )
        
        val testMetadata = listOf(
            createMockClassMetadata("com.example.TestAggregateRoot", "AGGREGATE_ROOT"),
            createMockClassMetadata("com.example.TestDomainEntity", "DOMAIN_ENTITY"),
            createMockClassMetadata("com.example.TestValueObject", "VALUE_OBJ")
        )
        
        // Generate JSON files
        val mainJson = jsonGenerator.generateMainSourcesJson(mainMetadata)
        val testJson = jsonGenerator.generateTestSourcesJson(testMetadata)
        
        resourceWriter.writeMainSourcesJson(mainJson, outputDir.absolutePath, "test-analysis-main.json")
        resourceWriter.writeTestSourcesJson(testJson, outputDir.absolutePath, "test-analysis-test.json")
        
        // Then - verify files are created
        val mainJsonFile = File(outputDir, "META-INF/ddd-analysis/test-analysis-main.json")
        val testJsonFile = File(outputDir, "META-INF/ddd-analysis/test-analysis-test.json")
        
        assertTrue(mainJsonFile.exists(), "Main sources JSON file should exist")
        assertTrue(testJsonFile.exists(), "Test sources JSON file should exist")
        
        // Verify JSON content
        val mainJsonContent = objectMapper.readTree(mainJsonFile)
        val testJsonContent = objectMapper.readTree(testJsonFile)
        
        assertEquals("main", mainJsonContent.get("sourceType").asText())
        assertEquals("test", testJsonContent.get("sourceType").asText())
        assertEquals(2, mainJsonContent.get("classes").size())
        assertEquals(3, testJsonContent.get("classes").size())
        
        // Verify specific test classes are included
        val testClasses = testJsonContent.get("classes")
        val testClassNames = testClasses.map { it.get("className").asText() }
        assertTrue(testClassNames.contains("com.example.TestAggregateRoot"))
        assertTrue(testClassNames.contains("com.example.TestDomainEntity"))
        assertTrue(testClassNames.contains("com.example.TestValueObject"))
    }
    
    @Test
    fun `test JAR packaging should include test metadata`() {
        // Given
        plugin.apply(project)
        val extension = project.extensions.getByType(org.morecup.pragmaddd.analyzer.PragmaDddAnalyzerExtension::class.java)
        extension.includeTestSources.set(true)
        extension.outputDirectory.set(outputDir.absolutePath)
        
        // Create test metadata file
        val resourceWriter = ResourceWriterImpl()
        val testJson = """
        {
            "generatedAt": "2024-01-15T10:30:00",
            "sourceType": "test",
            "classes": [
                {
                    "className": "com.example.TestAggregateRoot",
                    "packageName": "com.example",
                    "annotationType": "AGGREGATE_ROOT",
                    "properties": [],
                    "methods": [],
                    "documentation": null,
                    "annotations": []
                }
            ]
        }
        """.trimIndent()
        
        // When
        resourceWriter.writeTestSourcesJson(testJson, outputDir.absolutePath, "ddd-analysis-test.json")
        
        // Then
        val testJsonFile = File(outputDir, "META-INF/ddd-analysis/ddd-analysis-test.json")
        assertTrue(testJsonFile.exists())
        
        val jsonContent = objectMapper.readTree(testJsonFile)
        assertEquals("test", jsonContent.get("sourceType").asText())
        assertEquals("com.example.TestAggregateRoot", 
            jsonContent.get("classes").get(0).get("className").asText())
    }
    
    @Test
    fun `test source analysis should handle different DDD annotation types`() {
        // Given
        val jsonGenerator = JsonGeneratorImpl()
        val testMetadata = listOf(
            createMockClassMetadata("com.example.TestAggregate", "AGGREGATE_ROOT"),
            createMockClassMetadata("com.example.TestEntity", "DOMAIN_ENTITY"),
            createMockClassMetadata("com.example.TestValue", "VALUE_OBJ")
        )
        
        // When
        val testJson = jsonGenerator.generateTestSourcesJson(testMetadata)
        val jsonContent = objectMapper.readTree(testJson)
        
        // Then
        val classes = jsonContent.get("classes")
        assertEquals(3, classes.size())
        
        val annotationTypes = classes.map { it.get("annotationType").asText() }.toSet()
        assertTrue(annotationTypes.contains("AGGREGATE_ROOT"))
        assertTrue(annotationTypes.contains("DOMAIN_ENTITY"))
        assertTrue(annotationTypes.contains("VALUE_OBJ"))
    }
    
    @Test
    fun `test source analysis should preserve class structure metadata`() {
        // Given
        val jsonGenerator = JsonGeneratorImpl()
        val testMetadata = listOf(
            createMockClassMetadataWithDetails(
                className = "com.example.TestAggregateRoot",
                annotationType = "AGGREGATE_ROOT",
                propertyCount = 2,
                methodCount = 3
            )
        )
        
        // When
        val testJson = jsonGenerator.generateTestSourcesJson(testMetadata)
        val jsonContent = objectMapper.readTree(testJson)
        
        // Then
        val testClass = jsonContent.get("classes").get(0)
        assertEquals("com.example.TestAggregateRoot", testClass.get("className").asText())
        assertEquals("AGGREGATE_ROOT", testClass.get("annotationType").asText())
        assertEquals(2, testClass.get("properties").size())
        assertEquals(3, testClass.get("methods").size())
    }
    
    @Test
    fun `resource writer should verify test JSON files are written correctly`() {
        // Given
        val resourceWriter = ResourceWriterImpl()
        val testJson = """{"sourceType": "test", "classes": []}"""
        
        // When
        resourceWriter.writeTestSourcesJson(testJson, outputDir.absolutePath, "verification-test.json")
        
        // Then
        assertTrue(resourceWriter.verifyJsonFileWritten(outputDir.absolutePath, "ddd-analysis/verification-test.json"))
        
        val jsonFiles = resourceWriter.listJsonFiles(outputDir.absolutePath)
        assertTrue(jsonFiles.any { it.name == "verification-test.json" })
    }
    
    @Test
    fun `test compilation should be skipped when includeTestSources is false`() {
        // Given
        plugin.apply(project)
        val extension = project.extensions.getByType(org.morecup.pragmaddd.analyzer.PragmaDddAnalyzerExtension::class.java)
        extension.includeTestSources.set(false)
        
        // When
        val testCompilation = createMockCompilation("test")
        val options = plugin.applyToCompilation(testCompilation).get()
        
        // Then
        assertTrue(options.isEmpty(), "Test compilation should be skipped when includeTestSources is false")
    }
    
    /**
     * Creates test source files in the temp directory
     */
    private fun createTestSourceFiles() {
        val testSourceDir = tempDir.resolve("src/test/kotlin/com/example").toFile()
        testSourceDir.mkdirs()
        
        // Create TestAggregateRoot.kt
        File(testSourceDir, "TestAggregateRoot.kt").writeText("""
            package com.example
            
            import org.morecup.pragmaddd.core.annotation.AggregateRoot
            
            @AggregateRoot
            class TestAggregateRoot(private val id: String)
        """.trimIndent())
        
        // Create TestDomainEntity.kt
        File(testSourceDir, "TestDomainEntity.kt").writeText("""
            package com.example
            
            import org.morecup.pragmaddd.core.annotation.DomainEntity
            
            @DomainEntity
            class TestDomainEntity(private val entityId: Long)
        """.trimIndent())
        
        // Create TestValueObject.kt
        File(testSourceDir, "TestValueObject.kt").writeText("""
            package com.example
            
            import org.morecup.pragmaddd.core.annotation.ValueObj
            
            @ValueObj
            data class TestValueObject(val value: String)
        """.trimIndent())
    }
    
    /**
     * Helper method to create mock Kotlin compilation
     */
    private fun createMockCompilation(name: String): org.jetbrains.kotlin.gradle.plugin.KotlinCompilation<*> {
        val compilation = org.mockito.kotlin.mock<org.jetbrains.kotlin.gradle.plugin.KotlinCompilation<*>>()
        val target = org.mockito.kotlin.mock<org.jetbrains.kotlin.gradle.plugin.KotlinTarget>()
        
        org.mockito.kotlin.whenever(compilation.name).thenReturn(name)
        org.mockito.kotlin.whenever(compilation.target).thenReturn(target)
        org.mockito.kotlin.whenever(target.project).thenReturn(project)
        
        return compilation
    }
    
    /**
     * Helper method to create mock class metadata
     */
    private fun createMockClassMetadata(className: String, annotationType: String): org.morecup.pragmaddd.analyzer.model.ClassMetadata {
        return org.morecup.pragmaddd.analyzer.model.ClassMetadata(
            className = className,
            packageName = "com.example",
            annotationType = org.morecup.pragmaddd.analyzer.model.DddAnnotationType.valueOf(annotationType),
            properties = emptyList(),
            methods = emptyList(),
            documentation = null,
            annotations = emptyList()
        )
    }
    
    /**
     * Helper method to create mock class metadata with detailed structure
     */
    private fun createMockClassMetadataWithDetails(
        className: String, 
        annotationType: String,
        propertyCount: Int,
        methodCount: Int
    ): org.morecup.pragmaddd.analyzer.model.ClassMetadata {
        val properties = (1..propertyCount).map { i ->
            org.morecup.pragmaddd.analyzer.model.PropertyMetadata(
                name = "property$i",
                type = "String",
                isPrivate = true,
                isMutable = false,
                documentation = null,
                annotations = emptyList()
            )
        }
        
        val methods = (1..methodCount).map { i ->
            org.morecup.pragmaddd.analyzer.model.MethodMetadata(
                name = "method$i",
                parameters = emptyList(),
                returnType = "Unit",
                isPrivate = false,
                methodCalls = emptyList(),
                propertyAccesses = emptyList(),
                documentation = null,
                annotations = emptyList()
            )
        }
        
        return org.morecup.pragmaddd.analyzer.model.ClassMetadata(
            className = className,
            packageName = "com.example",
            annotationType = org.morecup.pragmaddd.analyzer.model.DddAnnotationType.valueOf(annotationType),
            properties = properties,
            methods = methods,
            documentation = null,
            annotations = emptyList()
        )
    }
}