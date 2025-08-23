# Requirements Document

## Introduction

The current property access detection system in the Pragma DDD Analyzer has a critical issue where method calls are correctly detected (e.g., `<set-status>`, `<get-status>`), but the `propertyAccesses` array in the analysis results remains empty. This means that property read and write operations within method bodies are not being properly identified and recorded, which is essential for domain-driven design analysis.

The system needs to be enhanced to correctly detect and classify property access patterns, including direct field access, getter/setter method calls, and property access through various Kotlin language constructs.

## Requirements

### Requirement 1

**User Story:** As a domain analyst, I want the system to detect property read operations, so that I can understand which methods access which properties for reading.

#### Acceptance Criteria

1. WHEN a method contains a property read operation like `status == OrderStatus.PENDING` THEN the system SHALL record a property access with type GET for the `status` property
2. WHEN a method contains a getter method call like `getStatus()` THEN the system SHALL record a property access with type GET for the `status` property  
3. WHEN a method contains a Kotlin property access like `this.status` THEN the system SHALL record a property access with type GET for the `status` property
4. WHEN a method returns a property value directly THEN the system SHALL record a property access with type GET for that property

### Requirement 2

**User Story:** As a domain analyst, I want the system to detect property write operations, so that I can understand which methods modify which properties.

#### Acceptance Criteria

1. WHEN a method contains a property assignment like `status = OrderStatus.CONFIRMED` THEN the system SHALL record a property access with type SET for the `status` property
2. WHEN a method contains a setter method call like `setStatus(newStatus)` THEN the system SHALL record a property access with type SET for the `status` property
3. WHEN a method contains a Kotlin property setter call like `<set-status>` THEN the system SHALL record a property access with type SET for the `status` property
4. WHEN a method modifies a property through compound assignment like `totalAmount += amount` THEN the system SHALL record both GET and SET property accesses for the `totalAmount` property

### Requirement 3

**User Story:** As a domain analyst, I want the system to correctly map method calls to property accesses, so that the analysis results show meaningful property access patterns.

#### Acceptance Criteria

1. WHEN the system detects a method call with pattern `<set-propertyName>` THEN it SHALL create a PropertyAccessMetadata with accessType SET and propertyName extracted from the method name
2. WHEN the system detects a method call with pattern `<get-propertyName>` THEN it SHALL create a PropertyAccessMetadata with accessType GET and propertyName extracted from the method name
3. WHEN the system detects a Java-style getter method call like `getPropertyName()` THEN it SHALL create a PropertyAccessMetadata with accessType GET and propertyName derived from the method name
4. WHEN the system detects a Java-style setter method call like `setPropertyName(value)` THEN it SHALL create a PropertyAccessMetadata with accessType SET and propertyName derived from the method name

### Requirement 4

**User Story:** As a domain analyst, I want the system to handle complex property access scenarios, so that I can analyze sophisticated domain logic patterns.

#### Acceptance Criteria

1. WHEN a method accesses properties through method chains like `items.size` THEN the system SHALL record a property access with type GET for the `items` property
2. WHEN a method accesses properties in conditional expressions like `if (status == OrderStatus.PENDING && items.isNotEmpty())` THEN the system SHALL record property accesses for both `status` and `items` properties
3. WHEN a method accesses the same property multiple times THEN the system SHALL record each access separately but deduplicate identical access patterns
4. WHEN a method accesses properties of different classes THEN the system SHALL correctly identify the ownerClass for each property access

### Requirement 5

**User Story:** As a domain analyst, I want the system to provide accurate property access metadata, so that I can rely on the analysis results for architectural decisions.

#### Acceptance Criteria

1. WHEN the system records a property access THEN it SHALL include the correct propertyName, accessType (GET or SET), and ownerClass
2. WHEN the system processes a method with no property accesses THEN the propertyAccesses array SHALL be empty
3. WHEN the system processes a method with property accesses THEN the propertyAccesses array SHALL contain all detected accesses without duplicates
4. WHEN the system encounters an error during property access detection THEN it SHALL log the error and continue processing other methods without failing the entire analysis

### Requirement 6

**User Story:** As a developer, I want the property access detection to work consistently across different Kotlin language constructs, so that the analysis is comprehensive and reliable.

#### Acceptance Criteria

1. WHEN analyzing Kotlin data classes THEN the system SHALL detect property accesses in generated methods like equals, hashCode, and toString
2. WHEN analyzing Kotlin primary constructor properties THEN the system SHALL correctly identify property accesses in methods that use these properties
3. WHEN analyzing Kotlin backing fields THEN the system SHALL distinguish between direct field access and property access through getters/setters
4. WHEN analyzing extension functions THEN the system SHALL correctly identify property accesses on the receiver object