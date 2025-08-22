package org.morecup.pragmaddd.analyzer.model

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue

class MetadataModelsTest {

    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
    }

    @Test
    fun `should serialize and deserialize ClassMetadata correctly`() {
        // Given
        val classMetadata = ClassMetadata(
            className = "com.example.User",
            packageName = "com.example",
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
                    name = "updateProfile",
                    parameters = listOf(
                        ParameterMetadata(
                            name = "newName",
                            type = "String",
                            annotations = emptyList()
                        )
                    ),
                    returnType = "Unit",
                    isPrivate = false,
                    methodCalls = emptyList(),
                    propertyAccesses = emptyList(),
                    documentation = null,
                    annotations = emptyList()
                )
            ),
            documentation = DocumentationMetadata(
                summary = "User aggregate root",
                description = "Represents a user in the system",
                parameters = emptyMap(),
                returnDescription = null
            ),
            annotations = listOf(
                AnnotationMetadata(
                    name = "AggregateRoot",
                    parameters = emptyMap()
                )
            )
        )

        // When
        val json = objectMapper.writeValueAsString(classMetadata)
        val deserializedMetadata = objectMapper.readValue<ClassMetadata>(json)

        // Then
        assertEquals(classMetadata, deserializedMetadata)
        assertTrue(json.contains("\"className\":\"com.example.User\""))
        assertTrue(json.contains("\"annotationType\":\"AGGREGATE_ROOT\""))
    }

    @Test
    fun `should serialize PropertyMetadata with all fields`() {
        // Given
        val propertyMetadata = PropertyMetadata(
            name = "email",
            type = "String",
            isPrivate = false,
            isMutable = true,
            documentation = DocumentationMetadata(
                summary = "User email address",
                description = "The primary email for the user",
                parameters = emptyMap(),
                returnDescription = null
            ),
            annotations = listOf(
                AnnotationMetadata(
                    name = "NotNull",
                    parameters = emptyMap()
                )
            )
        )

        // When
        val json = objectMapper.writeValueAsString(propertyMetadata)
        val deserializedMetadata = objectMapper.readValue<PropertyMetadata>(json)

        // Then
        assertEquals(propertyMetadata, deserializedMetadata)
        assertTrue(json.contains("\"name\":\"email\""))
        assertTrue(json.contains("\"isMutable\":true"))
    }

    @Test
    fun `should serialize MethodMetadata with method calls and property accesses`() {
        // Given
        val methodMetadata = MethodMetadata(
            name = "validateAndUpdate",
            parameters = listOf(
                ParameterMetadata(
                    name = "newValue",
                    type = "String",
                    annotations = listOf(
                        AnnotationMetadata(
                            name = "NotBlank",
                            parameters = mapOf("message" to "Value cannot be blank")
                        )
                    )
                )
            ),
            returnType = "Boolean",
            isPrivate = true,
            methodCalls = listOf(
                MethodCallMetadata(
                    targetMethod = "isNotBlank",
                    receiverType = "String",
                    parameters = listOf()
                ),
                MethodCallMetadata(
                    targetMethod = "validate",
                    receiverType = "com.example.Validator",
                    parameters = listOf("String")
                )
            ),
            propertyAccesses = listOf(
                PropertyAccessMetadata(
                    propertyName = "value",
                    accessType = PropertyAccessType.READ,
                    ownerClass = "com.example.User"
                ),
                PropertyAccessMetadata(
                    propertyName = "value",
                    accessType = PropertyAccessType.WRITE,
                    ownerClass = "com.example.User"
                )
            ),
            documentation = DocumentationMetadata(
                summary = "Validates and updates the value",
                description = "Performs validation before updating the internal value",
                parameters = mapOf("newValue" to "The new value to set"),
                returnDescription = "true if validation passed and update succeeded"
            ),
            annotations = listOf(
                AnnotationMetadata(
                    name = "Transactional",
                    parameters = mapOf("readOnly" to false)
                )
            )
        )

        // When
        val json = objectMapper.writeValueAsString(methodMetadata)
        val deserializedMetadata = objectMapper.readValue<MethodMetadata>(json)

        // Then
        assertEquals(methodMetadata, deserializedMetadata)
        assertTrue(json.contains("\"methodCalls\""))
        assertTrue(json.contains("\"propertyAccesses\""))
        assertTrue(json.contains("\"targetMethod\":\"isNotBlank\""))
        assertTrue(json.contains("\"accessType\":\"READ\""))
    }

    @Test
    fun `should serialize MethodCallMetadata correctly`() {
        // Given
        val methodCallMetadata = MethodCallMetadata(
            targetMethod = "calculateTotal",
            receiverType = "com.example.Calculator",
            parameters = listOf("BigDecimal", "Int")
        )

        // When
        val json = objectMapper.writeValueAsString(methodCallMetadata)
        val deserializedMetadata = objectMapper.readValue<MethodCallMetadata>(json)

        // Then
        assertEquals(methodCallMetadata, deserializedMetadata)
        assertTrue(json.contains("\"targetMethod\":\"calculateTotal\""))
        assertTrue(json.contains("\"receiverType\":\"com.example.Calculator\""))
    }

    @Test
    fun `should serialize PropertyAccessMetadata correctly`() {
        // Given
        val propertyAccessMetadata = PropertyAccessMetadata(
            propertyName = "balance",
            accessType = PropertyAccessType.WRITE,
            ownerClass = "com.example.Account"
        )

        // When
        val json = objectMapper.writeValueAsString(propertyAccessMetadata)
        val deserializedMetadata = objectMapper.readValue<PropertyAccessMetadata>(json)

        // Then
        assertEquals(propertyAccessMetadata, deserializedMetadata)
        assertTrue(json.contains("\"propertyName\":\"balance\""))
        assertTrue(json.contains("\"accessType\":\"WRITE\""))
    }

    @Test
    fun `should serialize DocumentationMetadata with all fields`() {
        // Given
        val documentationMetadata = DocumentationMetadata(
            summary = "Process payment",
            description = "Processes a payment transaction with validation and logging",
            parameters = mapOf(
                "amount" to "The payment amount",
                "currency" to "The currency code"
            ),
            returnDescription = "Payment result with transaction ID"
        )

        // When
        val json = objectMapper.writeValueAsString(documentationMetadata)
        val deserializedMetadata = objectMapper.readValue<DocumentationMetadata>(json)

        // Then
        assertEquals(documentationMetadata, deserializedMetadata)
        assertTrue(json.contains("\"summary\":\"Process payment\""))
        assertTrue(json.contains("\"parameters\""))
    }

    @Test
    fun `should serialize AnnotationMetadata with parameters`() {
        // Given
        val annotationMetadata = AnnotationMetadata(
            name = "Validated",
            parameters = mapOf(
                "groups" to listOf("Create", "Update"),
                "message" to "Validation failed",
                "enabled" to true,
                "priority" to 1
            )
        )

        // When
        val json = objectMapper.writeValueAsString(annotationMetadata)
        val deserializedMetadata = objectMapper.readValue<AnnotationMetadata>(json)

        // Then
        assertEquals(annotationMetadata, deserializedMetadata)
        assertTrue(json.contains("\"name\":\"Validated\""))
        assertTrue(json.contains("\"parameters\""))
    }

    @Test
    fun `should serialize AnalysisResult correctly`() {
        // Given
        val analysisResult = AnalysisResult(
            generatedAt = "2024-01-15T10:30:00Z",
            sourceType = "main",
            classes = listOf(
                ClassMetadata(
                    className = "com.example.Order",
                    packageName = "com.example",
                    annotationType = DddAnnotationType.DOMAIN_ENTITY,
                    properties = emptyList(),
                    methods = emptyList(),
                    documentation = null,
                    annotations = emptyList()
                )
            )
        )

        // When
        val json = objectMapper.writeValueAsString(analysisResult)
        val deserializedResult = objectMapper.readValue<AnalysisResult>(json)

        // Then
        assertEquals(analysisResult, deserializedResult)
        assertTrue(json.contains("\"generatedAt\":\"2024-01-15T10:30:00Z\""))
        assertTrue(json.contains("\"sourceType\":\"main\""))
        assertTrue(json.contains("\"classes\""))
    }

    @Test
    fun `should handle null values correctly with JsonInclude annotation`() {
        // Given
        val classMetadata = ClassMetadata(
            className = "com.example.Simple",
            packageName = "com.example",
            annotationType = DddAnnotationType.VALUE_OBJ,
            properties = emptyList(),
            methods = emptyList(),
            documentation = null, // This should not appear in JSON
            annotations = emptyList()
        )

        // When
        val json = objectMapper.writeValueAsString(classMetadata)

        // Then
        assertTrue(json.contains("\"className\":\"com.example.Simple\""))
        // Should not contain documentation field since it's null
        assertTrue(!json.contains("\"documentation\":null"))
    }

    @Test
    fun `should serialize all DddAnnotationType enum values`() {
        // Given
        val aggregateRoot = DddAnnotationType.AGGREGATE_ROOT
        val domainEntity = DddAnnotationType.DOMAIN_ENTITY
        val valueObj = DddAnnotationType.VALUE_OBJ

        // When & Then
        assertEquals("AGGREGATE_ROOT", objectMapper.writeValueAsString(aggregateRoot).trim('"'))
        assertEquals("DOMAIN_ENTITY", objectMapper.writeValueAsString(domainEntity).trim('"'))
        assertEquals("VALUE_OBJ", objectMapper.writeValueAsString(valueObj).trim('"'))
    }

    @Test
    fun `should serialize all PropertyAccessType enum values`() {
        // Given
        val read = PropertyAccessType.READ
        val write = PropertyAccessType.WRITE

        // When & Then
        assertEquals("READ", objectMapper.writeValueAsString(read).trim('"'))
        assertEquals("WRITE", objectMapper.writeValueAsString(write).trim('"'))
    }

    @Test
    fun `should deserialize enum values from JSON strings`() {
        // Given
        val aggregateRootJson = "\"AGGREGATE_ROOT\""
        val readAccessJson = "\"READ\""

        // When
        val annotationType = objectMapper.readValue<DddAnnotationType>(aggregateRootJson)
        val accessType = objectMapper.readValue<PropertyAccessType>(readAccessJson)

        // Then
        assertEquals(DddAnnotationType.AGGREGATE_ROOT, annotationType)
        assertEquals(PropertyAccessType.READ, accessType)
    }

    @Test
    fun `should handle empty collections correctly`() {
        // Given
        val methodMetadata = MethodMetadata(
            name = "simpleMethod",
            parameters = emptyList(),
            returnType = "Unit",
            isPrivate = false,
            methodCalls = emptyList(),
            propertyAccesses = emptyList(),
            documentation = null,
            annotations = emptyList()
        )

        // When
        val json = objectMapper.writeValueAsString(methodMetadata)
        val deserializedMetadata = objectMapper.readValue<MethodMetadata>(json)

        // Then
        assertEquals(methodMetadata, deserializedMetadata)
        assertTrue(json.contains("\"parameters\":[]"))
        assertTrue(json.contains("\"methodCalls\":[]"))
        assertTrue(json.contains("\"propertyAccesses\":[]"))
        assertTrue(json.contains("\"annotations\":[]"))
    }

    @Test
    fun `should validate data model structure integrity`() {
        // Given - Create a complex nested structure
        val complexClassMetadata = ClassMetadata(
            className = "com.example.ComplexAggregate",
            packageName = "com.example",
            annotationType = DddAnnotationType.AGGREGATE_ROOT,
            properties = listOf(
                PropertyMetadata(
                    name = "id",
                    type = "UUID",
                    isPrivate = true,
                    isMutable = false,
                    documentation = DocumentationMetadata(
                        summary = "Unique identifier",
                        description = null,
                        parameters = emptyMap(),
                        returnDescription = null
                    ),
                    annotations = listOf(
                        AnnotationMetadata(
                            name = "Id",
                            parameters = emptyMap()
                        )
                    )
                )
            ),
            methods = listOf(
                MethodMetadata(
                    name = "processCommand",
                    parameters = listOf(
                        ParameterMetadata(
                            name = "command",
                            type = "Command",
                            annotations = listOf(
                                AnnotationMetadata(
                                    name = "Valid",
                                    parameters = emptyMap()
                                )
                            )
                        )
                    ),
                    returnType = "Result",
                    isPrivate = false,
                    methodCalls = listOf(
                        MethodCallMetadata(
                            targetMethod = "validate",
                            receiverType = "Command",
                            parameters = listOf()
                        )
                    ),
                    propertyAccesses = listOf(
                        PropertyAccessMetadata(
                            propertyName = "state",
                            accessType = PropertyAccessType.READ,
                            ownerClass = "com.example.ComplexAggregate"
                        )
                    ),
                    documentation = DocumentationMetadata(
                        summary = "Processes a command",
                        description = "Validates and processes the given command",
                        parameters = mapOf("command" to "The command to process"),
                        returnDescription = "Processing result"
                    ),
                    annotations = listOf(
                        AnnotationMetadata(
                            name = "Transactional",
                            parameters = mapOf("isolation" to "READ_COMMITTED")
                        )
                    )
                )
            ),
            documentation = DocumentationMetadata(
                summary = "Complex aggregate root",
                description = "A complex aggregate demonstrating all metadata features",
                parameters = emptyMap(),
                returnDescription = null
            ),
            annotations = listOf(
                AnnotationMetadata(
                    name = "AggregateRoot",
                    parameters = mapOf("boundedContext" to "OrderManagement")
                )
            )
        )

        // When
        val json = objectMapper.writeValueAsString(complexClassMetadata)
        val deserializedMetadata = objectMapper.readValue<ClassMetadata>(json)

        // Then
        assertEquals(complexClassMetadata, deserializedMetadata)
        assertNotNull(deserializedMetadata.properties.first().documentation)
        assertNotNull(deserializedMetadata.methods.first().documentation)
        assertEquals(1, deserializedMetadata.methods.first().methodCalls.size)
        assertEquals(1, deserializedMetadata.methods.first().propertyAccesses.size)
    }
}