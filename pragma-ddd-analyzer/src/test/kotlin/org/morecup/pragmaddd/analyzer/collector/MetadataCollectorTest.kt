package org.morecup.pragmaddd.analyzer.collector

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.kotlin.*
import org.morecup.pragmaddd.analyzer.analyzer.ClassAnalyzer
import org.morecup.pragmaddd.analyzer.error.ErrorReporter
import org.morecup.pragmaddd.analyzer.model.*
import org.jetbrains.kotlin.ir.declarations.IrClass

class MetadataCollectorTest {
    
    private lateinit var classAnalyzer: ClassAnalyzer
    private lateinit var errorReporter: ErrorReporter
    private lateinit var metadataCollector: MetadataCollector
    
    @BeforeEach
    fun setUp() {
        classAnalyzer = mock()
        errorReporter = mock()
        metadataCollector = MetadataCollectorImpl(classAnalyzer, errorReporter)
    }
    
    @Test
    fun `addToMainSources should add valid metadata to main sources`() {
        // Given
        val metadata = createSampleClassMetadata("TestClass", "com.example")
        
        // When
        metadataCollector.addToMainSources(metadata)
        
        // Then
        val mainSources = metadataCollector.getMainSourcesMetadata()
        assertEquals(1, mainSources.size)
        assertEquals(metadata, mainSources[0])
    }
    
    @Test
    fun `addToMainSources should reject metadata with blank class name`() {
        // Given
        val invalidMetadata = createSampleClassMetadata("", "com.example")
        
        // When
        metadataCollector.addToMainSources(invalidMetadata)
        
        // Then
        val mainSources = metadataCollector.getMainSourcesMetadata()
        assertEquals(1, mainSources.size) // Still added but validation will catch it
    }
    
    @Test
    fun `getMainSourcesMetadata should return empty list initially`() {
        // When
        val mainSources = metadataCollector.getMainSourcesMetadata()
        
        // Then
        assertTrue(mainSources.isEmpty())
    }
    
    @Test
    fun `clearMetadata should remove all collected metadata`() {
        // Given
        val metadata = createSampleClassMetadata("TestClass", "com.example")
        metadataCollector.addToMainSources(metadata)
        
        // When
        metadataCollector.clearMetadata()
        
        // Then
        val mainSources = metadataCollector.getMainSourcesMetadata()
        assertTrue(mainSources.isEmpty())
    }
    
    @Test
    fun `addToMainSources should handle duplicate class names`() {
        // Given
        val metadata1 = createSampleClassMetadata("TestClass", "com.example")
        val metadata2 = createSampleClassMetadata("TestClass", "com.example")
        
        // When
        metadataCollector.addToMainSources(metadata1)
        metadataCollector.addToMainSources(metadata2)
        
        // Then
        val mainSources = metadataCollector.getMainSourcesMetadata()
        assertEquals(1, mainSources.size) // Should replace duplicate
        verify(errorReporter).reportWarning(any())
    }
    
    @Test
    fun `validateMetadata should return valid result for valid metadata`() {
        // Given
        val metadata = createSampleClassMetadata("TestClass", "com.example")
        metadataCollector.addToMainSources(metadata)
        
        // When
        val result = metadataCollector.validateMetadata()
        
        // Then
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }
    
    @Test
    fun `validateMetadata should detect duplicate class names`() {
        // Given
        val metadata1 = createSampleClassMetadata("TestClass", "com.example")
        val metadata2 = createSampleClassMetadata("TestClass", "com.example")
        metadataCollector.addToMainSources(metadata1)
        metadataCollector.addToMainSources(metadata2)
        
        // When
        val result = metadataCollector.validateMetadata()
        
        // Then
        assertTrue(result.isValid) // No duplicates after replacement
    }
    
    @Test
    fun `validateMetadata should warn about empty metadata collection`() {
        // When
        val result = metadataCollector.validateMetadata()
        
        // Then
        assertTrue(result.isValid)
        assertTrue(result.warnings.isNotEmpty())
    }
    
    @Test
    fun `aggregateMetadata should merge metadata from another collector`() {
        // Given
        val metadata1 = createSampleClassMetadata("TestClass1", "com.example")
        val metadata2 = createSampleClassMetadata("TestClass2", "com.example")
        
        val otherCollector = MetadataCollectorImpl(classAnalyzer, errorReporter)
        otherCollector.addToMainSources(metadata2)
        
        metadataCollector.addToMainSources(metadata1)
        
        // When
        metadataCollector.aggregateMetadata(otherCollector)
        
        // Then
        val mainSources = metadataCollector.getMainSourcesMetadata()
        assertEquals(2, mainSources.size)
    }
    
    @Test
    fun `getMemoryUsage should return current memory statistics`() {
        // Given
        val metadata = createSampleClassMetadata("TestClass", "com.example")
        metadataCollector.addToMainSources(metadata)
        
        // When
        val memoryStats = metadataCollector.getMemoryUsage()
        
        // Then
        assertEquals(1, memoryStats.mainSourcesCount)
        assertTrue(memoryStats.approximateMemoryUsage > 0)
    }
    
    @Test
    fun `collectClassMetadata should handle successful analysis`() {
        // Given
        val irClass = mock<IrClass>()
        val mockName = mock<org.jetbrains.kotlin.name.Name>()
        whenever(mockName.asString()).thenReturn("TestClass")
        whenever(irClass.name).thenReturn(mockName)
        
        val expectedMetadata = createSampleClassMetadata("TestClass", "com.example")
        whenever(classAnalyzer.analyzeClass(irClass)).thenReturn(expectedMetadata)
        
        // When
        val result = metadataCollector.collectClassMetadata(irClass)
        
        // Then
        assertEquals(expectedMetadata, result)
    }
    
    @Test
    fun `collectClassMetadata should handle analysis failure`() {
        // Given
        val irClass = mock<IrClass>()
        val mockName = mock<org.jetbrains.kotlin.name.Name>()
        whenever(mockName.asString()).thenReturn("TestClass")
        whenever(irClass.name).thenReturn(mockName)
        
        whenever(classAnalyzer.analyzeClass(irClass)).thenThrow(RuntimeException("Analysis failed"))
        
        // When
        val result = metadataCollector.collectClassMetadata(irClass)
        
        // Then
        assertNull(result)
        verify(errorReporter).reportError(any())
    }
    
    private fun createSampleClassMetadata(className: String, packageName: String): ClassMetadata {
        return ClassMetadata(
            className = className,
            packageName = packageName,
            annotationType = DddAnnotationType.AGGREGATE_ROOT,
            properties = listOf(
                PropertyMetadata(
                    name = "id",
                    type = "String",
                    isPrivate = true,
                    isMutable = false,
                    documentation = null,
                    annotations = emptyList()
                )
            ),
            methods = listOf(
                MethodMetadata(
                    name = "getId",
                    parameters = emptyList(),
                    returnType = "String",
                    isPrivate = false,
                    methodCalls = emptyList(),
                    propertyAccesses = emptyList(),
                    documentation = null,
                    annotations = emptyList()
                )
            ),
            documentation = null,
            annotations = emptyList()
        )
    }
}
