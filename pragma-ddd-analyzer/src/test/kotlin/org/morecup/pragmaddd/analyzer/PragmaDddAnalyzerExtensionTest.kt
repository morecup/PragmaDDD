package org.morecup.pragmaddd.analyzer

import org.gradle.api.provider.Property
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Unit tests for PragmaDddAnalyzerExtension configuration validation and functionality
 */
class PragmaDddAnalyzerExtensionTest {
    
    @Test
    fun `validate should pass with valid configuration`() {
        // Given - output directory and JSON file naming are now fixed, only test configurable properties
        val extension = createMockExtension()
        
        // When & Then - should not throw
        extension.validate()
    }
    
    // Note: Tests for output directory and JSON file naming validation removed
    // because these properties are now fixed and not configurable by users
    
    @Test
    fun `validate should throw exception when maxClassesPerCompilation is zero or negative`() {
        listOf(0, -1, -100).forEach { invalidValue ->
            // Given
            val extension = createMockExtension(
                maxClassesPerCompilation = invalidValue
            )
            
            // When & Then
            val exception = assertThrows<IllegalArgumentException> {
                extension.validate()
            }
            assertTrue(
                exception.message!!.contains("Maximum classes per compilation must be a positive number"),
                "Expected error for invalid value $invalidValue, but got: ${exception.message}"
            )
        }
    }
    
    @Test
    fun `validate should throw exception when maxClassesPerCompilation is too high`() {
        // Given
        val extension = createMockExtension(
            maxClassesPerCompilation = 15000
        )
        
        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            extension.validate()
        }
        assertTrue(exception.message!!.contains("Maximum classes per compilation is too high"))
    }
    
    @Test
    fun `validate should throw exception when property analysis is enabled but method analysis is disabled`() {
        // Given
        val extension = createMockExtension(
            enableMethodAnalysis = false,
            enablePropertyAnalysis = true
        )
        
        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            extension.validate()
        }
        assertEquals("Property analysis requires method analysis to be enabled", exception.message)
    }
    
    @Test
    fun `validate should pass when both method and property analysis are disabled`() {
        // Given
        val extension = createMockExtension(
            enableMethodAnalysis = false,
            enablePropertyAnalysis = false
        )
        
        // When & Then - should not throw
        extension.validate()
    }
    
    // Note: Tests for output directory and JSON file naming validation removed
    // because these properties are now fixed and not configurable by users
    
    @Test
    fun `getMainSourceJsonFileName should return fixed file name`() {
        // Given
        val extension = createMockExtension()
        
        // When
        val fileName = extension.getMainSourceJsonFileName()
        
        // Then
        assertEquals("domain-analyzer.json", fileName)
    }
    
    // Test removed - getTestSourceJsonFileName method no longer exists
    
    @Test
    fun `hasAnyAnalysisFeaturesEnabled should return true when any feature is enabled`() {
        // Test method analysis enabled
        val extension1 = createMockExtension(
            enableMethodAnalysis = true,
            enablePropertyAnalysis = false,
            enableDocumentationExtraction = false
        )
        assertTrue(extension1.hasAnyAnalysisFeaturesEnabled())
        
        // Test property analysis enabled
        val extension2 = createMockExtension(
            enableMethodAnalysis = false,
            enablePropertyAnalysis = true,
            enableDocumentationExtraction = false
        )
        assertTrue(extension2.hasAnyAnalysisFeaturesEnabled())
        
        // Test documentation extraction enabled
        val extension3 = createMockExtension(
            enableMethodAnalysis = false,
            enablePropertyAnalysis = false,
            enableDocumentationExtraction = true
        )
        assertTrue(extension3.hasAnyAnalysisFeaturesEnabled())
    }
    
    @Test
    fun `hasAnyAnalysisFeaturesEnabled should return false when all features are disabled`() {
        // Given
        val extension = createMockExtension(
            enableMethodAnalysis = false,
            enablePropertyAnalysis = false,
            enableDocumentationExtraction = false
        )
        
        // When & Then
        assertFalse(extension.hasAnyAnalysisFeaturesEnabled())
    }
    
    @Test
    fun `getConfigurationSummary should return formatted summary with fixed paths`() {
        // Given
        val extension = createMockExtension(
            enableMethodAnalysis = false,
            enablePropertyAnalysis = true,
            enableDocumentationExtraction = false,
            maxClassesPerCompilation = 500,
            failOnAnalysisErrors = true
        )
        
        // When
        val summary = extension.getConfigurationSummary()
        
        // Then
        assertNotNull(summary)
        assertTrue(summary.contains("Pragma DDD Analyzer Configuration:"))
        assertTrue(summary.contains("Output Path: build/generated/pragmaddd/main/resources/META-INF/pragma-ddd-analyzer/domain-analyzer.json (FIXED - NOT CONFIGURABLE)"))
        assertTrue(summary.contains("Enable Method Analysis: false"))
        assertTrue(summary.contains("Enable Property Analysis: true"))
        assertTrue(summary.contains("Enable Documentation Extraction: false"))
        assertTrue(summary.contains("Max Classes Per Compilation: 500"))
        assertTrue(summary.contains("Fail On Analysis Errors: true"))
    }
    
    @Test
    fun `getConfigurationSummary should handle null values gracefully for configurable properties`() {
        // Given
        val extension = createMockExtension(
            enableMethodAnalysis = null,
            enablePropertyAnalysis = null,
            enableDocumentationExtraction = null,
            maxClassesPerCompilation = null,
            failOnAnalysisErrors = null
        )
        
        // When
        val summary = extension.getConfigurationSummary()
        
        // Then
        assertNotNull(summary)
        assertTrue(summary.contains("Output Path: build/generated/pragmaddd/main/resources/META-INF/pragma-ddd-analyzer/domain-analyzer.json (FIXED - NOT CONFIGURABLE)"))
        assertTrue(summary.contains("Enable Method Analysis: null"))
        assertTrue(summary.contains("Enable Property Analysis: null"))
        assertTrue(summary.contains("Enable Documentation Extraction: null"))
        assertTrue(summary.contains("Max Classes Per Compilation: null"))
        assertTrue(summary.contains("Fail On Analysis Errors: null"))
    }
    
    /**
     * Creates a mock extension with the specified configuration values
     * Note: outputDirectory and jsonFileNaming are now fixed and not configurable
     */
    private fun createMockExtension(
        enableMethodAnalysis: Boolean? = true,
        enablePropertyAnalysis: Boolean? = true,
        enableDocumentationExtraction: Boolean? = true,
        maxClassesPerCompilation: Int? = 1000,
        failOnAnalysisErrors: Boolean? = false
    ): PragmaDddAnalyzerExtension {
        val extension = object : PragmaDddAnalyzerExtension() {
            override val outputDirectory: Property<String> = mock()
            override val jsonFileNaming: Property<String> = mock()
            override val enableMethodAnalysis: Property<Boolean> = mock()
            override val enablePropertyAnalysis: Property<Boolean> = mock()
            override val enableDocumentationExtraction: Property<Boolean> = mock()
            override val maxClassesPerCompilation: Property<Int> = mock()
            override val failOnAnalysisErrors: Property<Boolean> = mock()
        }
        
        // Configure mock properties - fixed values for output directory and JSON naming
        whenever(extension.outputDirectory.orNull).thenReturn("build/generated/pragmaddd/main/resources")
        whenever(extension.jsonFileNaming.orNull).thenReturn("domain-analyzer")
        whenever(extension.enableMethodAnalysis.orNull).thenReturn(enableMethodAnalysis)
        whenever(extension.enablePropertyAnalysis.orNull).thenReturn(enablePropertyAnalysis)
        whenever(extension.enableDocumentationExtraction.orNull).thenReturn(enableDocumentationExtraction)
        whenever(extension.maxClassesPerCompilation.orNull).thenReturn(maxClassesPerCompilation)
        whenever(extension.failOnAnalysisErrors.orNull).thenReturn(failOnAnalysisErrors)
        
        // Mock the get() methods for the new utility methods
        whenever(extension.jsonFileNaming.get()).thenReturn("domain-analyzer")
        whenever(extension.enableMethodAnalysis.get()).thenReturn(enableMethodAnalysis ?: true)
        whenever(extension.enablePropertyAnalysis.get()).thenReturn(enablePropertyAnalysis ?: true)
        whenever(extension.enableDocumentationExtraction.get()).thenReturn(enableDocumentationExtraction ?: true)
        
        return extension
    }
}