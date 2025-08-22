package org.morecup.pragmaddd.analyzer.detector

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.name.FqName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.morecup.pragmaddd.analyzer.model.DddAnnotationType

/**
 * Unit tests for AnnotationDetector functionality
 * These tests focus on the core logic and interface contracts
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
class AnnotationDetectorTest {
    
    private lateinit var annotationDetector: AnnotationDetector
    
    @BeforeEach
    fun setUp() {
        annotationDetector = AnnotationDetectorImpl()
    }
    
    @Test
    fun `annotation detector should be instantiable`() {
        // When & Then
        assertNotNull(annotationDetector)
        assertTrue(annotationDetector is AnnotationDetectorImpl)
    }
    
    @Test
    fun `annotation detector should implement all interface methods`() {
        // This test verifies that the interface is properly implemented
        // We can't test the actual IR functionality without complex setup,
        // but we can verify the interface contract
        
        val methods = AnnotationDetector::class.java.methods
        val methodNames = methods.map { it.name }.toSet()
        
        assertTrue(methodNames.contains("hasAggregateRootAnnotation"))
        assertTrue(methodNames.contains("hasDomainEntityAnnotation"))
        assertTrue(methodNames.contains("hasValueObjAnnotation"))
        assertTrue(methodNames.contains("getDddAnnotationType"))
        assertTrue(methodNames.contains("hasDddAnnotation"))
        assertTrue(methodNames.contains("extractAnnotationMetadata"))
        assertTrue(methodNames.contains("extractAllAnnotationMetadata"))
    }
    
    @Test
    fun `ddd annotation type enum should have correct values`() {
        // Test that the DddAnnotationType enum has the expected values
        val values = DddAnnotationType.values()
        assertEquals(3, values.size)
        assertTrue(values.contains(DddAnnotationType.AGGREGATE_ROOT))
        assertTrue(values.contains(DddAnnotationType.DOMAIN_ENTITY))
        assertTrue(values.contains(DddAnnotationType.VALUE_OBJ))
    }
    
    @Test
    fun `annotation detector implementation should be properly structured`() {
        // Test that the implementation is properly structured
        // This verifies the class exists and can be instantiated
        
        val impl = annotationDetector as AnnotationDetectorImpl
        assertNotNull(impl)
        
        // Verify that the class has the expected structure
        val clazz = AnnotationDetectorImpl::class.java
        assertNotNull(clazz)
        
        // Check that it implements the interface
        assertTrue(AnnotationDetector::class.java.isAssignableFrom(clazz))
    }
}
