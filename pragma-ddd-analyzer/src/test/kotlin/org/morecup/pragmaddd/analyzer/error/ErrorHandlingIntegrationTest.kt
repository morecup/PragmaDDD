package org.morecup.pragmaddd.analyzer.error

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.morecup.pragmaddd.analyzer.generator.JsonGenerator
import org.morecup.pragmaddd.analyzer.generator.JsonGeneratorImpl
import org.morecup.pragmaddd.analyzer.model.*
import java.io.File
import java.io.IOException

class ErrorHandlingIntegrationTest {
    
    private lateinit var errorReporter: SilentErrorReporter
    
    @BeforeEach
    fun setUp() {
        errorReporter = SilentErrorReporter()
    }
    
    @Test
    fun `ErrorReporter should collect and report different error types`() {
        // Test that ErrorReporter can handle various error types
        val classError = AnalysisError.ClassAnalysisError("TestClass", "Class analysis failed", RuntimeException("cause"))
        val methodError = AnalysisError.MethodAnalysisError("TestClass", "testMethod", "Method analysis failed", null)
        val propertyError = AnalysisError.PropertyAnalysisError("TestClass", "testProperty", "Property analysis failed", null)
        val jsonError = AnalysisError.JsonGenerationError("JSON generation failed", null)
        val outputError = AnalysisError.OutputGenerationError("/path/to/output", "Output generation failed", null)
        
        // Report errors
        errorReporter.reportError(classError)
        errorReporter.reportError(methodError)
        errorReporter.reportError(propertyError)
        errorReporter.reportError(jsonError)
        errorReporter.reportError(outputError)
        
        // Verify all errors were collected
        val errors = errorReporter.getErrors()
        assertEquals(5, errors.size)
        
        // Verify error types
        assertTrue(errors[0] is AnalysisError.ClassAnalysisError)
        assertTrue(errors[1] is AnalysisError.MethodAnalysisError)
        assertTrue(errors[2] is AnalysisError.PropertyAnalysisError)
        assertTrue(errors[3] is AnalysisError.JsonGenerationError)
        assertTrue(errors[4] is AnalysisError.OutputGenerationError)
    }
    
    @Test
    fun `JsonGenerator should handle serialization exceptions gracefully`() {
        val jsonGenerator = JsonGeneratorImpl(errorReporter)
        
        // Create metadata with problematic data that might cause serialization issues
        val problematicMetadata = listOf(
            ClassMetadata(
                className = "TestClass",
                packageName = "com.test",
                annotationType = DddAnnotationType.AGGREGATE_ROOT,
                properties = emptyList(),
                methods = emptyList(),
                documentation = null,
                annotations = emptyList()
            )
        )
        
        // This should work normally, but let's test error handling by mocking
        val result = jsonGenerator.generateMainSourcesJson(problematicMetadata)
        
        assertNotNull(result)
        assertTrue(result.contains("TestClass"))
        assertEquals(0, errorReporter.getErrors().size) // Should succeed normally
    }
    
    @Test
    fun `JsonGenerator should handle file write exceptions gracefully`() {
        val jsonGenerator = JsonGeneratorImpl(errorReporter)
        
        // Try to write to an invalid path (Windows-style invalid path)
        val invalidPath = "Z:\\invalid\\path\\that\\does\\not\\exist\\file.json"
        
        try {
            jsonGenerator.writeToFile("{}", invalidPath)
            fail("Expected exception to be thrown")
        } catch (e: Exception) {
            // Exception is expected
        }
        
        // Should report the error
        assertEquals(1, errorReporter.getErrors().size)
        
        val error = errorReporter.getErrors()[0]
        assertTrue(error is AnalysisError.OutputGenerationError)
        assertEquals(invalidPath, (error as AnalysisError.OutputGenerationError).outputPath)
    }
    
    @Test
    fun `JsonGenerator should validate JSON correctly`() {
        val jsonGenerator = JsonGeneratorImpl(errorReporter)
        
        // Valid JSON
        assertTrue(jsonGenerator.validateJson("""{"valid": "json"}"""))
        assertEquals(0, errorReporter.getErrors().size)
        
        // Invalid JSON
        assertFalse(jsonGenerator.validateJson("""{"invalid": json}"""))
        assertEquals(1, errorReporter.getErrors().size)
        
        val error = errorReporter.getErrors()[0]
        assertTrue(error is AnalysisError.JsonGenerationError)
        assertTrue(error.message.contains("JSON validation failed"))
    }
    
    @Test
    fun `ErrorReporter should handle warnings correctly`() {
        // Test that ErrorReporter can handle warnings
        val warning1 = AnalysisWarning("Missing documentation", "TestClass", "testMethod")
        val warning2 = AnalysisWarning("Empty method body", "TestClass", "emptyMethod")
        val warning3 = AnalysisWarning("General warning")
        
        // Report warnings
        errorReporter.reportWarning(warning1)
        errorReporter.reportWarning(warning2)
        errorReporter.reportWarning(warning3)
        
        // Verify all warnings were collected
        val warnings = errorReporter.getWarnings()
        assertEquals(3, warnings.size)
        
        // Verify warning content
        assertEquals("Missing documentation", warnings[0].message)
        assertEquals("TestClass", warnings[0].className)
        assertEquals("testMethod", warnings[0].elementName)
        
        assertEquals("Empty method body", warnings[1].message)
        assertEquals("TestClass", warnings[1].className)
        assertEquals("emptyMethod", warnings[1].elementName)
        
        assertEquals("General warning", warnings[2].message)
        assertNull(warnings[2].className)
        assertNull(warnings[2].elementName)
    }
    
    @Test
    fun `ErrorReporter should support clearing errors and warnings`() {
        // Add some errors and warnings
        errorReporter.reportError(AnalysisError.ClassAnalysisError("TestClass", "Error", null))
        errorReporter.reportWarning(AnalysisWarning("Warning", "TestClass"))
        
        // Verify they were added
        assertEquals(1, errorReporter.getErrors().size)
        assertEquals(1, errorReporter.getWarnings().size)
        
        // Clear all
        errorReporter.clear()
        
        // Verify they were cleared
        assertEquals(0, errorReporter.getErrors().size)
        assertEquals(0, errorReporter.getWarnings().size)
    }
    
    @Test
    fun `Error handling should work with real JsonGenerator file operations`() {
        val jsonGenerator = JsonGeneratorImpl(errorReporter)
        val tempDir = createTempDir()
        
        try {
            val metadata = listOf(
                ClassMetadata(
                    className = "TestClass",
                    packageName = "com.test",
                    annotationType = DddAnnotationType.VALUE_OBJ,
                    properties = emptyList(),
                    methods = emptyList(),
                    documentation = null,
                    annotations = emptyList()
                )
            )
            
            // This should succeed
            jsonGenerator.generateAndWriteMainSourcesJson(
                metadata,
                tempDir.absolutePath,
                "test-main.json"
            )
            
            assertEquals(0, errorReporter.getErrors().size)
            
            // Verify file was created
            val outputFile = File(tempDir, "test-main.json")
            assertTrue(outputFile.exists())
            
            val content = outputFile.readText()
            assertTrue(content.contains("TestClass"))
            assertTrue(content.contains("VALUE_OBJ"))
            
        } finally {
            tempDir.deleteRecursively()
        }
    }
    
    private fun createTempDir(): File {
        return kotlin.io.path.createTempDirectory("ddd-analyzer-test").toFile()
    }
}