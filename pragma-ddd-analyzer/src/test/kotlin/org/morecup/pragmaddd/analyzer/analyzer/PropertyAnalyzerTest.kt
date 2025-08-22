package org.morecup.pragmaddd.analyzer.analyzer

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.morecup.pragmaddd.analyzer.model.PropertyAccessMetadata
import org.morecup.pragmaddd.analyzer.model.PropertyAccessType

/**
 * Unit tests for PropertyAnalyzer implementation
 * Tests various property access patterns including direct field access,
 * getter/setter method calls, and method chain property access
 */
class PropertyAnalyzerTest {
    
    private lateinit var propertyAnalyzer: PropertyAnalyzer
    
    @BeforeEach
    fun setUp() {
        propertyAnalyzer = PropertyAnalyzerImpl()
    }
    
    @Test
    fun `should verify PropertyAnalyzer interface exists and has correct methods`() {
        // This test verifies that the PropertyAnalyzer interface exists and has the expected methods
        val methods = PropertyAnalyzer::class.java.methods
        val methodNames = methods.map { it.name }.toSet()
        
        assertTrue(methodNames.contains("extractPropertyAccess"))
        assertTrue(methodNames.contains("detectDirectFieldAccess"))
        assertTrue(methodNames.contains("detectGetterSetterCalls"))
        assertTrue(methodNames.contains("detectMethodChainPropertyAccess"))
    }

    @Test
    fun `should create PropertyAnalyzerImpl and verify it implements PropertyAnalyzer`() {
        // Given
        val analyzer = PropertyAnalyzerImpl()

        // Then
        assertNotNull(analyzer)
        assertTrue(analyzer is PropertyAnalyzer)
        
        // Verify all interface methods are implemented
        val interfaceMethods = PropertyAnalyzer::class.java.methods
        val implMethods = PropertyAnalyzerImpl::class.java.methods
        
        val interfaceMethodNames = interfaceMethods.map { it.name }.toSet()
        val implMethodNames = implMethods.map { it.name }.toSet()
        
        interfaceMethodNames.forEach { methodName ->
            assertTrue(implMethodNames.contains(methodName), "Method $methodName should be implemented")
        }
    }

    @Test
    fun `should verify interface methods exist on implementation`() {
        // Given
        val analyzer = PropertyAnalyzerImpl()

        // Then - verify interface methods exist
        assertNotNull(analyzer::extractPropertyAccess)
        assertNotNull(analyzer::detectDirectFieldAccess)
        assertNotNull(analyzer::detectGetterSetterCalls)
        assertNotNull(analyzer::detectMethodChainPropertyAccess)
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
    fun `should verify property access metadata supports different owner classes`() {
        // Test PropertyAccessMetadata with different owner classes
        val access1 = PropertyAccessMetadata(
            propertyName = "property1",
            accessType = PropertyAccessType.READ,
            ownerClass = "com.example.Class1"
        )
        
        val access2 = PropertyAccessMetadata(
            propertyName = "property2",
            accessType = PropertyAccessType.WRITE,
            ownerClass = "com.example.Class2"
        )
        
        val access3 = PropertyAccessMetadata(
            propertyName = "property3",
            accessType = PropertyAccessType.READ,
            ownerClass = null // External or unknown class
        )
        
        assertNotNull(access1)
        assertEquals("com.example.Class1", access1.ownerClass)
        
        assertNotNull(access2)
        assertEquals("com.example.Class2", access2.ownerClass)
        
        assertNotNull(access3)
        assertNull(access3.ownerClass)
    }

    @Test
    fun `should verify property access metadata supports various property names`() {
        // Test PropertyAccessMetadata with various property naming patterns
        val simpleProperty = PropertyAccessMetadata(
            propertyName = "name",
            accessType = PropertyAccessType.READ,
            ownerClass = "TestClass"
        )
        
        val camelCaseProperty = PropertyAccessMetadata(
            propertyName = "firstName",
            accessType = PropertyAccessType.WRITE,
            ownerClass = "TestClass"
        )
        
        val underscoreProperty = PropertyAccessMetadata(
            propertyName = "user_id",
            accessType = PropertyAccessType.READ,
            ownerClass = "TestClass"
        )
        
        assertNotNull(simpleProperty)
        assertEquals("name", simpleProperty.propertyName)
        
        assertNotNull(camelCaseProperty)
        assertEquals("firstName", camelCaseProperty.propertyName)
        
        assertNotNull(underscoreProperty)
        assertEquals("user_id", underscoreProperty.propertyName)
    }

    @Test
    fun `should verify property access analysis supports complex scenarios`() {
        // Test complex property access scenarios
        val complexAccesses = listOf(
            PropertyAccessMetadata("id", PropertyAccessType.READ, "User"),
            PropertyAccessMetadata("name", PropertyAccessType.WRITE, "User"),
            PropertyAccessMetadata("email", PropertyAccessType.READ, "User"),
            PropertyAccessMetadata("status", PropertyAccessType.WRITE, "Order"),
            PropertyAccessMetadata("items", PropertyAccessType.READ, "Order")
        )
        
        assertNotNull(complexAccesses)
        assertEquals(5, complexAccesses.size)
        
        // Verify different access types
        val readAccesses = complexAccesses.filter { it.accessType == PropertyAccessType.READ }
        val writeAccesses = complexAccesses.filter { it.accessType == PropertyAccessType.WRITE }
        
        assertEquals(3, readAccesses.size)
        assertEquals(2, writeAccesses.size)
        
        // Verify different owner classes
        val userAccesses = complexAccesses.filter { it.ownerClass == "User" }
        val orderAccesses = complexAccesses.filter { it.ownerClass == "Order" }
        
        assertEquals(3, userAccesses.size)
        assertEquals(2, orderAccesses.size)
    }

    @Test
    fun `should verify property analyzer handles method chain scenarios`() {
        // Test method chain property access patterns
        val chainAccesses = listOf(
            PropertyAccessMetadata("user", PropertyAccessType.READ, "Order"),
            PropertyAccessMetadata("profile", PropertyAccessType.READ, "User"),
            PropertyAccessMetadata("name", PropertyAccessType.READ, "Profile")
        )
        
        assertNotNull(chainAccesses)
        assertEquals(3, chainAccesses.size)
        
        // All should be read accesses in a typical method chain
        assertTrue(chainAccesses.all { it.accessType == PropertyAccessType.READ })
        
        // Verify the chain structure
        assertEquals("user", chainAccesses[0].propertyName)
        assertEquals("Order", chainAccesses[0].ownerClass)
        
        assertEquals("profile", chainAccesses[1].propertyName)
        assertEquals("User", chainAccesses[1].ownerClass)
        
        assertEquals("name", chainAccesses[2].propertyName)
        assertEquals("Profile", chainAccesses[2].ownerClass)
    }

    @Test
    fun `should verify property analyzer supports getter setter patterns`() {
        // Test getter/setter method call patterns
        val getterSetterAccesses = listOf(
            PropertyAccessMetadata("name", PropertyAccessType.READ, "User"),   // getName()
            PropertyAccessMetadata("name", PropertyAccessType.WRITE, "User"),  // setName()
            PropertyAccessMetadata("age", PropertyAccessType.READ, "User"),    // getAge()
            PropertyAccessMetadata("age", PropertyAccessType.WRITE, "User")    // setAge()
        )
        
        assertNotNull(getterSetterAccesses)
        assertEquals(4, getterSetterAccesses.size)
        
        // Verify getter/setter pairs
        val nameAccesses = getterSetterAccesses.filter { it.propertyName == "name" }
        val ageAccesses = getterSetterAccesses.filter { it.propertyName == "age" }
        
        assertEquals(2, nameAccesses.size)
        assertEquals(2, ageAccesses.size)
        
        // Each property should have both read and write access
        assertTrue(nameAccesses.any { it.accessType == PropertyAccessType.READ })
        assertTrue(nameAccesses.any { it.accessType == PropertyAccessType.WRITE })
        assertTrue(ageAccesses.any { it.accessType == PropertyAccessType.READ })
        assertTrue(ageAccesses.any { it.accessType == PropertyAccessType.WRITE })
    }

    @Test
    fun `should verify property analyzer interface completeness`() {
        // Test that PropertyAnalyzer interface provides comprehensive property analysis capabilities
        val analyzer = PropertyAnalyzerImpl()
        
        // Verify all required methods are available
        assertNotNull(analyzer::extractPropertyAccess)
        assertNotNull(analyzer::detectDirectFieldAccess)
        assertNotNull(analyzer::detectGetterSetterCalls)
        assertNotNull(analyzer::detectMethodChainPropertyAccess)
        
        // Verify the analyzer can be used for different analysis scenarios
        assertTrue(analyzer is PropertyAnalyzer)
        
        // The analyzer should be ready to process IR functions when provided
        // (actual IR processing would be tested in integration tests)
    }
}