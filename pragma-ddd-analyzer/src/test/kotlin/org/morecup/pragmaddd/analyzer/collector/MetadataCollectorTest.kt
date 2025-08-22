package org.morecup.pragmaddd.analyzer.collector

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.morecup.pragmaddd.analyzer.analyzer.ClassAnalyzer
import org.morecup.pragmaddd.analyzer.model.*

class MetadataCollectorTest {
    
    private lateinit var classAnalyzer: ClassAnalyzer
    private lateinit var metadataCollector: MetadataCollector
    
    @BeforeEach
    fun setUp() {
        classAnalyzer = TestClassAnalyzer()
        val errorReporter = org.morecup.pragmaddd.analyzer.error.SilentErrorReporter()
        metadataCollector = MetadataCollectorImpl(classAnalyzer, errorReporter)
    }
    
    // Note: collectClassMetadata tests are omitted as they require complex IrClass mocking
    // The method is simple delegation and is tested through integration tests
    
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
        assertEquals(1, metadataCollector.getTotalCount())
        assertTrue(metadataCollector.hasMetadata())
    }
    
    @Test
    fun `addToTestSources should add valid metadata to test sources`() {
        // Given
        val metadata = createSampleClassMetadata("TestClass", "com.example")
        
        // When
        metadataCollector.addToTestSources(metadata)
        
        // Then
        val testSources = metadataCollector.getTestSourcesMetadata()
        assertEquals(1, testSources.size)
        assertEquals(metadata, testSources[0])
        assertEquals(1, metadataCollector.getTotalCount())
        assertTrue(metadataCollector.hasMetadata())
    }
    
    @Test
    fun `addToMainSources should reject metadata with blank class name`() {
        // Given
        val invalidMetadata = createSampleClassMetadata("", "com.example")
        
        // When
        metadataCollector.addToMainSources(invalidMetadata)
        
        // Then
        val mainSources = metadataCollector.getMainSourcesMetadata()
        assertEquals(0, mainSources.size)
        assertEquals(0, metadataCollector.getTotalCount())
        assertFalse(metadataCollector.hasMetadata())
    }
    
    @Test
    fun `getMainSourcesMetadata should return immutable copy`() {
        // Given
        val metadata = createSampleClassMetadata("TestClass", "com.example")
        metadataCollector.addToMainSources(metadata)
        
        // When
        val mainSources1 = metadataCollector.getMainSourcesMetadata()
        val mainSources2 = metadataCollector.getMainSourcesMetadata()
        
        // Then
        assertEquals(mainSources1, mainSources2)
        assertTrue(mainSources1 !== mainSources2) // Different instances
    }
    
    @Test
    fun `getTestSourcesMetadata should return immutable copy`() {
        // Given
        val metadata = createSampleClassMetadata("TestClass", "com.example")
        metadataCollector.addToTestSources(metadata)
        
        // When
        val testSources1 = metadataCollector.getTestSourcesMetadata()
        val testSources2 = metadataCollector.getTestSourcesMetadata()
        
        // Then
        assertEquals(testSources1, testSources2)
        assertTrue(testSources1 !== testSources2) // Different instances
    }
    
    @Test
    fun `clear should remove all metadata`() {
        // Given
        val metadata1 = createSampleClassMetadata("TestClass1", "com.example")
        val metadata2 = createSampleClassMetadata("TestClass2", "com.example")
        metadataCollector.addToMainSources(metadata1)
        metadataCollector.addToTestSources(metadata2)
        
        // When
        metadataCollector.clear()
        
        // Then
        assertEquals(0, metadataCollector.getMainSourcesMetadata().size)
        assertEquals(0, metadataCollector.getTestSourcesMetadata().size)
        assertEquals(0, metadataCollector.getTotalCount())
        assertFalse(metadataCollector.hasMetadata())
    }
    
    @Test
    fun `validateMetadata should return valid result for valid metadata`() {
        // Given
        val metadata1 = createSampleClassMetadata("TestClass1", "com.example")
        val metadata2 = createSampleClassMetadata("TestClass2", "com.example.test")
        metadataCollector.addToMainSources(metadata1)
        metadataCollector.addToTestSources(metadata2)
        
        // When
        val result = metadataCollector.validateMetadata()
        
        // Then
        assertTrue(result.isValid)
        assertEquals(0, result.errors.size)
    }
    
    @Test
    fun `validateMetadata should detect duplicate classes in same source`() {
        // Given
        val metadata1 = createSampleClassMetadata("TestClass", "com.example")
        val metadata2 = createSampleClassMetadata("TestClass", "com.example")
        
        // Force add duplicates by creating a new collector that doesn't validate on add
        val errorReporter = org.morecup.pragmaddd.analyzer.error.SilentErrorReporter()
        val collector = MetadataCollectorImpl(classAnalyzer, errorReporter)
        // Use reflection or create a test-specific method to bypass validation
        collector.addToMainSources(metadata1)
        // Manually add to internal list to simulate duplicate
        val field = collector.javaClass.getDeclaredField("mainSourcesMetadata")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val list = field.get(collector) as MutableList<ClassMetadata>
        list.add(metadata2)
        
        // When
        val result = collector.validateMetadata()
        
        // Then
        assertFalse(result.isValid)
        assertEquals(1, result.errors.size)
        assertEquals(ValidationErrorType.DUPLICATE_CLASS, result.errors[0].type)
    }
    
    @Test
    fun `validateMetadata should detect cross-source duplicates`() {
        // Given
        val metadata1 = createSampleClassMetadata("TestClass", "com.example")
        val metadata2 = createSampleClassMetadata("TestClass", "com.example")
        
        // Force add to both sources
        val errorReporter = org.morecup.pragmaddd.analyzer.error.SilentErrorReporter()
        val collector = MetadataCollectorImpl(classAnalyzer, errorReporter)
        collector.addToMainSources(metadata1)
        
        // Manually add to test sources to simulate cross-source duplicate
        val field = collector.javaClass.getDeclaredField("testSourcesMetadata")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val list = field.get(collector) as MutableList<ClassMetadata>
        list.add(metadata2)
        
        // When
        val result = collector.validateMetadata()
        
        // Then
        assertFalse(result.isValid)
        assertEquals(1, result.errors.size)
        assertEquals(ValidationErrorType.DUPLICATE_CLASS, result.errors[0].type)
        assertTrue(result.errors[0].message.contains("exists in both main and test sources"))
    }
    
    @Test
    fun `validateMetadata should detect invalid package names`() {
        // Given
        val invalidMetadata = createSampleClassMetadata("TestClass", "Com.Example") // Invalid: starts with capital
        
        // Force add invalid metadata
        val errorReporter = org.morecup.pragmaddd.analyzer.error.SilentErrorReporter()
        val collector = MetadataCollectorImpl(classAnalyzer, errorReporter)
        val field = collector.javaClass.getDeclaredField("mainSourcesMetadata")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val list = field.get(collector) as MutableList<ClassMetadata>
        list.add(invalidMetadata)
        
        // When
        val result = collector.validateMetadata()
        
        // Then
        assertFalse(result.isValid)
        assertEquals(1, result.errors.size)
        assertEquals(ValidationErrorType.INVALID_PACKAGE_NAME, result.errors[0].type)
    }
    
    @Test
    fun `validateMetadata should generate warnings for missing documentation`() {
        // Given
        val metadataWithoutDocs = createSampleClassMetadata("TestClass", "com.example", hasDocumentation = false)
        metadataCollector.addToMainSources(metadataWithoutDocs)
        
        // When
        val result = metadataCollector.validateMetadata()
        
        // Then
        assertTrue(result.isValid) // Warnings don't make it invalid
        assertEquals(0, result.errors.size)
        assertTrue(result.warnings.any { it.type == ValidationWarningType.MISSING_DOCUMENTATION })
    }
    
    @Test
    fun `validateMetadata should generate warnings for empty method lists`() {
        // Given
        val metadataWithoutMethods = createSampleClassMetadata("TestClass", "com.example", hasMethods = false)
        metadataCollector.addToMainSources(metadataWithoutMethods)
        
        // When
        val result = metadataCollector.validateMetadata()
        
        // Then
        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.type == ValidationWarningType.EMPTY_METHOD_LIST })
    }
    
    @Test
    fun `validateMetadata should generate warnings for empty property lists`() {
        // Given
        val metadataWithoutProperties = createSampleClassMetadata("TestClass", "com.example", hasProperties = false)
        metadataCollector.addToMainSources(metadataWithoutProperties)
        
        // When
        val result = metadataCollector.validateMetadata()
        
        // Then
        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.type == ValidationWarningType.EMPTY_PROPERTY_LIST })
    }
    
    @Test
    fun `aggregateMetadata should merge metadata from another collector`() {
        // Given
        val metadata1 = createSampleClassMetadata("TestClass1", "com.example")
        val metadata2 = createSampleClassMetadata("TestClass2", "com.example")
        val metadata3 = createSampleClassMetadata("TestClass3", "com.example")
        val metadata4 = createSampleClassMetadata("TestClass4", "com.example")
        
        metadataCollector.addToMainSources(metadata1)
        metadataCollector.addToTestSources(metadata2)
        
        val errorReporter2 = org.morecup.pragmaddd.analyzer.error.SilentErrorReporter()
        val otherCollector = MetadataCollectorImpl(classAnalyzer, errorReporter2)
        otherCollector.addToMainSources(metadata3)
        otherCollector.addToTestSources(metadata4)
        
        // When
        metadataCollector.aggregateMetadata(otherCollector)
        
        // Then
        assertEquals(2, metadataCollector.getMainSourcesMetadata().size)
        assertEquals(2, metadataCollector.getTestSourcesMetadata().size)
        assertEquals(4, metadataCollector.getTotalCount())
        
        val mainSources = metadataCollector.getMainSourcesMetadata()
        assertTrue(mainSources.contains(metadata1))
        assertTrue(mainSources.contains(metadata3))
        
        val testSources = metadataCollector.getTestSourcesMetadata()
        assertTrue(testSources.contains(metadata2))
        assertTrue(testSources.contains(metadata4))
    }
    
    @Test
    fun `getTotalCount should return sum of main and test sources`() {
        // Given
        val metadata1 = createSampleClassMetadata("TestClass1", "com.example")
        val metadata2 = createSampleClassMetadata("TestClass2", "com.example")
        val metadata3 = createSampleClassMetadata("TestClass3", "com.example")
        
        // When
        metadataCollector.addToMainSources(metadata1)
        metadataCollector.addToMainSources(metadata2)
        metadataCollector.addToTestSources(metadata3)
        
        // Then
        assertEquals(3, metadataCollector.getTotalCount())
    }
    
    @Test
    fun `hasMetadata should return false when no metadata collected`() {
        // When/Then
        assertFalse(metadataCollector.hasMetadata())
    }
    
    @Test
    fun `hasMetadata should return true when main sources have metadata`() {
        // Given
        val metadata = createSampleClassMetadata("TestClass", "com.example")
        
        // When
        metadataCollector.addToMainSources(metadata)
        
        // Then
        assertTrue(metadataCollector.hasMetadata())
    }
    
    @Test
    fun `hasMetadata should return true when test sources have metadata`() {
        // Given
        val metadata = createSampleClassMetadata("TestClass", "com.example")
        
        // When
        metadataCollector.addToTestSources(metadata)
        
        // Then
        assertTrue(metadataCollector.hasMetadata())
    }
    
    private fun createSampleClassMetadata(
        className: String,
        packageName: String,
        hasDocumentation: Boolean = true,
        hasMethods: Boolean = true,
        hasProperties: Boolean = true
    ): ClassMetadata {
        val documentation = if (hasDocumentation) {
            DocumentationMetadata(
                summary = "Test class documentation",
                description = "Detailed description",
                parameters = emptyMap(),
                returnDescription = null
            )
        } else null
        
        val methods = if (hasMethods) {
            listOf(
                MethodMetadata(
                    name = "testMethod",
                    parameters = emptyList(),
                    returnType = "Unit",
                    isPrivate = false,
                    methodCalls = emptyList(),
                    propertyAccesses = emptyList(),
                    documentation = null,
                    annotations = emptyList()
                )
            )
        } else emptyList()
        
        val properties = if (hasProperties) {
            listOf(
                PropertyMetadata(
                    name = "testProperty",
                    type = "String",
                    isPrivate = false,
                    isMutable = true,
                    documentation = null,
                    annotations = emptyList()
                )
            )
        } else emptyList()
        
        return ClassMetadata(
            className = className,
            packageName = packageName,
            annotationType = DddAnnotationType.AGGREGATE_ROOT,
            properties = properties,
            methods = methods,
            documentation = documentation,
            annotations = emptyList()
        )
    }
    
    /**
     * Test implementation of ClassAnalyzer for testing purposes
     */
    private class TestClassAnalyzer : ClassAnalyzer {
        private var returnValue: ClassMetadata? = null
        
        fun setReturnValue(value: ClassMetadata?) {
            returnValue = value
        }
        
        override fun analyzeClass(irClass: IrClass): ClassMetadata? {
            return returnValue
        }
        
        override fun extractProperties(irClass: IrClass): List<PropertyMetadata> {
            return emptyList()
        }
        
        override fun extractMethods(irClass: IrClass): List<MethodMetadata> {
            return emptyList()
        }
        
        override fun extractDocumentation(irClass: IrClass): DocumentationMetadata? {
            return null
        }
    }
}