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
        // Given
        val extension = createMockExtension(
            outputDirectory = "build/resources",
            jsonFileNaming = "ddd-analysis"
        )
        
        // When & Then - should not throw
        extension.validate()
    }
    
    @Test
    fun `validate should throw exception when output directory is null`() {
        // Given
        val extension = createMockExtension(
            outputDirectory = null,
            jsonFileNaming = "ddd-analysis"
        )
        
        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            extension.validate()
        }
        assertEquals("Output directory cannot be null or blank", exception.message)
    }
    
    @Test
    fun `validate should throw exception when output directory is blank`() {
        // Given
        val extension = createMockExtension(
            outputDirectory = "   ",
            jsonFileNaming = "ddd-analysis"
        )
        
        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            extension.validate()
        }
        assertEquals("Output directory cannot be null or blank", exception.message)
    }
    
    @Test
    fun `validate should throw exception when json file naming is null`() {
        // Given
        val extension = createMockExtension(
            outputDirectory = "build/resources",
            jsonFileNaming = null
        )
        
        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            extension.validate()
        }
        assertEquals("JSON file naming cannot be null or blank", exception.message)
    }
    
    @Test
    fun `validate should throw exception when json file naming is blank`() {
        // Given
        val extension = createMockExtension(
            outputDirectory = "build/resources",
            jsonFileNaming = ""
        )
        
        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            extension.validate()
        }
        assertEquals("JSON file naming cannot be null or blank", exception.message)
    }
    
    @Test
    fun `validate should throw exception when json file naming contains invalid characters`() {
        val invalidChars = listOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')
        
        invalidChars.forEach { invalidChar ->
            // Given
            val extension = createMockExtension(
                outputDirectory = "build/resources",
                jsonFileNaming = "ddd${invalidChar}analysis"
            )
            
            // When & Then
            val exception = assertThrows<IllegalArgumentException> {
                extension.validate()
            }
            assertTrue(
                exception.message!!.contains("JSON file naming contains invalid characters"),
                "Expected error message for invalid character '$invalidChar', but got: ${exception.message}"
            )
        }
    }
    
    @Test
    fun `validate should pass with valid json file naming characters`() {
        val validNames = listOf(
            "ddd-analysis",
            "ddd_analysis",
            "dddAnalysis",
            "DDD.Analysis",
            "ddd123analysis",
            "analysis-v1.0"
        )
        
        validNames.forEach { validName ->
            // Given
            val extension = createMockExtension(
                outputDirectory = "build/resources",
                jsonFileNaming = validName
            )
            
            // When & Then - should not throw
            extension.validate()
        }
    }
    
    @Test
    fun `validate should throw exception when maxClassesPerCompilation is zero or negative`() {
        listOf(0, -1, -100).forEach { invalidValue ->
            // Given
            val extension = createMockExtension(
                outputDirectory = "build/resources",
                jsonFileNaming = "ddd-analysis",
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
            outputDirectory = "build/resources",
            jsonFileNaming = "ddd-analysis",
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
            outputDirectory = "build/resources",
            jsonFileNaming = "ddd-analysis",
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
            outputDirectory = "build/resources",
            jsonFileNaming = "ddd-analysis",
            enableMethodAnalysis = false,
            enablePropertyAnalysis = false
        )
        
        // When & Then - should not throw
        extension.validate()
    }
    
    @Test
    fun `validate should throw exception for reserved file names`() {
        val reservedNames = listOf("CON", "PRN", "AUX", "NUL", "COM1", "LPT1")
        
        reservedNames.forEach { reservedName ->
            // Given
            val extension = createMockExtension(
                outputDirectory = "build/resources",
                jsonFileNaming = reservedName
            )
            
            // When & Then
            val exception = assertThrows<IllegalArgumentException> {
                extension.validate()
            }
            assertTrue(
                exception.message!!.contains("JSON file naming cannot use reserved system names"),
                "Expected error for reserved name '$reservedName', but got: ${exception.message}"
            )
        }
    }
    
    @Test
    fun `validate should throw exception for too long json file naming`() {
        // Given
        val longName = "a".repeat(101)
        val extension = createMockExtension(
            outputDirectory = "build/resources",
            jsonFileNaming = longName
        )
        
        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            extension.validate()
        }
        assertTrue(exception.message!!.contains("JSON file naming is too long"))
    }
    
    @Test
    fun `validate should throw exception for output directory with invalid path characters`() {
        val invalidChars = listOf('*', '?', '"', '<', '>', '|')
        
        invalidChars.forEach { invalidChar ->
            // Given
            val extension = createMockExtension(
                outputDirectory = "build/output${invalidChar}dir",
                jsonFileNaming = "ddd-analysis"
            )
            
            // When & Then
            val exception = assertThrows<IllegalArgumentException> {
                extension.validate()
            }
            assertTrue(
                exception.message!!.contains("Output directory contains invalid path characters"),
                "Expected error for invalid character '$invalidChar', but got: ${exception.message}"
            )
        }
    }
    
    @Test
    fun `validate should throw exception for output directory with leading or trailing whitespace`() {
        listOf("  build/resources", "build/resources  ", "  build/resources  ").forEach { invalidPath ->
            // Given
            val extension = createMockExtension(
                outputDirectory = invalidPath,
                jsonFileNaming = "ddd-analysis"
            )
            
            // When & Then
            val exception = assertThrows<IllegalArgumentException> {
                extension.validate()
            }
            assertTrue(exception.message!!.contains("Output directory cannot have leading or trailing whitespace"))
        }
    }
    
    @Test
    fun `getMainSourceJsonFileName should return correct file name`() {
        // Given
        val extension = createMockExtension(jsonFileNaming = "custom-analysis")
        
        // When
        val fileName = extension.getMainSourceJsonFileName()
        
        // Then
        assertEquals("custom-analysis-main.json", fileName)
    }
    
    @Test
    fun `getTestSourceJsonFileName should return correct file name`() {
        // Given
        val extension = createMockExtension(jsonFileNaming = "custom-analysis")
        
        // When
        val fileName = extension.getTestSourceJsonFileName()
        
        // Then
        assertEquals("custom-analysis-test.json", fileName)
    }
    
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
    fun `getConfigurationSummary should return formatted summary`() {
        // Given
        val extension = createMockExtension(
            outputDirectory = "build/resources",
            includeTestSources = true,
            jsonFileNaming = "custom-analysis",
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
        assertTrue(summary.contains("Output Directory: build/resources"))
        assertTrue(summary.contains("Include Test Sources: true"))
        assertTrue(summary.contains("JSON File Naming: custom-analysis"))
        assertTrue(summary.contains("Enable Method Analysis: false"))
        assertTrue(summary.contains("Enable Property Analysis: true"))
        assertTrue(summary.contains("Enable Documentation Extraction: false"))
        assertTrue(summary.contains("Max Classes Per Compilation: 500"))
        assertTrue(summary.contains("Fail On Analysis Errors: true"))
    }
    
    @Test
    fun `getConfigurationSummary should handle null values gracefully`() {
        // Given
        val extension = createMockExtension(
            outputDirectory = null,
            includeTestSources = null,
            jsonFileNaming = null,
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
        assertTrue(summary.contains("Output Directory: null"))
        assertTrue(summary.contains("Include Test Sources: null"))
        assertTrue(summary.contains("JSON File Naming: null"))
        assertTrue(summary.contains("Enable Method Analysis: null"))
        assertTrue(summary.contains("Enable Property Analysis: null"))
        assertTrue(summary.contains("Enable Documentation Extraction: null"))
        assertTrue(summary.contains("Max Classes Per Compilation: null"))
        assertTrue(summary.contains("Fail On Analysis Errors: null"))
    }
    
    /**
     * Creates a mock extension with the specified configuration values
     */
    private fun createMockExtension(
        outputDirectory: String? = "build/resources",
        includeTestSources: Boolean? = true,
        jsonFileNaming: String? = "ddd-analysis",
        enableMethodAnalysis: Boolean? = true,
        enablePropertyAnalysis: Boolean? = true,
        enableDocumentationExtraction: Boolean? = true,
        maxClassesPerCompilation: Int? = 1000,
        failOnAnalysisErrors: Boolean? = false
    ): PragmaDddAnalyzerExtension {
        val extension = object : PragmaDddAnalyzerExtension() {
            override val outputDirectory: Property<String> = mock()
            override val includeTestSources: Property<Boolean> = mock()
            override val jsonFileNaming: Property<String> = mock()
            override val enableMethodAnalysis: Property<Boolean> = mock()
            override val enablePropertyAnalysis: Property<Boolean> = mock()
            override val enableDocumentationExtraction: Property<Boolean> = mock()
            override val maxClassesPerCompilation: Property<Int> = mock()
            override val failOnAnalysisErrors: Property<Boolean> = mock()
        }
        
        // Configure mock properties
        whenever(extension.outputDirectory.orNull).thenReturn(outputDirectory)
        whenever(extension.includeTestSources.orNull).thenReturn(includeTestSources)
        whenever(extension.jsonFileNaming.orNull).thenReturn(jsonFileNaming)
        whenever(extension.enableMethodAnalysis.orNull).thenReturn(enableMethodAnalysis)
        whenever(extension.enablePropertyAnalysis.orNull).thenReturn(enablePropertyAnalysis)
        whenever(extension.enableDocumentationExtraction.orNull).thenReturn(enableDocumentationExtraction)
        whenever(extension.maxClassesPerCompilation.orNull).thenReturn(maxClassesPerCompilation)
        whenever(extension.failOnAnalysisErrors.orNull).thenReturn(failOnAnalysisErrors)
        
        // Mock the get() methods for the new utility methods
        whenever(extension.jsonFileNaming.get()).thenReturn(jsonFileNaming ?: "ddd-analysis")
        whenever(extension.enableMethodAnalysis.get()).thenReturn(enableMethodAnalysis ?: true)
        whenever(extension.enablePropertyAnalysis.get()).thenReturn(enablePropertyAnalysis ?: true)
        whenever(extension.enableDocumentationExtraction.get()).thenReturn(enableDocumentationExtraction ?: true)
        
        return extension
    }
}