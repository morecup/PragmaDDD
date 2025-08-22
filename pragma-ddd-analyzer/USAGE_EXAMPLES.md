# Pragma DDD Analyzer - Usage Examples

This document provides comprehensive examples of how to use the Pragma DDD Analyzer in various scenarios.

## Basic Usage

### Simple Project Setup

```kotlin
// build.gradle.kts
plugins {
    kotlin("jvm")
    id("org.morecup.pragmaddd.analyzer") version "0.0.1"
}

// Use default configuration
pragmaDddAnalyzer {
    // All defaults are fine for most projects
}
```

### DDD Annotated Classes

```kotlin
// Domain classes with DDD annotations
@AggregateRoot
data class User(
    private val id: UserId,
    private var name: String,
    private var email: Email
) {
    /**
     * Updates the user's profile information
     * @param newName The new name to set
     * @param newEmail The new email to set
     */
    fun updateProfile(newName: String, newEmail: String) {
        this.name = newName
        this.email = Email(newEmail)
    }
    
    fun getName(): String = name
    fun getEmail(): String = email.value
}

@DomainEntity
class Order(
    private val id: OrderId,
    private val customerId: UserId
) {
    private val items = mutableListOf<OrderItem>()
    
    fun addItem(item: OrderItem) {
        items.add(item)
    }
    
    fun getTotalAmount(): BigDecimal {
        return items.sumOf { it.price }
    }
}

@ValueObj
data class Email(val value: String) {
    init {
        require(value.contains("@")) { "Invalid email format" }
    }
}
```

## Configuration Examples

### Development Environment

```kotlin
// build.gradle.kts - Full analysis for development
pragmaDddAnalyzer {
    outputDirectory.set("build/generated/ddd-analysis")
    includeTestSources.set(true)
    jsonFileNaming.set("${project.name}-ddd-analysis")
    
    // Enable all analysis features
    enableMethodAnalysis.set(true)
    enablePropertyAnalysis.set(true)
    enableDocumentationExtraction.set(true)
    
    // Generous limits for development
    maxClassesPerCompilation.set(1000)
    failOnAnalysisErrors.set(false) // Don't fail build on analysis errors
}
```

### Production/CI Environment

```kotlin
// build.gradle.kts - Optimized for CI/CD
pragmaDddAnalyzer {
    outputDirectory.set("build/resources/main") // Package with JAR
    includeTestSources.set(false) // Skip test analysis in production
    jsonFileNaming.set("ddd-metadata")
    
    // Minimal analysis for faster builds
    enableMethodAnalysis.set(false)
    enablePropertyAnalysis.set(false)
    enableDocumentationExtraction.set(false)
    
    // Conservative limits
    maxClassesPerCompilation.set(500)
    failOnAnalysisErrors.set(true) // Fail fast on errors
}
```

### Large Project Optimization

```kotlin
// build.gradle.kts - For projects with 1000+ classes
pragmaDddAnalyzer {
    // Reduce memory usage
    maxClassesPerCompilation.set(200)
    
    // Disable expensive analysis
    enableMethodAnalysis.set(false)
    enablePropertyAnalysis.set(false)
    
    // Keep documentation for API generation
    enableDocumentationExtraction.set(true)
    
    // Custom output location
    outputDirectory.set("build/ddd-metadata")
    jsonFileNaming.set("domain-model")
}
```

## Multi-Module Project Setup

### Root Project Configuration

```kotlin
// settings.gradle.kts
include(":domain-core")
include(":domain-user")
include(":domain-order")
include(":application")
include(":infrastructure")

// build.gradle.kts (root)
plugins {
    kotlin("jvm") version "1.9.24" apply false
    id("org.morecup.pragmaddd.analyzer") version "0.0.1" apply false
}

subprojects {
    apply(plugin = "kotlin")
    
    // Only apply DDD analyzer to domain modules
    if (name.startsWith("domain-")) {
        apply(plugin = "org.morecup.pragmaddd.analyzer")
        
        configure<PragmaDddAnalyzerExtension> {
            jsonFileNaming.set("${project.name}-ddd")
            maxClassesPerCompilation.set(100) // Smaller modules
        }
    }
}
```

### Domain Module Configuration

```kotlin
// domain-user/build.gradle.kts
dependencies {
    implementation(project(":domain-core"))
}

pragmaDddAnalyzer {
    // Module-specific configuration
    jsonFileNaming.set("user-domain")
    enableMethodAnalysis.set(true) // Detailed analysis for core domain
}
```

```kotlin
// domain-user/src/main/kotlin/User.kt
@AggregateRoot
class User(
    private val id: UserId,
    private var profile: UserProfile,
    private var preferences: UserPreferences
) {
    fun updateProfile(newProfile: UserProfile) {
        // Business logic here
        this.profile = newProfile
        // Emit domain event
        DomainEvents.raise(UserProfileUpdated(id, newProfile))
    }
}

@DomainEntity
class UserProfile(
    private var firstName: String,
    private var lastName: String,
    private var email: Email
) {
    fun getFullName(): String = "$firstName $lastName"
}
```

## Generated JSON Output Examples

### Simple Class Analysis

```json
{
  "generatedAt": "2024-01-15T10:30:00Z",
  "sourceType": "main",
  "classes": [
    {
      "className": "User",
      "packageName": "com.example.domain",
      "annotationType": "AGGREGATE_ROOT",
      "documentation": {
        "summary": "User aggregate root",
        "description": "Represents a user in the system with profile management capabilities",
        "parameters": {},
        "returnDescription": null
      },
      "annotations": [
        {
          "name": "AggregateRoot",
          "parameters": {}
        }
      ],
      "properties": [
        {
          "name": "id",
          "type": "UserId",
          "isPrivate": true,
          "isMutable": false,
          "documentation": null,
          "annotations": []
        },
        {
          "name": "name",
          "type": "String",
          "isPrivate": true,
          "isMutable": true,
          "documentation": null,
          "annotations": []
        }
      ],
      "methods": [
        {
          "name": "updateProfile",
          "parameters": [
            {
              "name": "newName",
              "type": "String"
            },
            {
              "name": "newEmail",
              "type": "String"
            }
          ],
          "returnType": "Unit",
          "isPrivate": false,
          "documentation": {
            "summary": "Updates the user's profile information",
            "description": null,
            "parameters": {
              "newName": "The new name to set",
              "newEmail": "The new email to set"
            },
            "returnDescription": null
          },
          "annotations": [],
          "methodCalls": [
            {
              "targetMethod": "Email",
              "receiverType": null,
              "parameters": ["newEmail"]
            }
          ],
          "propertyAccesses": [
            {
              "propertyName": "name",
              "accessType": "WRITE",
              "ownerClass": "com.example.domain.User"
            },
            {
              "propertyName": "email",
              "accessType": "WRITE",
              "ownerClass": "com.example.domain.User"
            }
          ]
        }
      ]
    }
  ]
}
```

## Integration Examples

### Runtime Metadata Access

```kotlin
// Accessing generated metadata at runtime
class DomainModelInspector {
    fun loadDomainMetadata(): List<ClassMetadata> {
        val inputStream = javaClass.classLoader
            .getResourceAsStream("META-INF/ddd-analysis/ddd-analysis-main.json")
            ?: throw IllegalStateException("DDD metadata not found")
        
        val objectMapper = ObjectMapper()
        val jsonNode = objectMapper.readTree(inputStream)
        
        return jsonNode["classes"].map { classNode ->
            objectMapper.treeToValue(classNode, ClassMetadata::class.java)
        }
    }
    
    fun findAggregateRoots(): List<ClassMetadata> {
        return loadDomainMetadata().filter { 
            it.annotationType == "AGGREGATE_ROOT" 
        }
    }
    
    fun analyzeMethodComplexity(): Map<String, Int> {
        return loadDomainMetadata().associate { classMetadata ->
            val className = "${classMetadata.packageName}.${classMetadata.className}"
            val methodCount = classMetadata.methods.size
            className to methodCount
        }
    }
}
```

### API Documentation Generation

```kotlin
// Generate API documentation from DDD metadata
class DomainApiDocGenerator {
    fun generateMarkdown(): String {
        val metadata = loadDomainMetadata()
        val markdown = StringBuilder()
        
        markdown.appendLine("# Domain Model API")
        markdown.appendLine()
        
        metadata.forEach { classMetadata ->
            markdown.appendLine("## ${classMetadata.className}")
            markdown.appendLine()
            
            classMetadata.documentation?.let { doc ->
                markdown.appendLine("**Description:** ${doc.summary}")
                doc.description?.let { 
                    markdown.appendLine()
                    markdown.appendLine(it)
                }
                markdown.appendLine()
            }
            
            markdown.appendLine("**Type:** ${classMetadata.annotationType}")
            markdown.appendLine()
            
            if (classMetadata.methods.isNotEmpty()) {
                markdown.appendLine("### Methods")
                markdown.appendLine()
                
                classMetadata.methods.forEach { method ->
                    val params = method.parameters.joinToString(", ") { 
                        "${it.name}: ${it.type}" 
                    }
                    markdown.appendLine("- `${method.name}($params): ${method.returnType}`")
                    
                    method.documentation?.summary?.let { summary ->
                        markdown.appendLine("  - $summary")
                    }
                }
                markdown.appendLine()
            }
        }
        
        return markdown.toString()
    }
}
```

### Testing Integration

```kotlin
// Test utilities using DDD metadata
class DomainModelTests {
    
    @Test
    fun `all aggregate roots should have proper documentation`() {
        val metadata = loadDomainMetadata()
        val aggregateRoots = metadata.filter { it.annotationType == "AGGREGATE_ROOT" }
        
        aggregateRoots.forEach { aggregate ->
            assertThat(aggregate.documentation?.summary)
                .describedAs("Aggregate ${aggregate.className} should have documentation")
                .isNotNull()
        }
    }
    
    @Test
    fun `domain entities should not have public setters`() {
        val metadata = loadDomainMetadata()
        val entities = metadata.filter { it.annotationType == "DOMAIN_ENTITY" }
        
        entities.forEach { entity ->
            val setterMethods = entity.methods.filter { 
                it.name.startsWith("set") && !it.isPrivate 
            }
            
            assertThat(setterMethods)
                .describedAs("Entity ${entity.className} should not have public setters")
                .isEmpty()
        }
    }
    
    @Test
    fun `value objects should be immutable`() {
        val metadata = loadDomainMetadata()
        val valueObjects = metadata.filter { it.annotationType == "VALUE_OBJ" }
        
        valueObjects.forEach { valueObj ->
            val mutableProperties = valueObj.properties.filter { it.isMutable }
            
            assertThat(mutableProperties)
                .describedAs("Value object ${valueObj.className} should be immutable")
                .isEmpty()
        }
    }
}
```

## Advanced Usage Patterns

### Conditional Analysis

```kotlin
// build.gradle.kts - Environment-specific configuration
val isCI = System.getenv("CI") == "true"
val isDevelopment = !isCI

pragmaDddAnalyzer {
    enableMethodAnalysis.set(isDevelopment)
    enablePropertyAnalysis.set(isDevelopment)
    enableDocumentationExtraction.set(true) // Always extract docs
    
    maxClassesPerCompilation.set(if (isCI) 200 else 1000)
    failOnAnalysisErrors.set(isCI)
}
```

### Custom Output Processing

```kotlin
// Custom Gradle task to process generated metadata
tasks.register("processDddMetadata") {
    dependsOn("compileKotlin")
    
    doLast {
        val metadataDir = file("build/resources/main/META-INF/ddd-analysis")
        if (metadataDir.exists()) {
            metadataDir.listFiles()?.forEach { file ->
                if (file.extension == "json") {
                    println("Processing DDD metadata: ${file.name}")
                    // Custom processing logic here
                }
            }
        }
    }
}
```

### Plugin Extension

```kotlin
// Custom extension for additional configuration
open class CustomDddAnalyzerExtension : PragmaDddAnalyzerExtension() {
    
    @get:Input
    val generateApiDocs = objects.property<Boolean>().convention(false)
    
    @get:Input
    val customOutputFormat = objects.property<String>().convention("json")
    
    fun configureForEnvironment(env: String) {
        when (env) {
            "dev" -> {
                enableMethodAnalysis.set(true)
                enablePropertyAnalysis.set(true)
                generateApiDocs.set(true)
            }
            "prod" -> {
                enableMethodAnalysis.set(false)
                enablePropertyAnalysis.set(false)
                generateApiDocs.set(false)
            }
        }
    }
}
```

## Troubleshooting Examples

### Debug Configuration

```kotlin
// build.gradle.kts - Debug configuration
pragmaDddAnalyzer {
    // Enable all features for debugging
    enableMethodAnalysis.set(true)
    enablePropertyAnalysis.set(true)
    enableDocumentationExtraction.set(true)
    
    // Don't fail on errors during debugging
    failOnAnalysisErrors.set(false)
    
    // Increase limits for comprehensive analysis
    maxClassesPerCompilation.set(2000)
}

// Run with debug flag
// ./gradlew build -Dddd.analyzer.debug=true
```

### Performance Testing

```kotlin
// build.gradle.kts - Performance testing setup
tasks.register("benchmarkDddAnalysis") {
    dependsOn("clean")
    
    doFirst {
        println("Starting DDD analysis benchmark...")
        val startTime = System.currentTimeMillis()
        
        // Store start time for later use
        project.extra["benchmarkStartTime"] = startTime
    }
    
    finalizedBy("compileKotlin")
}

tasks.named("compileKotlin") {
    doLast {
        val startTime = project.extra["benchmarkStartTime"] as Long
        val duration = System.currentTimeMillis() - startTime
        println("DDD analysis completed in ${duration}ms")
    }
}
```

This comprehensive guide covers all major usage patterns and configuration options for the Pragma DDD Analyzer. Use these examples as starting points for your specific use cases.