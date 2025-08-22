package org.morecup.pragmaddd.analyzer.error

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class ErrorReporterTest {
    
    private lateinit var defaultReporter: DefaultErrorReporter
    private lateinit var silentReporter: SilentErrorReporter
    private lateinit var outputStream: ByteArrayOutputStream
    private lateinit var originalOut: PrintStream
    
    @BeforeEach
    fun setUp() {
        defaultReporter = DefaultErrorReporter(failOnError = false, logPrefix = "Test")
        silentReporter = SilentErrorReporter(failOnError = false)
        
        // Capture console output for testing
        outputStream = ByteArrayOutputStream()
        originalOut = System.out
        System.setOut(PrintStream(outputStream))
    }
    
    @Test
    fun `DefaultErrorReporter should report warnings correctly`() {
        val warning = AnalysisWarning("Test warning", "TestClass", "testElement")
        
        defaultReporter.reportWarning(warning)
        
        val warnings = defaultReporter.getWarnings()
        assertEquals(1, warnings.size)
        assertEquals(warning, warnings[0])
        
        val output = outputStream.toString()
        assertTrue(output.contains("Test: WARNING in TestClass.testElement: Test warning"))
    }
    
    @Test
    fun `DefaultErrorReporter should report errors correctly`() {
        val error = AnalysisError.ClassAnalysisError("TestClass", "Test error", RuntimeException("cause"))
        
        defaultReporter.reportError(error)
        
        val errors = defaultReporter.getErrors()
        assertEquals(1, errors.size)
        assertEquals(error, errors[0])
        
        val output = outputStream.toString()
        assertTrue(output.contains("Test: ERROR in TestClass: Test error"))
        assertTrue(output.contains("Caused by: RuntimeException: cause"))
    }
    
    @Test
    fun `DefaultErrorReporter should not fail build by default`() {
        val error = AnalysisError.ClassAnalysisError("TestClass", "Test error", null)
        
        defaultReporter.reportError(error)
        
        assertFalse(defaultReporter.shouldFailBuild(listOf(error)))
    }
    
    @Test
    fun `DefaultErrorReporter should fail build on critical errors when configured`() {
        val failingReporter = DefaultErrorReporter(failOnError = true)
        val configError = AnalysisError.ConfigurationError("Config error", null)
        val outputError = AnalysisError.OutputGenerationError("/path", "Output error", null)
        val jsonError = AnalysisError.JsonGenerationError("JSON error", null)
        val classError = AnalysisError.ClassAnalysisError("TestClass", "Class error", null)
        
        assertTrue(failingReporter.shouldFailBuild(listOf(configError)))
        assertTrue(failingReporter.shouldFailBuild(listOf(outputError)))
        assertTrue(failingReporter.shouldFailBuild(listOf(jsonError)))
        assertFalse(failingReporter.shouldFailBuild(listOf(classError)))
    }
    
    @Test
    fun `DefaultErrorReporter should clear errors and warnings`() {
        val warning = AnalysisWarning("Test warning")
        val error = AnalysisError.ClassAnalysisError("TestClass", "Test error", null)
        
        defaultReporter.reportWarning(warning)
        defaultReporter.reportError(error)
        
        assertEquals(1, defaultReporter.getWarnings().size)
        assertEquals(1, defaultReporter.getErrors().size)
        
        defaultReporter.clear()
        
        assertEquals(0, defaultReporter.getWarnings().size)
        assertEquals(0, defaultReporter.getErrors().size)
    }
    
    @Test
    fun `SilentErrorReporter should collect errors without logging`() {
        val warning = AnalysisWarning("Test warning")
        val error = AnalysisError.ClassAnalysisError("TestClass", "Test error", null)
        
        silentReporter.reportWarning(warning)
        silentReporter.reportError(error)
        
        assertEquals(1, silentReporter.getWarnings().size)
        assertEquals(1, silentReporter.getErrors().size)
        assertEquals(warning, silentReporter.getWarnings()[0])
        assertEquals(error, silentReporter.getErrors()[0])
        
        // Should not produce any console output
        val output = outputStream.toString()
        assertTrue(output.isEmpty())
    }
    
    @Test
    fun `SilentErrorReporter should fail build when configured`() {
        val failingReporter = SilentErrorReporter(failOnError = true)
        val error = AnalysisError.ClassAnalysisError("TestClass", "Test error", null)
        
        assertTrue(failingReporter.shouldFailBuild(listOf(error)))
    }
    
    @Test
    fun `DefaultErrorReporter should handle warnings without class or element name`() {
        val warning = AnalysisWarning("General warning")
        
        defaultReporter.reportWarning(warning)
        
        val output = outputStream.toString()
        assertTrue(output.contains("Test: WARNING: General warning"))
        assertFalse(output.contains(" in "))
    }
    
    @Test
    fun `DefaultErrorReporter should handle warnings with class but no element name`() {
        val warning = AnalysisWarning("Class warning", "TestClass")
        
        defaultReporter.reportWarning(warning)
        
        val output = outputStream.toString()
        assertTrue(output.contains("Test: WARNING in TestClass: Class warning"))
    }
    
    @Test
    fun `DefaultErrorReporter should handle errors without cause`() {
        val error = AnalysisError.ClassAnalysisError("TestClass", "Test error", null)
        
        defaultReporter.reportError(error)
        
        val output = outputStream.toString()
        assertTrue(output.contains("Test: ERROR in TestClass: Test error"))
        assertFalse(output.contains("Caused by:"))
    }
    
    @Test
    fun `DefaultErrorReporter should show stack trace in debug mode`() {
        // Set debug mode
        System.setProperty("ddd.analyzer.debug", "true")
        
        val error = AnalysisError.ClassAnalysisError("TestClass", "Test error", RuntimeException("cause"))
        
        defaultReporter.reportError(error)
        
        val output = outputStream.toString()
        assertTrue(output.contains("Test: ERROR in TestClass: Test error"))
        assertTrue(output.contains("Caused by: RuntimeException: cause"))
        
        // Clean up
        System.clearProperty("ddd.analyzer.debug")
    }
    
    @Test
    fun `DefaultErrorReporter should handle different error types for location extraction`() {
        val methodError = AnalysisError.MethodAnalysisError("TestClass", "testMethod", "Method error", null)
        val propertyError = AnalysisError.PropertyAnalysisError("TestClass", "testProperty", "Property error", null)
        val docError = AnalysisError.DocumentationExtractionError("TestClass", "testElement", "Doc error", null)
        val jsonError = AnalysisError.JsonGenerationError("JSON error", null)
        
        defaultReporter.reportError(methodError)
        defaultReporter.reportError(propertyError)
        defaultReporter.reportError(docError)
        defaultReporter.reportError(jsonError)
        
        val output = outputStream.toString()
        assertTrue(output.contains("ERROR in TestClass.testMethod: Method error"))
        assertTrue(output.contains("ERROR in TestClass.testProperty: Property error"))
        assertTrue(output.contains("ERROR in TestClass.testElement: Doc error"))
        assertTrue(output.contains("ERROR: JSON error"))
    }
    
    @Test
    fun `ErrorReporter should return immutable lists`() {
        val warning = AnalysisWarning("Test warning")
        val error = AnalysisError.ClassAnalysisError("TestClass", "Test error", null)
        
        defaultReporter.reportWarning(warning)
        defaultReporter.reportError(error)
        
        val warnings = defaultReporter.getWarnings()
        val errors = defaultReporter.getErrors()
        
        // Verify we can't modify the returned lists
        assertThrows(UnsupportedOperationException::class.java) {
            (warnings as MutableList).clear()
        }
        
        assertThrows(UnsupportedOperationException::class.java) {
            (errors as MutableList).clear()
        }
    }
    
    @org.junit.jupiter.api.AfterEach
    fun tearDown() {
        System.setOut(originalOut)
    }
}
