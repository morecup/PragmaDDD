package org.morecup.pragmaddd.analyzer.error

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class AnalysisErrorTest {
    
    @Test
    fun `ClassAnalysisError should contain correct information`() {
        val className = "TestClass"
        val message = "Failed to analyze class"
        val cause = RuntimeException("Root cause")
        
        val error = AnalysisError.ClassAnalysisError(className, message, cause)
        
        assertEquals(className, error.className)
        assertEquals(message, error.message)
        assertEquals(cause, error.cause)
    }
    
    @Test
    fun `MethodAnalysisError should contain correct information`() {
        val className = "TestClass"
        val methodName = "testMethod"
        val message = "Failed to analyze method"
        val cause = RuntimeException("Root cause")
        
        val error = AnalysisError.MethodAnalysisError(className, methodName, message, cause)
        
        assertEquals(className, error.className)
        assertEquals(methodName, error.methodName)
        assertEquals(message, error.message)
        assertEquals(cause, error.cause)
    }
    
    @Test
    fun `PropertyAnalysisError should contain correct information`() {
        val className = "TestClass"
        val propertyName = "testProperty"
        val message = "Failed to analyze property"
        val cause = RuntimeException("Root cause")
        
        val error = AnalysisError.PropertyAnalysisError(className, propertyName, message, cause)
        
        assertEquals(className, error.className)
        assertEquals(propertyName, error.propertyName)
        assertEquals(message, error.message)
        assertEquals(cause, error.cause)
    }
    
    @Test
    fun `AnnotationDetectionError should contain correct information`() {
        val className = "TestClass"
        val message = "Failed to detect annotation"
        val cause = RuntimeException("Root cause")
        
        val error = AnalysisError.AnnotationDetectionError(className, message, cause)
        
        assertEquals(className, error.className)
        assertEquals(message, error.message)
        assertEquals(cause, error.cause)
    }
    
    @Test
    fun `DocumentationExtractionError should contain correct information`() {
        val className = "TestClass"
        val elementName = "testElement"
        val message = "Failed to extract documentation"
        val cause = RuntimeException("Root cause")
        
        val error = AnalysisError.DocumentationExtractionError(className, elementName, message, cause)
        
        assertEquals(className, error.className)
        assertEquals(elementName, error.elementName)
        assertEquals(message, error.message)
        assertEquals(cause, error.cause)
    }
    
    @Test
    fun `JsonGenerationError should contain correct information`() {
        val message = "Failed to generate JSON"
        val cause = RuntimeException("Root cause")
        
        val error = AnalysisError.JsonGenerationError(message, cause)
        
        assertEquals(message, error.message)
        assertEquals(cause, error.cause)
    }
    
    @Test
    fun `OutputGenerationError should contain correct information`() {
        val outputPath = "/path/to/output"
        val message = "Failed to write output"
        val cause = RuntimeException("Root cause")
        
        val error = AnalysisError.OutputGenerationError(outputPath, message, cause)
        
        assertEquals(outputPath, error.outputPath)
        assertEquals(message, error.message)
        assertEquals(cause, error.cause)
    }
    
    @Test
    fun `MetadataCollectionError should contain correct information`() {
        val className = "TestClass"
        val message = "Failed to collect metadata"
        val cause = RuntimeException("Root cause")
        
        val error = AnalysisError.MetadataCollectionError(className, message, cause)
        
        assertEquals(className, error.className)
        assertEquals(message, error.message)
        assertEquals(cause, error.cause)
    }
    
    @Test
    fun `ConfigurationError should contain correct information`() {
        val message = "Invalid configuration"
        val cause = RuntimeException("Root cause")
        
        val error = AnalysisError.ConfigurationError(message, cause)
        
        assertEquals(message, error.message)
        assertEquals(cause, error.cause)
    }
    
    @Test
    fun `AnalysisWarning should contain correct information`() {
        val message = "Warning message"
        val className = "TestClass"
        val elementName = "testElement"
        
        val warning = AnalysisWarning(message, className, elementName)
        
        assertEquals(message, warning.message)
        assertEquals(className, warning.className)
        assertEquals(elementName, warning.elementName)
    }
    
    @Test
    fun `AnalysisWarning should work with null optional fields`() {
        val message = "Warning message"
        
        val warning = AnalysisWarning(message)
        
        assertEquals(message, warning.message)
        assertNull(warning.className)
        assertNull(warning.elementName)
    }
}
