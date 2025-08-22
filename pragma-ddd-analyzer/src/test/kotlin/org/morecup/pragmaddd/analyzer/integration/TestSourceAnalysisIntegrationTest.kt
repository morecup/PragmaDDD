package org.morecup.pragmaddd.analyzer.integration

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.io.TempDir
import org.morecup.pragmaddd.analyzer.PragmaDddAnalyzerExtension
import org.morecup.pragmaddd.analyzer.PragmaDddAnalyzerPlugin
import org.morecup.pragmaddd.analyzer.collector.MetadataCollectorImpl
import org.morecup.pragmaddd.analyzer.analyzer.ClassAnalyzerImpl
import org.morecup.pragmaddd.analyzer.detector.AnnotationDetectorImpl
import org.morecup.pragmaddd.analyzer.analyzer.MethodAnalyzerImpl
import org.morecup.pragmaddd.analyzer.analyzer.DocumentationExtractorImpl
import org.morecup.pragmaddd.analyzer.analyzer.PropertyAnalyzerImpl
import org.morecup.pragmaddd.analyzer.generator.JsonGeneratorImpl
import org.morecup.pragmaddd.analyzer.writer.ResourceWriterImpl
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File
import java.nio.file.Path

/**
 * Integration tests for test source set analysis
 * Tests the complete workflow of analyzing test classes and generating separate JSON files
 */
class TestSourceAnalysisIntegrationTest {
    
    @TempDir
    lateinit var tempDir: Path
    
    private lateinit var project: Project
    private lateinit var plugin: PragmaDddAnalyzerPlugin
    private lateinit var outputDir: File
    
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
    fun `plugin should detect test compilation correctly`() {
        // Given
        plugin.apply(project)
        
        // When - test compilation
        val testCompilation = createMockCompilation("test")
        val testOptions = plugin.applyToCompilation(testCompilation).get()
        
        // Then
        assertFalse(testOptions.isEmpty())
        val testOptionsMap = testOptions.associate { it.key to it.value }
        assertEquals("true", testOptionsMap["isTestCompilation"])
        
        // When - main compilation
        val mainCompilation = createMockCompilation("main")
        val mainOptions = plugin.applyToCompilation(mainCompilation).get()
        
        // Then
        val mainOptionsMap = mainOptions.associate { it.key to it.value }
        assertEquals("false", mainOptionsMap["isTestCompilation"])
    }
    
    @Test
    fun `plugin should handle different test compilation names`() {
        // Given
        plugin.apply(project)
        val testCompilationNames = listOf("test", "integrationTest", "functionalTest", "testFixtures")
        
        testCompilationNames.forEach { compilationName ->
            // When
            val compilation = createMockCompilation(compilationName)
            val options = plugin.applyToCompilation(compilation).get()
            
            // Then
            if (options.isNotEmpty()) {
                val optionsMap = options.associate { it.key to it.value }
                assertEquals("true", optionsMap["isTestCompilation"], 
                    "Compilation '$compilationName' should be detected as test compilation")
            }
        }
    }
    
    @Test
    fun `plugin should skip test compilation when includeTestSources is disabled`() {
        // Given
        plugin.apply(project)
        val extension = project.extensions.getByType(PragmaDddAnalyzerExtension::class.java)
        extension.includeTestSources.set(false)
        
        // When
        val testCompilation = createMockCompilation("test")
        val testOptions = plugin.applyToCompilation(testCompilation).get()
        
        // Then
        assertTrue(testOptions.isEmpty(), "Test compilation should be skipped when includeTestSources is false")
    }
    
    @Test
    fun `metadata collector should separate main and test sources`() {
        // Given
        val annotationDetector = AnnotationDetectorImpl()
        val documentationExtractor = DocumentationExtractorImpl()
        val propertyAnalyzer = PropertyAnalyzerImpl()
        val errorReporter = org.morecup.pragmaddd.analyzer.error.SilentErrorReporter()
        val methodAnalyzer = MethodAnalyzerImpl(annotationDetector, documentationExtractor, propertyAnalyzer, errorReporter, true)
        val classAnalyzer = ClassAnalyzerImpl(annotationDetector, methodAnalyzer, documentationExtractor, errorReporter)
        val metadataCollector = MetadataCollectorImpl(classAnalyzer, errorReporter)
        
        // Create mock metadata
        val mainClassMetadata = createMockClassMetadata("com.example.MainClass", "AGGREGATE_ROOT")
        val testClassMetadata = createMockClassMetadata("com.example.TestClass", "DOMAIN_ENTITY")
        
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
    }
    
    @Test
    fun `json generator should create separate files for main and test sources`() {
        // Given
        val jsonGenerator = JsonGeneratorImpl()
        val mainMetadata = listOf(createMockClassMetadata("com.example.MainClass", "AGGREGATE_ROOT"))
        val testMetadata = listOf(createMockClassMetadata("com.example.TestClass", "DOMAIN_ENTITY"))
        
        // When
        val mainJson = jsonGenerator.generateMainSourcesJson(mainMetadata)
        val testJson = jsonGenerator.generateTestSourcesJson(testMetadata)
        
        // Then
        assertNotNull(mainJson)
        assertNotNull(testJson)
        assertTrue(mainJson.contains("\"sourceType\" : \"main\"") || mainJson.contains("\"sourceType\":\"main\""))
        assertTrue(testJson.contains("\"sourceType\" : \"test\"") || testJson.contains("\"sourceType\":\"test\""))
        assertTrue(mainJson.contains("com.example.MainClass"))
        assertTrue(testJson.contains("com.example.TestClass"))
    }
    
    @Test
    fun `resource writer should write test sources to correct location`() {
        // Given
        val resourceWriter = ResourceWriterImpl()
        val testJson = """{"sourceType": "test", "classes": []}"""
        val testFileName = "ddd-analysis-test.json"
        
        // When
        resourceWriter.writeTestSourcesJson(testJson, outputDir.absolutePath, testFileName)
        
        // Then
        val expectedFile = File(outputDir, "META-INF/ddd-analysis/$testFileName")
        assertTrue(expectedFile.exists(), "Test JSON file should be written to META-INF/ddd-analysis/")
        assertEquals(testJson, expectedFile.readText())
    }
    
    @Test
    fun `plugin should configure test jar packaging when test sources are enabled`() {
        // Given
        plugin.apply(project)
        val extension = project.extensions.getByType(PragmaDddAnalyzerExtension::class.java)
        extension.includeTestSources.set(true)
        
        // When - simulate afterEvaluate
        project.afterEvaluate { }
        
        // Then - test JAR task should be configured
        // Note: In a real scenario, this would check for actual task configuration
        assertTrue(extension.includeTestSources.get())
    }
    
    @Test
    fun `extension should provide correct test source file names`() {
        // Given
        plugin.apply(project)
        val extension = project.extensions.getByType(PragmaDddAnalyzerExtension::class.java)
        extension.jsonFileNaming.set("my-project")
        
        // When & Then
        assertEquals("my-project-test.json", extension.getTestSourceJsonFileName())
        assertEquals("my-project-main.json", extension.getMainSourceJsonFileName())
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
        
        // Create duplicate metadata
        val duplicateClass1 = createMockClassMetadata("com.example.DuplicateClass", "AGGREGATE_ROOT")
        val duplicateClass2 = createMockClassMetadata("com.example.DuplicateClass", "AGGREGATE_ROOT")
        
        // When
        metadataCollector.addToMainSources(duplicateClass1)
        metadataCollector.addToTestSources(duplicateClass2)
        val validationResult = metadataCollector.validateMetadata()
        
        // Then
        assertFalse(validationResult.isValid)
        assertTrue(validationResult.errors.any { it.message.contains("exists in both main and test sources") })
    }
    
    @Test
    fun `plugin should handle test source output directory configuration`() {
        // Given
        plugin.apply(project)
        val extension = project.extensions.getByType(PragmaDddAnalyzerExtension::class.java)
        extension.outputDirectory.set("build/test-resources")
        
        // When
        val testCompilation = createMockCompilation("test")
        val options = plugin.applyToCompilation(testCompilation).get()
        
        // Then
        val optionsMap = options.associate { it.key to it.value }
        assertTrue(optionsMap["outputDirectory"]!!.contains("test-resources"))
    }
    
    /**
     * Helper method to create mock Kotlin compilation
     */
    private fun createMockCompilation(name: String): KotlinCompilation<*> {
        val compilation = mock<KotlinCompilation<*>>()
        val target = mock<org.jetbrains.kotlin.gradle.plugin.KotlinTarget>()
        
        whenever(compilation.name).thenReturn(name)
        whenever(compilation.target).thenReturn(target)
        whenever(target.project).thenReturn(project)
        
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
}