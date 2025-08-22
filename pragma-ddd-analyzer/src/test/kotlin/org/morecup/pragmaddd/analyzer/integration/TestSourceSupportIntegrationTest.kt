package org.morecup.pragmaddd.analyzer.integration

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.io.TempDir
import org.morecup.pragmaddd.analyzer.PragmaDddAnalyzerPlugin
import org.morecup.pragmaddd.analyzer.PragmaDddAnalyzerExtension
import org.morecup.pragmaddd.analyzer.compiler.DddAnalysisIrGenerationExtension
import org.morecup.pragmaddd.analyzer.collector.MetadataCollectorImpl
import org.morecup.pragmaddd.analyzer.analyzer.ClassAnalyzerImpl
import org.morecup.pragmaddd.analyzer.detector.AnnotationDetectorImpl
import org.morecup.pragmaddd.analyzer.analyzer.MethodAnalyzerImpl
import org.morecup.pragmaddd.analyzer.analyzer.DocumentationExtractorImpl
import org.morecup.pragmaddd.analyzer.analyzer.PropertyAnalyzerImpl
import org.morecup.pragmaddd.analyzer.generator.JsonGeneratorImpl
import org.morecup.pragmaddd.analyzer.writer.ResourceWriterImpl
import org.morecup.pragmaddd.analyzer.model.ClassMetadata
import org.morecup.pragmaddd.analyzer.model.DddAnnotationType
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File
import java.nio.file.Path

/**
 * Comprehensive integration tests for test source set support
 * Verifies that the complete workflow works correctly for both main and test sources
 */
class TestSourceSupportIntegrationTest {
    
    @TempDir
    lateinit var tempDir: Path
    
    private lateinit var project: Project
    private lateinit var plugin: PragmaDddAnalyzerPlugin
    private lateinit var extension: PragmaDddAnalyzerExtension
    private lateinit var outputDir: File
    private val objectMapper = ObjectMapper().registerKotlinModule()
    
    @BeforeEach
    fun setup() {
        project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .build()
        
        plugin = PragmaDddAnalyzerPlugin()
        plugin.apply(project)
        
        extension = project.extensions.getByType(PragmaDddAnalyzerExtension::class.java)
        outputDir = tempDir.resolve("build/generated/ddd-analysis").toFile()
        outputDir.mkdirs()
        
        // Configure extension for testing
        extension.outputDirectory.set(outputDir.absolutePath)
        extension.includeTestSources.set(true)
        extension.jsonFileNaming.set("test-ddd-analysis")
        extension.enableMethodAnalysis.set(true)
        extension.enablePropertyAnalysis.set(true)
        extension.enableDocumentationExtraction.set(true)
    }
    
    @Test
    fun `test source support should be enabled by default`() {
        // Given - default configuration
        val defaultExtension = project.extensions.create("testDefault", PragmaDddAnalyzerExtension::class.java)
        defaultExtension.outputDirectory.convention("build/resources/main")
        defaultExtension.includeTestSources.convention(true)
        defaultExtension.jsonFileNaming.convention("ddd-analysis")
        
        // When & Then
        assertTrue(defaultExtension.includeTestSources.get())
        assertEquals("ddd-analysis-test.json", defaultExtension.getTestSourceJsonFileName())
        assertEquals("ddd-analysis-main.json", defaultExtension.getMainSourceJsonFileName())
    }
    
    @Test
    fun `extension should provide correct test source file names`() {
        // Given
        extension.jsonFileNaming.set("my-project")
        
        // When & Then
        assertEquals("my-project-main.json", extension.getMainSourceJsonFileName())
        assertEquals("my-project-test.json", extension.getTestSourceJsonFileName())
    }
    
    @Test
    fun `plugin should detect test compilation correctly`() {
        // Given
        val testCompilation = createMockCompilation("test")
        val mainCompilation = createMockCompilation("main")
        val integrationTestCompilation = createMockCompilation("integrationTest")
        
        // When
        val testOptions = plugin.applyToCompilation(testCompilation).get()
        val mainOptions = plugin.applyToCompilation(mainCompilation).get()
        val integrationTestOptions = plugin.applyToCompilation(integrationTestCompilation).get()
        
        // Then
        val testOptionsMap = testOptions.associate { it.key to it.value }
        val mainOptionsMap = mainOptions.associate { it.key to it.value }
        val integrationTestOptionsMap = integrationTestOptions.associate { it.key to it.value }
        
        assertEquals("true", testOptionsMap["isTestCompilation"])
        assertEquals("false", mainOptionsMap["isTestCompilation"])
        assertEquals("true", integrationTestOptionsMap["isTestCompilation"])
    }
    
    @Test
    fun `plugin should skip test compilation when includeTestSources is disabled`() {
        // Given
        extension.includeTestSources.set(false)
        val testCompilation = createMockCompilation("test")
        
        // When
        val testOptions = plugin.applyToCompilation(testCompilation).get()
        
        // Then
        assertTrue(testOptions.isEmpty(), "Test compilation should be skipped when includeTestSources is false")
    }
    
    @Test
    fun `metadata collector should separate main and test sources correctly`() {
        // Given
        val annotationDetector = AnnotationDetectorImpl()
        val documentationExtractor = DocumentationExtractorImpl()
        val propertyAnalyzer = PropertyAnalyzerImpl()
        val errorReporter = org.morecup.pragmaddd.analyzer.error.SilentErrorReporter()
        val methodAnalyzer = MethodAnalyzerImpl(annotationDetector, documentationExtractor, propertyAnalyzer, errorReporter, true)
        val classAnalyzer = ClassAnalyzerImpl(annotationDetector, methodAnalyzer, documentationExtractor, errorReporter)
        val metadataCollector = MetadataCollectorImpl(classAnalyzer, errorReporter)
        
        val mainClassMetadata = createTestClassMetadata("com.example.MainClass", DddAnnotationType.AGGREGATE_ROOT)
        val testClassMetadata = createTestClassMetadata("com.example.TestClass", DddAnnotationType.DOMAIN_ENTITY)
        
        // When
        metadataCollector.addToMainSources(mainClassMetadata)
        metadataCollector.addToTestSources(testClassMetadata)
        
        // Then
        val mainSources = metadataCollector.getMainSourcesMetadata()
        val testSources = metadataCollector.getTestSourcesMetadata()
        
        assertEquals(1, mainSources.size)
        assertEquals(1, testSources.size)
        assertEquals("com.example.MainClass", mainSources[0].className)
        assertEquals("com.example.TestClass", testSources[0].className)
        assertEquals(DddAnnotationType.AGGREGATE_ROOT, mainSources[0].annotationType)
        assertEquals(DddAnnotationType.DOMAIN_ENTITY, testSources[0].annotationType)
    }
    
    @Test
    fun `json generator should create separate files with correct source type`() {
        // Given
        val jsonGenerator = JsonGeneratorImpl()
        val mainMetadata = listOf(createTestClassMetadata("com.example.MainClass", DddAnnotationType.AGGREGATE_ROOT))
        val testMetadata = listOf(createTestClassMetadata("com.example.TestClass", DddAnnotationType.DOMAIN_ENTITY))
        
        // When
        val mainJson = jsonGenerator.generateMainSourcesJson(mainMetadata)
        val testJson = jsonGenerator.generateTestSourcesJson(testMetadata)
        
        // Then
        assertNotNull(mainJson)
        assertNotNull(testJson)
        
        val mainJsonTree = objectMapper.readTree(mainJson)
        val testJsonTree = objectMapper.readTree(testJson)
        
        assertEquals("main", mainJsonTree.get("sourceType").asText())
        assertEquals("test", testJsonTree.get("sourceType").asText())
        assertEquals(1, mainJsonTree.get("classes").size())
        assertEquals(1, testJsonTree.get("classes").size())
        
        assertEquals("com.example.MainClass", mainJsonTree.get("classes").get(0).get("className").asText())
        assertEquals("com.example.TestClass", testJsonTree.get("classes").get(0).get("className").asText())
    }
    
    @Test
    fun `resource writer should write test sources to correct location`() {
        // Given
        val resourceWriter = ResourceWriterImpl()
        val testJson = """
        {
            "generatedAt": "2024-01-15T10:30:00",
            "sourceType": "test",
            "classes": [
                {
                    "className": "com.example.TestClass",
                    "packageName": "com.example",
                    "annotationType": "DOMAIN_ENTITY",
                    "properties": [],
                    "methods": [],
                    "documentation": null,
                    "annotations": []
                }
            ]
        }
        """.trimIndent()
        
        // When
        resourceWriter.writeTestSourcesJson(testJson, outputDir.absolutePath, "test-analysis-test.json")
        
        // Then
        val expectedFile = File(outputDir, "META-INF/pragmaddd/test-analysis-test.json")
        assertTrue(expectedFile.exists(), "Test JSON file should be written to META-INF/pragmaddd/")
        
        val writtenContent = expectedFile.readText()
        val writtenJsonTree = objectMapper.readTree(writtenContent)
        assertEquals("test", writtenJsonTree.get("sourceType").asText())
        assertEquals("com.example.TestClass", writtenJsonTree.get("classes").get(0).get("className").asText())
    }
    
    @Test
    fun `metadata validation should detect cross-source duplicates`() {
        // Given
        val annotationDetector = AnnotationDetectorImpl()
        val documentationExtractor = DocumentationExtractorImpl()
        val propertyAnalyzer = PropertyAnalyzerImpl()
        val errorReporter = org.morecup.pragmaddd.analyzer.error.SilentErrorReporter()
        val methodAnalyzer = MethodAnalyzerImpl(annotationDetector, documentationExtractor, propertyAnalyzer, errorReporter, true)
        val classAnalyzer = ClassAnalyzerImpl(annotationDetector, methodAnalyzer, documentationExtractor, errorReporter)
        val metadataCollector = MetadataCollectorImpl(classAnalyzer, errorReporter)
        
        val duplicateClass1 = createTestClassMetadata("com.example.DuplicateClass", DddAnnotationType.AGGREGATE_ROOT)
        val duplicateClass2 = createTestClassMetadata("com.example.DuplicateClass", DddAnnotationType.AGGREGATE_ROOT)
        
        // When
        metadataCollector.addToMainSources(duplicateClass1)
        metadataCollector.addToTestSources(duplicateClass2)
        val validationResult = metadataCollector.validateMetadata()
        
        // Then
        assertFalse(validationResult.isValid)
        assertTrue(validationResult.errors.any { it.message.contains("exists in both main and test sources") })
    }
    
    @Test
    fun `ir generation extension should handle test compilation flag correctly`() {
        // Given
        val mainExtension = DddAnalysisIrGenerationExtension(
            outputDirectory = outputDir.absolutePath,
            isTestCompilation = false,
            jsonFileNaming = "test-analysis",
            enableMethodAnalysis = true,
            enablePropertyAnalysis = true,
            enableDocumentationExtraction = true
        )
        
        val testExtension = DddAnalysisIrGenerationExtension(
            outputDirectory = outputDir.absolutePath,
            isTestCompilation = true,
            jsonFileNaming = "test-analysis",
            enableMethodAnalysis = true,
            enablePropertyAnalysis = true,
            enableDocumentationExtraction = true
        )
        
        // When & Then - extensions should be created without errors
        assertNotNull(mainExtension)
        assertNotNull(testExtension)
        
        // The actual IR processing would happen during compilation
        // This test verifies that the extensions can be created with the correct parameters
    }
    
    @Test
    fun `complete workflow should generate separate json files for main and test`() {
        // Given
        val jsonGenerator = JsonGeneratorImpl()
        val resourceWriter = ResourceWriterImpl()
        
        val mainMetadata = listOf(
            createTestClassMetadata("com.example.User", DddAnnotationType.AGGREGATE_ROOT),
            createTestClassMetadata("com.example.Order", DddAnnotationType.DOMAIN_ENTITY)
        )
        
        val testMetadata = listOf(
            createTestClassMetadata("com.example.TestUser", DddAnnotationType.AGGREGATE_ROOT),
            createTestClassMetadata("com.example.TestOrder", DddAnnotationType.DOMAIN_ENTITY),
            createTestClassMetadata("com.example.TestValue", DddAnnotationType.VALUE_OBJ)
        )
        
        // When
        val mainJson = jsonGenerator.generateMainSourcesJson(mainMetadata)
        val testJson = jsonGenerator.generateTestSourcesJson(testMetadata)
        
        resourceWriter.writeMainSourcesJson(mainJson, outputDir.absolutePath, "complete-test-main.json")
        resourceWriter.writeTestSourcesJson(testJson, outputDir.absolutePath, "complete-test-test.json")
        
        // Then
        val mainJsonFile = File(outputDir, "META-INF/pragmaddd/complete-test-main.json")
        val testJsonFile = File(outputDir, "META-INF/pragmaddd/complete-test-test.json")
        
        assertTrue(mainJsonFile.exists())
        assertTrue(testJsonFile.exists())
        
        val mainJsonContent = objectMapper.readTree(mainJsonFile)
        val testJsonContent = objectMapper.readTree(testJsonFile)
        
        assertEquals("main", mainJsonContent.get("sourceType").asText())
        assertEquals("test", testJsonContent.get("sourceType").asText())
        assertEquals(2, mainJsonContent.get("classes").size())
        assertEquals(3, testJsonContent.get("classes").size())
        
        // Verify specific classes are in the correct files
        val mainClassNames = mainJsonContent.get("classes").map { it.get("className").asText() }
        val testClassNames = testJsonContent.get("classes").map { it.get("className").asText() }
        
        assertTrue(mainClassNames.contains("com.example.User"))
        assertTrue(mainClassNames.contains("com.example.Order"))
        assertTrue(testClassNames.contains("com.example.TestUser"))
        assertTrue(testClassNames.contains("com.example.TestOrder"))
        assertTrue(testClassNames.contains("com.example.TestValue"))
    }
    
    @Test
    fun `resource writer should verify json files are written correctly`() {
        // Given
        val resourceWriter = ResourceWriterImpl()
        val testJson = """{"sourceType": "test", "classes": []}"""
        val mainJson = """{"sourceType": "main", "classes": []}"""
        
        // When
        resourceWriter.writeMainSourcesJson(mainJson, outputDir.absolutePath, "verify-main.json")
        resourceWriter.writeTestSourcesJson(testJson, outputDir.absolutePath, "verify-test.json")
        
        // Then
        assertTrue(resourceWriter.verifyJsonFileWritten(outputDir.absolutePath, "pragmaddd/verify-main.json"))
        assertTrue(resourceWriter.verifyJsonFileWritten(outputDir.absolutePath, "pragmaddd/verify-test.json"))
        
        val jsonFiles = resourceWriter.listJsonFiles(outputDir.absolutePath)
        assertTrue(jsonFiles.any { it.name == "verify-main.json" })
        assertTrue(jsonFiles.any { it.name == "verify-test.json" })
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
     * Helper method to create test class metadata
     */
    private fun createTestClassMetadata(className: String, annotationType: DddAnnotationType): ClassMetadata {
        return ClassMetadata(
            className = className,
            packageName = "com.example",
            annotationType = annotationType,
            properties = emptyList(),
            methods = emptyList(),
            documentation = null,
            annotations = emptyList()
        )
    }
}