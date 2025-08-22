package org.morecup.pragmaddd.analyzer.compiler

import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Tests for the DDD Analysis Compiler Plugin infrastructure
 */
@OptIn(ExperimentalCompilerApi::class)
class DddAnalysisCompilerPluginTest {
    
    @Test
    fun `compiler plugin registrar should be instantiable`() {
        val registrar = DddAnalysisCompilerPluginRegistrar()
        assertNotNull(registrar)
        assertTrue(registrar.supportsK2)
    }
    
    @Test
    fun `command line processor should have correct plugin id`() {
        val processor = DddAnalysisCommandLineProcessor()
        assertEquals("org.morecup.pragmaddd.analyzer", processor.pluginId)
    }
    
    @Test
    fun `command line processor should have expected options`() {
        val processor = DddAnalysisCommandLineProcessor()
        val optionNames = processor.pluginOptions.map { it.optionName }.toSet()
        
        assertTrue(optionNames.contains("outputDirectory"))
        assertTrue(optionNames.contains("isTestCompilation"))
        assertTrue(optionNames.contains("enableMethodAnalysis"))
        assertTrue(optionNames.contains("enablePropertyAnalysis"))
    }
    
    @Test
    fun `command line processor should have configuration keys`() {
        // Test that the configuration keys are properly defined
        assertNotNull(DddAnalysisCommandLineProcessor.OUTPUT_DIRECTORY_KEY)
        assertNotNull(DddAnalysisCommandLineProcessor.IS_TEST_COMPILATION_KEY)
        assertNotNull(DddAnalysisCommandLineProcessor.ENABLE_METHOD_ANALYSIS_KEY)
        assertNotNull(DddAnalysisCommandLineProcessor.ENABLE_PROPERTY_ANALYSIS_KEY)
    }
    
    @Test
    fun `ir generation extension should be instantiable`() {
        val extension = DddAnalysisIrGenerationExtension(
            outputDirectory = "/test/output",
            isTestCompilation = false,
            jsonFileNaming = "test-analysis",
            enableMethodAnalysis = true,
            enablePropertyAnalysis = true,
            enableDocumentationExtraction = true
        )
        assertNotNull(extension)
    }
}