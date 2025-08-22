package org.morecup.pragmaddd.analyzer.compiler

import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * Unit tests for DddAnalysisCommandLineProcessor configuration handling
 */
class DddAnalysisCommandLineProcessorTest {
    
    private lateinit var processor: DddAnalysisCommandLineProcessor
    private lateinit var configuration: CompilerConfiguration
    
    @BeforeEach
    fun setup() {
        processor = DddAnalysisCommandLineProcessor()
        configuration = CompilerConfiguration()
    }
    
    @Test
    fun `pluginId should return correct value`() {
        // When
        val pluginId = processor.pluginId
        
        // Then
        assertEquals("org.morecup.pragmaddd.analyzer", pluginId)
    }
    
    @Test
    fun `pluginOptions should contain all expected options`() {
        // When
        val options = processor.pluginOptions
        
        // Then
        assertEquals(8, options.size)
        
        val optionNames = options.map { it.optionName }.toSet()
        assertTrue(optionNames.contains("outputDirectory"))
        assertTrue(optionNames.contains("isTestCompilation"))
        assertTrue(optionNames.contains("jsonFileNaming"))
        assertTrue(optionNames.contains("enableMethodAnalysis"))
        assertTrue(optionNames.contains("enablePropertyAnalysis"))
        assertTrue(optionNames.contains("enableDocumentationExtraction"))
        assertTrue(optionNames.contains("maxClassesPerCompilation"))
        assertTrue(optionNames.contains("failOnAnalysisErrors"))
    }
    
    @Test
    fun `outputDirectory option should have correct configuration`() {
        // When
        val option = findOption("outputDirectory")
        
        // Then
        assertEquals("outputDirectory", option.optionName)
        assertEquals("path", option.valueDescription)
        assertEquals("Output directory for generated JSON files", option.description)
        assertFalse(option.required)
    }
    
    @Test
    fun `isTestCompilation option should have correct configuration`() {
        // When
        val option = findOption("isTestCompilation")
        
        // Then
        assertEquals("isTestCompilation", option.optionName)
        assertEquals("true|false", option.valueDescription)
        assertEquals("Whether this is a test compilation", option.description)
        assertFalse(option.required)
    }
    
    @Test
    fun `jsonFileNaming option should have correct configuration`() {
        // When
        val option = findOption("jsonFileNaming")
        
        // Then
        assertEquals("jsonFileNaming", option.optionName)
        assertEquals("string", option.valueDescription)
        assertEquals("JSON file naming convention", option.description)
        assertFalse(option.required)
    }
    
    @Test
    fun `enableMethodAnalysis option should have correct configuration`() {
        // When
        val option = findOption("enableMethodAnalysis")
        
        // Then
        assertEquals("enableMethodAnalysis", option.optionName)
        assertEquals("true|false", option.valueDescription)
        assertEquals("Enable method analysis", option.description)
        assertFalse(option.required)
    }
    
    @Test
    fun `enablePropertyAnalysis option should have correct configuration`() {
        // When
        val option = findOption("enablePropertyAnalysis")
        
        // Then
        assertEquals("enablePropertyAnalysis", option.optionName)
        assertEquals("true|false", option.valueDescription)
        assertEquals("Enable property analysis", option.description)
        assertFalse(option.required)
    }
    
    @Test
    fun `enableDocumentationExtraction option should have correct configuration`() {
        // When
        val option = findOption("enableDocumentationExtraction")
        
        // Then
        assertEquals("enableDocumentationExtraction", option.optionName)
        assertEquals("true|false", option.valueDescription)
        assertEquals("Enable documentation extraction from KDoc", option.description)
        assertFalse(option.required)
    }
    
    @Test
    fun `maxClassesPerCompilation option should have correct configuration`() {
        // When
        val option = findOption("maxClassesPerCompilation")
        
        // Then
        assertEquals("maxClassesPerCompilation", option.optionName)
        assertEquals("number", option.valueDescription)
        assertEquals("Maximum number of classes to analyze per compilation", option.description)
        assertFalse(option.required)
    }
    
    @Test
    fun `failOnAnalysisErrors option should have correct configuration`() {
        // When
        val option = findOption("failOnAnalysisErrors")
        
        // Then
        assertEquals("failOnAnalysisErrors", option.optionName)
        assertEquals("true|false", option.valueDescription)
        assertEquals("Whether to fail the build on analysis errors", option.description)
        assertFalse(option.required)
    }
    
    @Test
    fun `processOption should handle outputDirectory correctly`() {
        // Given
        val option = findAbstractOption("outputDirectory")
        val value = "/path/to/output"
        
        // When
        processor.processOption(option, value, configuration)
        
        // Then
        assertEquals(value, configuration.get(DddAnalysisCommandLineProcessor.OUTPUT_DIRECTORY_KEY))
    }
    
    @Test
    fun `processOption should handle isTestCompilation correctly`() {
        // Given
        val option = findAbstractOption("isTestCompilation")
        
        // When - test true value
        processor.processOption(option, "true", configuration)
        
        // Then
        assertTrue(configuration.get(DddAnalysisCommandLineProcessor.IS_TEST_COMPILATION_KEY)!!)
        
        // When - test false value
        processor.processOption(option, "false", configuration)
        
        // Then
        assertFalse(configuration.get(DddAnalysisCommandLineProcessor.IS_TEST_COMPILATION_KEY)!!)
    }
    
    @Test
    fun `processOption should handle jsonFileNaming correctly`() {
        // Given
        val option = findAbstractOption("jsonFileNaming")
        val value = "custom-analysis"
        
        // When
        processor.processOption(option, value, configuration)
        
        // Then
        assertEquals(value, configuration.get(DddAnalysisCommandLineProcessor.JSON_FILE_NAMING_KEY))
    }
    
    @Test
    fun `processOption should handle enableMethodAnalysis correctly`() {
        // Given
        val option = findAbstractOption("enableMethodAnalysis")
        
        // When - test true value
        processor.processOption(option, "true", configuration)
        
        // Then
        assertTrue(configuration.get(DddAnalysisCommandLineProcessor.ENABLE_METHOD_ANALYSIS_KEY)!!)
        
        // When - test false value
        processor.processOption(option, "false", configuration)
        
        // Then
        assertFalse(configuration.get(DddAnalysisCommandLineProcessor.ENABLE_METHOD_ANALYSIS_KEY)!!)
    }
    
    @Test
    fun `processOption should handle enablePropertyAnalysis correctly`() {
        // Given
        val option = findAbstractOption("enablePropertyAnalysis")
        
        // When - test true value
        processor.processOption(option, "true", configuration)
        
        // Then
        assertTrue(configuration.get(DddAnalysisCommandLineProcessor.ENABLE_PROPERTY_ANALYSIS_KEY)!!)
        
        // When - test false value
        processor.processOption(option, "false", configuration)
        
        // Then
        assertFalse(configuration.get(DddAnalysisCommandLineProcessor.ENABLE_PROPERTY_ANALYSIS_KEY)!!)
    }
    
    @Test
    fun `processOption should handle enableDocumentationExtraction correctly`() {
        // Given
        val option = findAbstractOption("enableDocumentationExtraction")
        
        // When - test true value
        processor.processOption(option, "true", configuration)
        
        // Then
        assertTrue(configuration.get(DddAnalysisCommandLineProcessor.ENABLE_DOCUMENTATION_EXTRACTION_KEY)!!)
        
        // When - test false value
        processor.processOption(option, "false", configuration)
        
        // Then
        assertFalse(configuration.get(DddAnalysisCommandLineProcessor.ENABLE_DOCUMENTATION_EXTRACTION_KEY)!!)
    }
    
    @Test
    fun `processOption should handle maxClassesPerCompilation correctly`() {
        // Given
        val option = findAbstractOption("maxClassesPerCompilation")
        val value = "500"
        
        // When
        processor.processOption(option, value, configuration)
        
        // Then
        assertEquals(500, configuration.get(DddAnalysisCommandLineProcessor.MAX_CLASSES_PER_COMPILATION_KEY))
    }
    
    @Test
    fun `processOption should handle failOnAnalysisErrors correctly`() {
        // Given
        val option = findAbstractOption("failOnAnalysisErrors")
        
        // When - test true value
        processor.processOption(option, "true", configuration)
        
        // Then
        assertTrue(configuration.get(DddAnalysisCommandLineProcessor.FAIL_ON_ANALYSIS_ERRORS_KEY)!!)
        
        // When - test false value
        processor.processOption(option, "false", configuration)
        
        // Then
        assertFalse(configuration.get(DddAnalysisCommandLineProcessor.FAIL_ON_ANALYSIS_ERRORS_KEY)!!)
    }
    
    @Test
    fun `configuration keys should have default null values`() {
        // Then
        assertNull(configuration.get(DddAnalysisCommandLineProcessor.OUTPUT_DIRECTORY_KEY))
        assertNull(configuration.get(DddAnalysisCommandLineProcessor.IS_TEST_COMPILATION_KEY))
        assertNull(configuration.get(DddAnalysisCommandLineProcessor.JSON_FILE_NAMING_KEY))
        assertNull(configuration.get(DddAnalysisCommandLineProcessor.ENABLE_METHOD_ANALYSIS_KEY))
        assertNull(configuration.get(DddAnalysisCommandLineProcessor.ENABLE_PROPERTY_ANALYSIS_KEY))
        assertNull(configuration.get(DddAnalysisCommandLineProcessor.ENABLE_DOCUMENTATION_EXTRACTION_KEY))
        assertNull(configuration.get(DddAnalysisCommandLineProcessor.MAX_CLASSES_PER_COMPILATION_KEY))
        assertNull(configuration.get(DddAnalysisCommandLineProcessor.FAIL_ON_ANALYSIS_ERRORS_KEY))
    }
    
    @Test
    fun `processOption should handle multiple options correctly`() {
        // Given
        val outputDirOption = findAbstractOption("outputDirectory")
        val testCompilationOption = findAbstractOption("isTestCompilation")
        val jsonNamingOption = findAbstractOption("jsonFileNaming")
        val methodAnalysisOption = findAbstractOption("enableMethodAnalysis")
        val propertyAnalysisOption = findAbstractOption("enablePropertyAnalysis")
        val docExtractionOption = findAbstractOption("enableDocumentationExtraction")
        val maxClassesOption = findAbstractOption("maxClassesPerCompilation")
        val failOnErrorsOption = findAbstractOption("failOnAnalysisErrors")
        
        // When
        processor.processOption(outputDirOption, "/custom/output", configuration)
        processor.processOption(testCompilationOption, "true", configuration)
        processor.processOption(jsonNamingOption, "my-analysis", configuration)
        processor.processOption(methodAnalysisOption, "false", configuration)
        processor.processOption(propertyAnalysisOption, "true", configuration)
        processor.processOption(docExtractionOption, "false", configuration)
        processor.processOption(maxClassesOption, "750", configuration)
        processor.processOption(failOnErrorsOption, "true", configuration)
        
        // Then
        assertEquals("/custom/output", configuration.get(DddAnalysisCommandLineProcessor.OUTPUT_DIRECTORY_KEY))
        assertTrue(configuration.get(DddAnalysisCommandLineProcessor.IS_TEST_COMPILATION_KEY)!!)
        assertEquals("my-analysis", configuration.get(DddAnalysisCommandLineProcessor.JSON_FILE_NAMING_KEY))
        assertFalse(configuration.get(DddAnalysisCommandLineProcessor.ENABLE_METHOD_ANALYSIS_KEY)!!)
        assertTrue(configuration.get(DddAnalysisCommandLineProcessor.ENABLE_PROPERTY_ANALYSIS_KEY)!!)
        assertFalse(configuration.get(DddAnalysisCommandLineProcessor.ENABLE_DOCUMENTATION_EXTRACTION_KEY)!!)
        assertEquals(750, configuration.get(DddAnalysisCommandLineProcessor.MAX_CLASSES_PER_COMPILATION_KEY))
        assertTrue(configuration.get(DddAnalysisCommandLineProcessor.FAIL_ON_ANALYSIS_ERRORS_KEY)!!)
    }
    
    private fun findOption(optionName: String): CliOption {
        return processor.pluginOptions.first { it.optionName == optionName }
    }
    
    private fun findAbstractOption(optionName: String): AbstractCliOption {
        return processor.pluginOptions.first { it.optionName == optionName } as AbstractCliOption
    }
}