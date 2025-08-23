package org.morecup.pragmaddd.analyzer.analyzer

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.morecup.pragmaddd.analyzer.model.MethodCallMetadata
import org.morecup.pragmaddd.analyzer.model.PropertyAccessType

class MethodCallToPropertyAccessConverterTest {
    
    private val converter = MethodCallToPropertyAccessConverter()
    
    @Test
    fun `should convert Kotlin setter pattern to SET property access`() {
        // Given
        val methodCall = MethodCallMetadata(
            targetMethod = "<set-status>",
            receiverType = "com.example.Order",
            parameters = listOf("OrderStatus")
        )
        
        // When
        val result = converter.convertMethodCallsToPropertyAccess(listOf(methodCall))
        
        // Then
        assertEquals(1, result.size)
        val propertyAccess = result.first()
        assertEquals("status", propertyAccess.propertyName)
        assertEquals(PropertyAccessType.SET, propertyAccess.accessType)
        assertEquals("com.example.Order", propertyAccess.ownerClass)
    }
    
    @Test
    fun `should convert Kotlin getter pattern to GET property access`() {
        // Given
        val methodCall = MethodCallMetadata(
            targetMethod = "<get-items>",
            receiverType = "com.example.Order",
            parameters = emptyList()
        )
        
        // When
        val result = converter.convertMethodCallsToPropertyAccess(listOf(methodCall))
        
        // Then
        assertEquals(1, result.size)
        val propertyAccess = result.first()
        assertEquals("items", propertyAccess.propertyName)
        assertEquals(PropertyAccessType.GET, propertyAccess.accessType)
        assertEquals("com.example.Order", propertyAccess.ownerClass)
    }
    
    @Test
    fun `should convert Java-style getter to GET property access`() {
        // Given
        val methodCall = MethodCallMetadata(
            targetMethod = "getStatus",
            receiverType = "com.example.Order",
            parameters = emptyList()
        )
        
        // When
        val result = converter.convertMethodCallsToPropertyAccess(listOf(methodCall))
        
        // Then
        assertEquals(1, result.size)
        val propertyAccess = result.first()
        assertEquals("status", propertyAccess.propertyName)
        assertEquals(PropertyAccessType.GET, propertyAccess.accessType)
        assertEquals("com.example.Order", propertyAccess.ownerClass)
    }
    
    @Test
    fun `should convert Java-style setter to SET property access`() {
        // Given
        val methodCall = MethodCallMetadata(
            targetMethod = "setStatus",
            receiverType = "com.example.Order",
            parameters = listOf("OrderStatus")
        )
        
        // When
        val result = converter.convertMethodCallsToPropertyAccess(listOf(methodCall))
        
        // Then
        assertEquals(1, result.size)
        val propertyAccess = result.first()
        assertEquals("status", propertyAccess.propertyName)
        assertEquals(PropertyAccessType.SET, propertyAccess.accessType)
        assertEquals("com.example.Order", propertyAccess.ownerClass)
    }
    
    @Test
    fun `should handle multiple method calls with mixed patterns`() {
        // Given
        val methodCalls = listOf(
            MethodCallMetadata("<set-status>", "com.example.Order", listOf("OrderStatus")),
            MethodCallMetadata("<get-items>", "com.example.Order", emptyList()),
            MethodCallMetadata("getTotalAmount", "com.example.Order", emptyList()),
            MethodCallMetadata("setCustomerName", "com.example.Order", listOf("String"))
        )
        
        // When
        val result = converter.convertMethodCallsToPropertyAccess(methodCalls)
        
        // Then
        assertEquals(4, result.size)
        
        // Verify each conversion
        val statusSet = result.find { it.propertyName == "status" && it.accessType == PropertyAccessType.SET }
        assertNotNull(statusSet)
        
        val itemsGet = result.find { it.propertyName == "items" && it.accessType == PropertyAccessType.GET }
        assertNotNull(itemsGet)
        
        val totalAmountGet = result.find { it.propertyName == "totalAmount" && it.accessType == PropertyAccessType.GET }
        assertNotNull(totalAmountGet)
        
        val customerNameSet = result.find { it.propertyName == "customerName" && it.accessType == PropertyAccessType.SET }
        assertNotNull(customerNameSet)
    }
    
    @Test
    fun `should ignore non-property method calls`() {
        // Given
        val methodCalls = listOf(
            MethodCallMetadata("calculate", "com.example.Order", emptyList()),
            MethodCallMetadata("validate", "com.example.Order", listOf("String")),
            MethodCallMetadata("toString", "com.example.Order", emptyList())
        )
        
        // When
        val result = converter.convertMethodCallsToPropertyAccess(methodCalls)
        
        // Then
        assertEquals(0, result.size)
    }
    
    @Test
    fun `should handle null receiver type gracefully`() {
        // Given
        val methodCall = MethodCallMetadata(
            targetMethod = "<set-status>",
            receiverType = null,
            parameters = listOf("OrderStatus")
        )
        
        // When
        val result = converter.convertMethodCallsToPropertyAccess(listOf(methodCall))
        
        // Then
        assertEquals(1, result.size)
        val propertyAccess = result.first()
        assertEquals("status", propertyAccess.propertyName)
        assertEquals(PropertyAccessType.SET, propertyAccess.accessType)
        assertNull(propertyAccess.ownerClass)
    }
    
    @Test
    fun `extractPropertyNameFromKotlinSetter should extract valid property names`() {
        assertEquals("status", converter.extractPropertyNameFromKotlinSetter("<set-status>"))
        assertEquals("customerName", converter.extractPropertyNameFromKotlinSetter("<set-customerName>"))
        assertEquals("isActive", converter.extractPropertyNameFromKotlinSetter("<set-isActive>"))
    }
    
    @Test
    fun `extractPropertyNameFromKotlinSetter should return null for invalid patterns`() {
        assertNull(converter.extractPropertyNameFromKotlinSetter("setStatus"))
        assertNull(converter.extractPropertyNameFromKotlinSetter("<set->"))
        assertNull(converter.extractPropertyNameFromKotlinSetter("<set-"))
        assertNull(converter.extractPropertyNameFromKotlinSetter("set-status>"))
        assertNull(converter.extractPropertyNameFromKotlinSetter("<set-123invalid>"))
    }
    
    @Test
    fun `extractPropertyNameFromKotlinGetter should extract valid property names`() {
        assertEquals("status", converter.extractPropertyNameFromKotlinGetter("<get-status>"))
        assertEquals("customerName", converter.extractPropertyNameFromKotlinGetter("<get-customerName>"))
        assertEquals("isActive", converter.extractPropertyNameFromKotlinGetter("<get-isActive>"))
    }
    
    @Test
    fun `extractPropertyNameFromKotlinGetter should return null for invalid patterns`() {
        assertNull(converter.extractPropertyNameFromKotlinGetter("getStatus"))
        assertNull(converter.extractPropertyNameFromKotlinGetter("<get->"))
        assertNull(converter.extractPropertyNameFromKotlinGetter("<get-"))
        assertNull(converter.extractPropertyNameFromKotlinGetter("get-status>"))
        assertNull(converter.extractPropertyNameFromKotlinGetter("<get-123invalid>"))
    }
    
    @Test
    fun `extractPropertyNameFromJavaStyleGetter should extract valid property names`() {
        assertEquals("status", converter.extractPropertyNameFromJavaStyleGetter("getStatus", emptyList()))
        assertEquals("customerName", converter.extractPropertyNameFromJavaStyleGetter("getCustomerName", emptyList()))
        assertEquals("isActive", converter.extractPropertyNameFromJavaStyleGetter("getIsActive", emptyList()))
    }
    
    @Test
    fun `extractPropertyNameFromJavaStyleGetter should return null for invalid patterns`() {
        assertNull(converter.extractPropertyNameFromJavaStyleGetter("getStatus", listOf("String"))) // Has parameters
        assertNull(converter.extractPropertyNameFromJavaStyleGetter("get", emptyList())) // No property name
        assertNull(converter.extractPropertyNameFromJavaStyleGetter("status", emptyList())) // Doesn't start with get
        assertNull(converter.extractPropertyNameFromJavaStyleGetter("getstatus", emptyList())) // Lowercase after get
    }
    
    @Test
    fun `extractPropertyNameFromJavaStyleSetter should extract valid property names`() {
        assertEquals("status", converter.extractPropertyNameFromJavaStyleSetter("setStatus", listOf("OrderStatus")))
        assertEquals("customerName", converter.extractPropertyNameFromJavaStyleSetter("setCustomerName", listOf("String")))
        assertEquals("isActive", converter.extractPropertyNameFromJavaStyleSetter("setIsActive", listOf("Boolean")))
    }
    
    @Test
    fun `extractPropertyNameFromJavaStyleSetter should return null for invalid patterns`() {
        assertNull(converter.extractPropertyNameFromJavaStyleSetter("setStatus", emptyList())) // No parameters
        assertNull(converter.extractPropertyNameFromJavaStyleSetter("setStatus", listOf("String", "Int"))) // Too many parameters
        assertNull(converter.extractPropertyNameFromJavaStyleSetter("set", listOf("String"))) // No property name
        assertNull(converter.extractPropertyNameFromJavaStyleSetter("status", listOf("String"))) // Doesn't start with set
        assertNull(converter.extractPropertyNameFromJavaStyleSetter("setstatus", listOf("String"))) // Lowercase after set
    }
    
    @Test
    fun `should handle error cases gracefully and continue processing`() {
        // Given - mix of valid and potentially problematic method calls
        val methodCalls = listOf(
            MethodCallMetadata("<set-status>", "com.example.Order", listOf("OrderStatus")), // Valid
            MethodCallMetadata("<set->", "com.example.Order", listOf("String")), // Invalid Kotlin setter
            MethodCallMetadata("getCustomerName", "com.example.Order", emptyList()), // Valid Java getter
            MethodCallMetadata("", "com.example.Order", emptyList()) // Empty method name
        )
        
        // When
        val result = converter.convertMethodCallsToPropertyAccess(methodCalls)
        
        // Then - should process valid ones and skip invalid ones
        assertEquals(2, result.size)
        
        val statusSet = result.find { it.propertyName == "status" && it.accessType == PropertyAccessType.SET }
        assertNotNull(statusSet)
        
        val customerNameGet = result.find { it.propertyName == "customerName" && it.accessType == PropertyAccessType.GET }
        assertNotNull(customerNameGet)
    }
    
    @Test
    fun `should handle empty method calls list`() {
        // Given
        val methodCalls = emptyList<MethodCallMetadata>()
        
        // When
        val result = converter.convertMethodCallsToPropertyAccess(methodCalls)
        
        // Then
        assertEquals(0, result.size)
    }
    
    @Test
    fun `should handle complex property names correctly`() {
        // Given
        val methodCalls = listOf(
            MethodCallMetadata("<set-_privateField>", "com.example.Order", listOf("String")),
            MethodCallMetadata("<get-field123>", "com.example.Order", emptyList()),
            MethodCallMetadata("getXMLParser", "com.example.Order", emptyList()),
            MethodCallMetadata("setHTTPClient", "com.example.Order", listOf("HttpClient"))
        )
        
        // When
        val result = converter.convertMethodCallsToPropertyAccess(methodCalls)
        
        // Then
        assertEquals(4, result.size)
        
        val privateFieldSet = result.find { it.propertyName == "_privateField" }
        assertNotNull(privateFieldSet)
        
        val field123Get = result.find { it.propertyName == "field123" }
        assertNotNull(field123Get)
        
        val xmlParserGet = result.find { it.propertyName == "xMLParser" }
        assertNotNull(xmlParserGet)
        
        val httpClientSet = result.find { it.propertyName == "hTTPClient" }
        assertNotNull(httpClientSet)
    }
}