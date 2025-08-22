# Requirements Document

## Introduction

The pragma-ddd-analyzer is a Gradle plugin that integrates a Kotlin Compiler Plugin to analyze Domain-Driven Design (DDD) annotated classes during compilation. The plugin will scan classes annotated with @AggregateRoot, @DomainEntity, or @ValueObj and generate comprehensive JSON metadata files that capture class structure, methods, properties, documentation, and method call relationships. These JSON files will be packaged with JAR distributions and support both main and test source sets.

## Requirements

### Requirement 1

**User Story:** As a DDD framework user, I want the analyzer to automatically detect and process DDD-annotated classes during compilation, so that metadata is generated without manual intervention.

#### Acceptance Criteria

1. WHEN the Kotlin compiler processes a class annotated with @AggregateRoot THEN the analyzer SHALL capture the class metadata
2. WHEN the Kotlin compiler processes a class annotated with @DomainEntity THEN the analyzer SHALL capture the class metadata  
3. WHEN the Kotlin compiler processes a class annotated with @ValueObj THEN the analyzer SHALL capture the class metadata
4. WHEN compilation occurs THEN the analyzer SHALL only process classes with the specified DDD annotations
5. IF a class has multiple DDD annotations THEN the analyzer SHALL record all applicable annotation types

### Requirement 2

**User Story:** As a developer, I want comprehensive class metadata captured in JSON format, so that I can analyze class structure and relationships programmatically.

#### Acceptance Criteria

1. WHEN analyzing a DDD-annotated class THEN the analyzer SHALL record the fully qualified class name
2. WHEN analyzing a DDD-annotated class THEN the analyzer SHALL record all property names and their types
3. WHEN analyzing a DDD-annotated class THEN the analyzer SHALL record all method signatures including parameters and return types
4. WHEN analyzing a DDD-annotated class THEN the analyzer SHALL record all KDoc documentation comments
5. WHEN analyzing a DDD-annotated class THEN the analyzer SHALL record all annotation metadata on the class, properties, and methods
6. IF a property or method has no documentation THEN the analyzer SHALL record it as null or empty

### Requirement 3

**User Story:** As a developer, I want method call analysis captured in the metadata, so that I can understand method dependencies and property access patterns.

#### Acceptance Criteria

1. WHEN analyzing a method body THEN the analyzer SHALL record all method calls made within that method
2. WHEN analyzing a method body THEN the analyzer SHALL record all property reads performed within that method
3. WHEN analyzing a method body THEN the analyzer SHALL record all property writes performed within that method
4. WHEN recording method calls THEN the analyzer SHALL include the target method name and receiver type if available
5. WHEN recording property access THEN the analyzer SHALL include the property name and access type (read/write)
6. IF a method calls external methods outside the analyzed class THEN the analyzer SHALL record the external method references

### Requirement 4

**User Story:** As a build engineer, I want the JSON metadata files packaged with JAR distributions, so that runtime tools can access the metadata without separate deployment.

#### Acceptance Criteria

1. WHEN the build completes THEN the analyzer SHALL generate JSON files in the JAR's resources directory
2. WHEN packaging the JAR THEN the JSON metadata files SHALL be included in the final artifact
3. WHEN multiple DDD classes exist THEN the analyzer SHALL generate separate JSON files per class or a consolidated JSON structure
4. WHEN the JAR is distributed THEN the JSON metadata SHALL be accessible via classpath resource loading
5. IF the build fails THEN the analyzer SHALL not prevent compilation but SHALL log appropriate warnings

### Requirement 5

**User Story:** As a developer, I want test classes analyzed separately from main classes, so that I can distinguish between production and test metadata.

#### Acceptance Criteria

1. WHEN compiling test source sets THEN the analyzer SHALL generate separate JSON files for test classes
2. WHEN compiling main source sets THEN the analyzer SHALL generate JSON files for main classes
3. WHEN both main and test contain DDD-annotated classes THEN the analyzer SHALL maintain separate metadata files
4. WHEN packaging test JARs THEN the test JSON metadata SHALL be included in test artifacts
5. IF test classes reference main classes THEN the analyzer SHALL record cross-references appropriately

### Requirement 6

**User Story:** As a Gradle user, I want the analyzer integrated as a Gradle plugin, so that I can easily configure and apply it to my projects.

#### Acceptance Criteria

1. WHEN applying the pragma-ddd-analyzer plugin THEN it SHALL automatically register the Kotlin compiler plugin
2. WHEN the plugin is applied THEN it SHALL configure appropriate compilation tasks
3. WHEN building the project THEN the analyzer SHALL execute during the Kotlin compilation phase
4. WHEN the plugin is configured THEN it SHALL support standard Gradle configuration options
5. IF the plugin configuration is invalid THEN it SHALL provide clear error messages during build setup

### Requirement 7

**User Story:** As a developer, I want configurable output formats and locations, so that I can customize where and how metadata is generated.

#### Acceptance Criteria

1. WHEN configuring the plugin THEN I SHALL be able to specify the output directory for JSON files
2. WHEN configuring the plugin THEN I SHALL be able to specify the JSON file naming convention
3. WHEN configuring the plugin THEN I SHALL be able to enable or disable specific analysis features
4. WHEN the output directory doesn't exist THEN the analyzer SHALL create it automatically
5. IF custom configuration is provided THEN the analyzer SHALL validate configuration parameters before compilation