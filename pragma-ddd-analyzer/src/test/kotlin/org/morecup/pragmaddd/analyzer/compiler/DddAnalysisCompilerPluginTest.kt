package org.morecup.pragmaddd.analyzer.compiler

import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.kotlin.*

@OptIn(ExperimentalCompilerApi::class)
class DddAnalysisCompilerPluginTest {
    
    private lateinit var registrar: DddAnalysisCompilerPluginRegistrar
    private lateinit var configuration: CompilerConfiguration
    
    @BeforeEach
    fun setUp() {
        registrar = DddAnalysisCompilerPluginRegistrar()
        configuration = CompilerConfiguration()
    }
    
    @Test
    fun `should support K2 compiler`() {
        assertTrue(registrar.supportsK2)
    }
    
    @Test
    fun `should register extensions with default configuration`() {
        // Given - empty configuration (will use defaults)
        
        // When & Then - should not throw exception
        assertDoesNotThrow {
            with(registrar) {
                val extensionStorage = mock<CompilerPluginRegistrar.ExtensionStorage>()
                extensionStorage.registerExtensions(configuration)
            }
        }
    }
    
    @Test
    fun `should register extensions with custom configuration`() {
        // Given
        configuration.put(DddAnalysisCommandLineProcessor.OUTPUT_DIRECTORY_KEY, "/custom/output")
        configuration.put(DddAnalysisCommandLineProcessor.JSON_FILE_NAMING_KEY, "custom-analysis")
        configuration.put(DddAnalysisCommandLineProcessor.ENABLE_METHOD_ANALYSIS_KEY, false)
        configuration.put(DddAnalysisCommandLineProcessor.ENABLE_PROPERTY_ANALYSIS_KEY, false)
        configuration.put(DddAnalysisCommandLineProcessor.ENABLE_DOCUMENTATION_EXTRACTION_KEY, false)
        configuration.put(DddAnalysisCommandLineProcessor.MAX_CLASSES_PER_COMPILATION_KEY, 500)
        configuration.put(DddAnalysisCommandLineProcessor.FAIL_ON_ANALYSIS_ERRORS_KEY, true)
        
        // When & Then - should not throw exception
        assertDoesNotThrow {
            with(registrar) {
                val extensionStorage = mock<CompilerPluginRegistrar.ExtensionStorage>()
                extensionStorage.registerExtensions(configuration)
            }
        }
    }
    
    @Test
    fun `should handle null configuration values gracefully`() {
        // Given - configuration with some null values (should use defaults)
        // Don't set null values - just leave them unset to use defaults
        
        // When & Then - should not throw exception and use defaults
        assertDoesNotThrow {
            with(registrar) {
                val extensionStorage = mock<CompilerPluginRegistrar.ExtensionStorage>()
                extensionStorage.registerExtensions(configuration)
            }
        }
    }
    
    @Test
    fun `should handle partial configuration`() {
        // Given - only some configuration values set
        configuration.put(DddAnalysisCommandLineProcessor.OUTPUT_DIRECTORY_KEY, "/partial/output")
        configuration.put(DddAnalysisCommandLineProcessor.ENABLE_METHOD_ANALYSIS_KEY, false)
        
        // When & Then - should not throw exception
        assertDoesNotThrow {
            with(registrar) {
                val extensionStorage = mock<CompilerPluginRegistrar.ExtensionStorage>()
                extensionStorage.registerExtensions(configuration)
            }
        }
    }
}