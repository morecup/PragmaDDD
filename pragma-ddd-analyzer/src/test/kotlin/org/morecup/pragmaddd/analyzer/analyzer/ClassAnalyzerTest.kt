package org.morecup.pragmaddd.analyzer.analyzer

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.morecup.pragmaddd.analyzer.detector.AnnotationDetectorImpl
import org.morecup.pragmaddd.analyzer.model.*
import org.morecup.pragmaddd.analyzer.error.SilentErrorReporter

class ClassAnalyzerTest {

    @Test
    fun `should verify ClassAnalyzer interface exists and has correct methods`() {
        // This test verifies that the ClassAnalyzer interface exists and has the expected methods
        val methods = ClassAnalyzer::class.java.methods
        val methodNames = methods.map { it.name }.toSet()
        
        assertTrue(methodNames.contains("analyzeClass"))
        assertTrue(methodNames.contains("extractProperties"))
        assertTrue(methodNames.contains("extractMethods"))
        assertTrue(methodNames.contains("extractDocumentation"))
    }

    @Test
    fun `should create ClassAnalyzerImpl and verify it implements ClassAnalyzer`() {
        // Given
        val annotationDetector = AnnotationDetectorImpl()
        val documentationExtractor = DocumentationExtractorImpl()
        val propertyAnalyzer = PropertyAnalyzerImpl()
        val errorReporter = SilentErrorReporter()
        val methodAnalyzer = MethodAnalyzerImpl(annotationDetector, documentationExtractor, propertyAnalyzer, errorReporter, true)
        
        // When
        val analyzer = ClassAnalyzerImpl(annotationDetector, methodAnalyzer, documentationExtractor, errorReporter)

        // Then
        assertNotNull(analyzer)
        assertTrue(analyzer is ClassAnalyzer)
        
        // Verify all interface methods are implemented
        val interfaceMethods = ClassAnalyzer::class.java.methods
        val implMethods = ClassAnalyzerImpl::class.java.methods
        
        val interfaceMethodNames = interfaceMethods.map { it.name }.toSet()
        val implMethodNames = implMethods.map { it.name }.toSet()
        
        interfaceMethodNames.forEach { methodName ->
            assertTrue(implMethodNames.contains(methodName), "Method $methodName should be implemented")
        }
    }

    @Test
    fun `should verify interface methods exist on implementation`() {
        // Given
        val annotationDetector = AnnotationDetectorImpl()
        val documentationExtractor = DocumentationExtractorImpl()
        val propertyAnalyzer = PropertyAnalyzerImpl()
        val errorReporter = org.morecup.pragmaddd.analyzer.error.SilentErrorReporter()
        val methodAnalyzer = MethodAnalyzerImpl(annotationDetector, documentationExtractor, propertyAnalyzer, errorReporter, true)
        val analyzer = ClassAnalyzerImpl(annotationDetector, methodAnalyzer, documentationExtractor, errorReporter)

        // Then - verify interface methods exist
        assertNotNull(analyzer::analyzeClass)
        assertNotNull(analyzer::extractProperties)
        assertNotNull(analyzer::extractMethods)
        assertNotNull(analyzer::extractDocumentation)
    }

    @Test
    fun `should verify data model classes exist and have correct structure`() {
        // Test that all required data model classes exist and have the expected structure
        
        // ClassMetadata
        val classMetadata = ClassMetadata(
            className = "TestClass",
            packageName = "com.test",
            annotationType = DddAnnotationType.AGGREGATE_ROOT,
            properties = emptyList(),
            methods = emptyList(),
            documentation = null,
            annotations = emptyList()
        )
        assertNotNull(classMetadata)
        assertEquals("TestClass", classMetadata.className)
        assertEquals(DddAnnotationType.AGGREGATE_ROOT, classMetadata.annotationType)

        // PropertyMetadata
        val propertyMetadata = PropertyMetadata(
            name = "testProperty",
            type = "String",
            isPrivate = false,
            isMutable = true,
            documentation = null,
            annotations = emptyList()
        )
        assertNotNull(propertyMetadata)
        assertEquals("testProperty", propertyMetadata.name)
        assertEquals("String", propertyMetadata.type)

        // MethodMetadata
        val methodMetadata = MethodMetadata(
            name = "testMethod",
            parameters = emptyList(),
            returnType = "Unit",
            isPrivate = false,
            methodCalls = emptyList(),
            propertyAccesses = emptyList(),
            documentation = null,
            annotations = emptyList()
        )
        assertNotNull(methodMetadata)
        assertEquals("testMethod", methodMetadata.name)
        assertEquals("Unit", methodMetadata.returnType)

        // DocumentationMetadata
        val documentationMetadata = DocumentationMetadata(
            summary = "Test summary",
            description = "Test description",
            parameters = emptyMap(),
            returnDescription = null
        )
        assertNotNull(documentationMetadata)
        assertEquals("Test summary", documentationMetadata.summary)
        assertEquals("Test description", documentationMetadata.description)
    }

    @Test
    fun `should verify DDD annotation types are available`() {
        // Test that all DDD annotation types are available
        val aggregateRoot = DddAnnotationType.AGGREGATE_ROOT
        val domainEntity = DddAnnotationType.DOMAIN_ENTITY
        val valueObj = DddAnnotationType.VALUE_OBJ

        assertNotNull(aggregateRoot)
        assertNotNull(domainEntity)
        assertNotNull(valueObj)

        // Verify enum values
        assertEquals("AGGREGATE_ROOT", aggregateRoot.name)
        assertEquals("DOMAIN_ENTITY", domainEntity.name)
        assertEquals("VALUE_OBJ", valueObj.name)
    }

    @Test
    fun `should verify PropertyAccessType enum is available`() {
        // Test that PropertyAccessType enum is available
        val read = PropertyAccessType.GET
        val write = PropertyAccessType.SET

        assertNotNull(read)
        assertNotNull(write)

        // Verify enum values
        assertEquals("READ", read.name)
        assertEquals("WRITE", write.name)
    }

    @Test
    fun `should verify ParameterMetadata structure`() {
        // Test ParameterMetadata structure
        val parameterMetadata = ParameterMetadata(
            name = "testParam",
            type = "String",
            annotations = emptyList()
        )
        
        assertNotNull(parameterMetadata)
        assertEquals("testParam", parameterMetadata.name)
        assertEquals("String", parameterMetadata.type)
        assertTrue(parameterMetadata.annotations.isEmpty())
    }

    @Test
    fun `should verify MethodCallMetadata structure`() {
        // Test MethodCallMetadata structure
        val methodCallMetadata = MethodCallMetadata(
            targetMethod = "testMethod",
            receiverType = "TestClass",
            parameters = listOf("String", "Int")
        )
        
        assertNotNull(methodCallMetadata)
        assertEquals("testMethod", methodCallMetadata.targetMethod)
        assertEquals("TestClass", methodCallMetadata.receiverType)
        assertEquals(2, methodCallMetadata.parameters.size)
        assertEquals("String", methodCallMetadata.parameters[0])
        assertEquals("Int", methodCallMetadata.parameters[1])
    }

    @Test
    fun `should verify PropertyAccessMetadata structure`() {
        // Test PropertyAccessMetadata structure
        val propertyAccessMetadata = PropertyAccessMetadata(
            propertyName = "testProperty",
            accessType = PropertyAccessType.GET,
            ownerClass = "TestClass"
        )
        
        assertNotNull(propertyAccessMetadata)
        assertEquals("testProperty", propertyAccessMetadata.propertyName)
        assertEquals(PropertyAccessType.GET, propertyAccessMetadata.accessType)
        assertEquals("TestClass", propertyAccessMetadata.ownerClass)
    }

    @Test
    fun `should verify AnnotationMetadata structure`() {
        // Test AnnotationMetadata structure
        val annotationMetadata = AnnotationMetadata(
            name = "TestAnnotation",
            parameters = mapOf("value" to "test", "enabled" to true)
        )
        
        assertNotNull(annotationMetadata)
        assertEquals("TestAnnotation", annotationMetadata.name)
        assertEquals(2, annotationMetadata.parameters.size)
        assertEquals("test", annotationMetadata.parameters["value"])
        assertEquals(true, annotationMetadata.parameters["enabled"])
    }
}
