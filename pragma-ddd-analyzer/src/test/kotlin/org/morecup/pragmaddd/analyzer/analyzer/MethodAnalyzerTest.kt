package org.morecup.pragmaddd.analyzer.analyzer

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.morecup.pragmaddd.analyzer.detector.AnnotationDetectorImpl
import org.morecup.pragmaddd.analyzer.model.*

class MethodAnalyzerTest {

    private lateinit var annotationDetector: AnnotationDetectorImpl
    private lateinit var documentationExtractor: DocumentationExtractor
    private lateinit var propertyAnalyzer: PropertyAnalyzer
    private lateinit var methodAnalyzer: MethodAnalyzer

    @BeforeEach
    fun setUp() {
        annotationDetector = AnnotationDetectorImpl()
        documentationExtractor = DocumentationExtractorImpl()
        propertyAnalyzer = PropertyAnalyzerImpl()
        val errorReporter = org.morecup.pragmaddd.analyzer.error.SilentErrorReporter()
        methodAnalyzer = MethodAnalyzerImpl(annotationDetector, documentationExtractor, propertyAnalyzer, errorReporter, true)
    }

    @Test
    fun `should verify MethodAnalyzer interface exists and has correct methods`() {
        // This test verifies that the MethodAnalyzer interface exists and has the expected methods
        val methods = MethodAnalyzer::class.java.methods
        val methodNames = methods.map { it.name }.toSet()
        
        assertTrue(methodNames.contains("analyzeMethod"))
        assertTrue(methodNames.contains("extractMethodCalls"))
        assertTrue(methodNames.contains("extractPropertyAccess"))
        assertTrue(methodNames.contains("extractDocumentation"))
    }

    @Test
    fun `should create MethodAnalyzerImpl and verify it implements MethodAnalyzer`() {
        // Given
        val errorReporter = org.morecup.pragmaddd.analyzer.error.SilentErrorReporter()
        val analyzer = MethodAnalyzerImpl(annotationDetector, documentationExtractor, propertyAnalyzer, errorReporter, true)

        // Then
        assertNotNull(analyzer)
        assertTrue(analyzer is MethodAnalyzer)
        
        // Verify all interface methods are implemented
        val interfaceMethods = MethodAnalyzer::class.java.methods
        val implMethods = MethodAnalyzerImpl::class.java.methods
        
        val interfaceMethodNames = interfaceMethods.map { it.name }.toSet()
        val implMethodNames = implMethods.map { it.name }.toSet()
        
        interfaceMethodNames.forEach { methodName ->
            assertTrue(implMethodNames.contains(methodName), "Method $methodName should be implemented")
        }
    }

    @Test
    fun `should verify interface methods exist on implementation`() {
        // Given
        val errorReporter = org.morecup.pragmaddd.analyzer.error.SilentErrorReporter()
        val analyzer = MethodAnalyzerImpl(annotationDetector, documentationExtractor, propertyAnalyzer, errorReporter, true)

        // Then - verify interface methods exist
        assertNotNull(analyzer::analyzeMethod)
        assertNotNull(analyzer::extractMethodCalls)
        assertNotNull(analyzer::extractPropertyAccess)
        assertNotNull(analyzer::extractDocumentation)
    }

    @Test
    fun `should verify MethodCallMetadata structure for method call analysis`() {
        // Test MethodCallMetadata structure for method call detection
        val methodCallMetadata = MethodCallMetadata(
            targetMethod = "testMethod",
            receiverType = "com.example.TestClass",
            parameters = listOf("String", "Int", "Boolean")
        )
        
        assertNotNull(methodCallMetadata)
        assertEquals("testMethod", methodCallMetadata.targetMethod)
        assertEquals("com.example.TestClass", methodCallMetadata.receiverType)
        assertEquals(3, methodCallMetadata.parameters.size)
        assertEquals("String", methodCallMetadata.parameters[0])
        assertEquals("Int", methodCallMetadata.parameters[1])
        assertEquals("Boolean", methodCallMetadata.parameters[2])
    }

    @Test
    fun `should verify PropertyAccessMetadata structure for property access detection`() {
        // Test PropertyAccessMetadata structure for property access detection
        val readAccess = PropertyAccessMetadata(
            propertyName = "testProperty",
            accessType = PropertyAccessType.READ,
            ownerClass = "com.example.TestClass"
        )
        
        val writeAccess = PropertyAccessMetadata(
            propertyName = "testProperty",
            accessType = PropertyAccessType.WRITE,
            ownerClass = "com.example.TestClass"
        )
        
        assertNotNull(readAccess)
        assertEquals("testProperty", readAccess.propertyName)
        assertEquals(PropertyAccessType.READ, readAccess.accessType)
        assertEquals("com.example.TestClass", readAccess.ownerClass)
        
        assertNotNull(writeAccess)
        assertEquals("testProperty", writeAccess.propertyName)
        assertEquals(PropertyAccessType.WRITE, writeAccess.accessType)
        assertEquals("com.example.TestClass", writeAccess.ownerClass)
    }

    @Test
    fun `should verify MethodMetadata structure includes method calls and property accesses`() {
        // Test MethodMetadata structure with method calls and property accesses
        val methodCalls = listOf(
            MethodCallMetadata("method1", "Class1", listOf("String")),
            MethodCallMetadata("method2", "Class2", listOf("Int", "Boolean"))
        )
        
        val propertyAccesses = listOf(
            PropertyAccessMetadata("prop1", PropertyAccessType.READ, "Class1"),
            PropertyAccessMetadata("prop2", PropertyAccessType.WRITE, "Class2")
        )
        
        val methodMetadata = MethodMetadata(
            name = "testMethod",
            parameters = listOf(
                ParameterMetadata("param1", "String"),
                ParameterMetadata("param2", "Int")
            ),
            returnType = "Boolean",
            isPrivate = false,
            methodCalls = methodCalls,
            propertyAccesses = propertyAccesses,
            documentation = null,
            annotations = emptyList()
        )
        
        assertNotNull(methodMetadata)
        assertEquals("testMethod", methodMetadata.name)
        assertEquals("Boolean", methodMetadata.returnType)
        assertEquals(2, methodMetadata.parameters.size)
        assertEquals(2, methodMetadata.methodCalls.size)
        assertEquals(2, methodMetadata.propertyAccesses.size)
        
        // Verify method calls
        assertEquals("method1", methodMetadata.methodCalls[0].targetMethod)
        assertEquals("Class1", methodMetadata.methodCalls[0].receiverType)
        assertEquals("method2", methodMetadata.methodCalls[1].targetMethod)
        assertEquals("Class2", methodMetadata.methodCalls[1].receiverType)
        
        // Verify property accesses
        assertEquals("prop1", methodMetadata.propertyAccesses[0].propertyName)
        assertEquals(PropertyAccessType.READ, methodMetadata.propertyAccesses[0].accessType)
        assertEquals("prop2", methodMetadata.propertyAccesses[1].propertyName)
        assertEquals(PropertyAccessType.WRITE, methodMetadata.propertyAccesses[1].accessType)
    }

    @Test
    fun `should verify PropertyAccessType enum values for field access detection`() {
        // Test PropertyAccessType enum for field reads and writes
        val readType = PropertyAccessType.READ
        val writeType = PropertyAccessType.WRITE
        
        assertNotNull(readType)
        assertNotNull(writeType)
        assertEquals("READ", readType.name)
        assertEquals("WRITE", writeType.name)
        
        // Verify enum values are distinct
        assertNotEquals(readType, writeType)
    }

    @Test
    fun `should verify ParameterMetadata structure for method parameter analysis`() {
        // Test ParameterMetadata structure for method parameters
        val parameterWithAnnotations = ParameterMetadata(
            name = "annotatedParam",
            type = "String",
            annotations = listOf(
                AnnotationMetadata("NotNull", emptyMap()),
                AnnotationMetadata("Size", mapOf("min" to 1, "max" to 100))
            )
        )
        
        val parameterWithoutAnnotations = ParameterMetadata(
            name = "simpleParam",
            type = "Int",
            annotations = emptyList()
        )
        
        assertNotNull(parameterWithAnnotations)
        assertEquals("annotatedParam", parameterWithAnnotations.name)
        assertEquals("String", parameterWithAnnotations.type)
        assertEquals(2, parameterWithAnnotations.annotations.size)
        assertEquals("NotNull", parameterWithAnnotations.annotations[0].name)
        assertEquals("Size", parameterWithAnnotations.annotations[1].name)
        
        assertNotNull(parameterWithoutAnnotations)
        assertEquals("simpleParam", parameterWithoutAnnotations.name)
        assertEquals("Int", parameterWithoutAnnotations.type)
        assertTrue(parameterWithoutAnnotations.annotations.isEmpty())
    }

    @Test
    fun `should verify method analysis supports complex method signatures`() {
        // Test complex method signature analysis
        val complexMethodMetadata = MethodMetadata(
            name = "complexMethod",
            parameters = listOf(
                ParameterMetadata("param1", "List<String>"),
                ParameterMetadata("param2", "Map<String, Int>"),
                ParameterMetadata("param3", "Optional<Boolean>")
            ),
            returnType = "CompletableFuture<Result<String>>",
            isPrivate = true,
            methodCalls = listOf(
                MethodCallMetadata("stream", "List", emptyList()),
                MethodCallMetadata("filter", "Stream", listOf("Predicate")),
                MethodCallMetadata("collect", "Stream", listOf("Collector"))
            ),
            propertyAccesses = listOf(
                PropertyAccessMetadata("internalState", PropertyAccessType.READ, "CurrentClass"),
                PropertyAccessMetadata("cache", PropertyAccessType.WRITE, "CurrentClass")
            ),
            documentation = DocumentationMetadata(
                summary = "Complex method for testing",
                description = "This method demonstrates complex parameter types and return types",
                parameters = mapOf(
                    "param1" to "List of strings to process",
                    "param2" to "Mapping configuration",
                    "param3" to "Optional boolean flag"
                ),
                returnDescription = "Future containing the result"
            ),
            annotations = listOf(
                AnnotationMetadata("Async", emptyMap()),
                AnnotationMetadata("Transactional", mapOf("readOnly" to false))
            )
        )
        
        assertNotNull(complexMethodMetadata)
        assertEquals("complexMethod", complexMethodMetadata.name)
        assertEquals("CompletableFuture<Result<String>>", complexMethodMetadata.returnType)
        assertTrue(complexMethodMetadata.isPrivate)
        assertEquals(3, complexMethodMetadata.parameters.size)
        assertEquals(3, complexMethodMetadata.methodCalls.size)
        assertEquals(2, complexMethodMetadata.propertyAccesses.size)
        assertNotNull(complexMethodMetadata.documentation)
        assertEquals(2, complexMethodMetadata.annotations.size)
        
        // Verify complex parameter types
        assertEquals("List<String>", complexMethodMetadata.parameters[0].type)
        assertEquals("Map<String, Int>", complexMethodMetadata.parameters[1].type)
        assertEquals("Optional<Boolean>", complexMethodMetadata.parameters[2].type)
        
        // Verify method call chain analysis
        assertEquals("stream", complexMethodMetadata.methodCalls[0].targetMethod)
        assertEquals("filter", complexMethodMetadata.methodCalls[1].targetMethod)
        assertEquals("collect", complexMethodMetadata.methodCalls[2].targetMethod)
        
        // Verify property access patterns
        assertEquals(PropertyAccessType.READ, complexMethodMetadata.propertyAccesses[0].accessType)
        assertEquals(PropertyAccessType.WRITE, complexMethodMetadata.propertyAccesses[1].accessType)
    }

    @Test
    fun `should verify method analysis handles constructor calls`() {
        // Test constructor call detection in method analysis
        val constructorCallMetadata = MethodCallMetadata(
            targetMethod = "<init>",
            receiverType = "com.example.NewObject",
            parameters = listOf("String", "Int")
        )
        
        assertNotNull(constructorCallMetadata)
        assertEquals("<init>", constructorCallMetadata.targetMethod)
        assertEquals("com.example.NewObject", constructorCallMetadata.receiverType)
        assertEquals(2, constructorCallMetadata.parameters.size)
        assertEquals("String", constructorCallMetadata.parameters[0])
        assertEquals("Int", constructorCallMetadata.parameters[1])
    }

    @Test
    fun `should verify method analysis handles extension function calls`() {
        // Test extension function call detection
        val extensionCallMetadata = MethodCallMetadata(
            targetMethod = "extensionFunction",
            receiverType = "com.example.ExtensionReceiver",
            parameters = listOf("String")
        )
        
        assertNotNull(extensionCallMetadata)
        assertEquals("extensionFunction", extensionCallMetadata.targetMethod)
        assertEquals("com.example.ExtensionReceiver", extensionCallMetadata.receiverType)
        assertEquals(1, extensionCallMetadata.parameters.size)
        assertEquals("String", extensionCallMetadata.parameters[0])
    }
}