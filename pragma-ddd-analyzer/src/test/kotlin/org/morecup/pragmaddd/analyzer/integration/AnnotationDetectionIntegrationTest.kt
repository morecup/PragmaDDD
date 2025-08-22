package org.morecup.pragmaddd.analyzer.integration

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.morecup.pragmaddd.analyzer.detector.AnnotationDetector
import org.morecup.pragmaddd.analyzer.detector.AnnotationDetectorImpl
import org.morecup.pragmaddd.analyzer.model.DddAnnotationType

/**
 * Integration tests for annotation detection functionality
 * These tests verify that the annotation detector works correctly with the defined interfaces
 */
class AnnotationDetectionIntegrationTest {
    
    @Test
    fun `annotation detector should be properly instantiated`() {
        // Given & When
        val detector: AnnotationDetector = AnnotationDetectorImpl()
        
        // Then
        assertNotNull(detector)
        assertTrue(detector is AnnotationDetectorImpl)
    }
    
    @Test
    fun `ddd annotation types should be properly defined`() {
        // Given & When
        val annotationTypes = DddAnnotationType.values()
        
        // Then
        assertEquals(3, annotationTypes.size)
        assertTrue(annotationTypes.contains(DddAnnotationType.AGGREGATE_ROOT))
        assertTrue(annotationTypes.contains(DddAnnotationType.DOMAIN_ENTITY))
        assertTrue(annotationTypes.contains(DddAnnotationType.VALUE_OBJ))
    }
    
    @Test
    fun `annotation detector interface should have all required methods`() {
        // Given
        val detector: AnnotationDetector = AnnotationDetectorImpl()
        val detectorClass = detector::class.java
        
        // When
        val methods = detectorClass.methods.map { it.name }.toSet()
        
        // Then
        assertTrue(methods.contains("hasAggregateRootAnnotation"))
        assertTrue(methods.contains("hasDomainEntityAnnotation"))
        assertTrue(methods.contains("hasValueObjAnnotation"))
        assertTrue(methods.contains("getDddAnnotationType"))
        assertTrue(methods.contains("hasDddAnnotation"))
        assertTrue(methods.contains("extractAnnotationMetadata"))
        assertTrue(methods.contains("extractAllAnnotationMetadata"))
    }
    
    @Test
    fun `annotation detector should implement interface correctly`() {
        // Given
        val detector = AnnotationDetectorImpl()
        
        // When & Then
        assertTrue(detector is AnnotationDetector)
        
        // Verify that all interface methods are implemented
        val interfaceMethods = AnnotationDetector::class.java.methods
        val implementationMethods = detector::class.java.methods
        
        val interfaceMethodNames = interfaceMethods.map { it.name }.toSet()
        val implementationMethodNames = implementationMethods.map { it.name }.toSet()
        
        // All interface methods should be implemented
        interfaceMethodNames.forEach { methodName ->
            assertTrue(implementationMethodNames.contains(methodName), 
                "Method $methodName from interface should be implemented")
        }
    }
}
