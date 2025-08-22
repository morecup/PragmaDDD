package org.morecup.pragmaddd.analyzer.compiler

import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class DddAnalysisCommandLineProcessorTest {
    
    private lateinit var processor: DddAnalysisCommandLineProcessor
    private lateinit var configuration: CompilerConfiguration
    
    @BeforeEach
    fun setUp() {
        processor = DddAnalysisCommandLineProcessor()
        configuration = CompilerConfiguration()
    }
    
    @Test
    fun `should have correct plugin id`() {
        assertEquals("org.morecup.pragmaddd.analyzer", processor.pluginId)
    }
    
    @Test
    fun `should have all required plugin options`() {
        val options = processor.pluginOptions
        val optionNames = options.map { it.optionName }
        
        assertTrue(optionNames.contains("outputDirectory"))
        assertTrue(optionNames.contains("jsonFileNaming"))
        assertTrue(optionNames.contains("enableMethodAnalysis"))
        assertTrue(optionNames.contains("enablePropertyAnalysis"))
        assertTrue(optionNames.contains("enableDocumentationExtraction"))
        assertTrue(optionNames.contains("maxClassesPerCompilation"))
        assertTrue(optionNames.contains("failOnAnalysisErrors"))
    }
    
    @Test
    fun `should process output directory option`() {
        // Given
        val option = findOption("outputDirectory")
        val value = "/path/to/output"
        
        // When
        processor.processOption(option, value, configuration)
        
        // Then
        assertEquals(value, configuration.get(DddAnalysisCommandLineProcessor.OUTPUT_DIRECTORY_KEY))
    }
    
    @Test
    fun `should process json file naming option`() {
        // Given
        val option = findOption("jsonFileNaming")
        val value = "custom-analysis"
        
        // When
        processor.processOption(option, value, configuration)
        
        // Then
        assertEquals(value, configuration.get(DddAnalysisCommandLineProcessor.JSON_FILE_NAMING_KEY))
    }
    
    @Test
    fun `should process enable method analysis option`() {
        // Given
        val option = findOption("enableMethodAnalysis")
        val value = "false"
        
        // When
        processor.processOption(option, value, configuration)
        
        // Then
        assertEquals(false, configuration.get(DddAnalysisCommandLineProcessor.ENABLE_METHOD_ANALYSIS_KEY))
    }
    
    @Test
    fun `should process enable property analysis option`() {
        // Given
        val option = findOption("enablePropertyAnalysis")
        val value = "true"
        
        // When
        processor.processOption(option, value, configuration)
        
        // Then
        assertEquals(true, configuration.get(DddAnalysisCommandLineProcessor.ENABLE_PROPERTY_ANALYSIS_KEY))
    }
    
    @Test
    fun `should process enable documentation extraction option`() {
        // Given
        val option = findOption("enableDocumentationExtraction")
        val value = "false"
        
        // When
        processor.processOption(option, value, configuration)
        
        // Then
        assertEquals(false, configuration.get(DddAnalysisCommandLineProcessor.ENABLE_DOCUMENTATION_EXTRACTION_KEY))
    }
    
    @Test
    fun `should process max classes per compilation option`() {
        // Given
        val option = findOption("maxClassesPerCompilation")
        val value = "500"
        
        // When
        processor.processOption(option, value, configuration)
        
        // Then
        assertEquals(500, configuration.get(DddAnalysisCommandLineProcessor.MAX_CLASSES_PER_COMPILATION_KEY))
    }
    
    @Test
    fun `should process fail on analysis errors option`() {
        // Given
        val option = findOption("failOnAnalysisErrors")
        val value = "true"
        
        // When
        processor.processOption(option, value, configuration)
        
        // Then
        assertEquals(true, configuration.get(DddAnalysisCommandLineProcessor.FAIL_ON_ANALYSIS_ERRORS_KEY))
    }
    
    @Test
    fun `should handle boolean conversion for true values`() {
        // Given
        val option = findOption("enableMethodAnalysis")
        val value = "true"
        
        // When
        processor.processOption(option, value, configuration)
        
        // Then
        assertEquals(true, configuration.get(DddAnalysisCommandLineProcessor.ENABLE_METHOD_ANALYSIS_KEY))
    }
    
    @Test
    fun `should handle boolean conversion for false values`() {
        // Given
        val option = findOption("enableMethodAnalysis")
        val value = "false"
        
        // When
        processor.processOption(option, value, configuration)
        
        // Then
        assertEquals(false, configuration.get(DddAnalysisCommandLineProcessor.ENABLE_METHOD_ANALYSIS_KEY))
    }
    
    @Test
    fun `should handle integer conversion`() {
        // Given
        val option = findOption("maxClassesPerCompilation")
        val value = "1000"
        
        // When
        processor.processOption(option, value, configuration)
        
        // Then
        assertEquals(1000, configuration.get(DddAnalysisCommandLineProcessor.MAX_CLASSES_PER_COMPILATION_KEY))
    }
    
    @Test
    fun `should process multiple options correctly`() {
        // Given
        val outputOption = findOption("outputDirectory")
        val namingOption = findOption("jsonFileNaming")
        val methodOption = findOption("enableMethodAnalysis")
        
        // When
        processor.processOption(outputOption, "/output", configuration)
        processor.processOption(namingOption, "test-analysis", configuration)
        processor.processOption(methodOption, "true", configuration)
        
        // Then
        assertEquals("/output", configuration.get(DddAnalysisCommandLineProcessor.OUTPUT_DIRECTORY_KEY))
        assertEquals("test-analysis", configuration.get(DddAnalysisCommandLineProcessor.JSON_FILE_NAMING_KEY))
        assertEquals(true, configuration.get(DddAnalysisCommandLineProcessor.ENABLE_METHOD_ANALYSIS_KEY))
    }
    
    private fun findOption(optionName: String): CliOption {
        return processor.pluginOptions.find { it.optionName == optionName }
            ?: throw IllegalArgumentException("Option not found: $optionName")
    }
}
