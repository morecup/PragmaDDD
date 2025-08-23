package org.morecup.pragmaddd.analyzer.analyzer

import org.morecup.pragmaddd.analyzer.model.MethodCallMetadata
import org.morecup.pragmaddd.analyzer.model.PropertyAccessMetadata
import org.morecup.pragmaddd.analyzer.model.PropertyAccessType

/**
 * Component responsible for converting method calls to property access metadata.
 * Handles pattern matching for Kotlin setter/getter method calls and Java-style
 * getter/setter method name conversion with robust error handling.
 */
class MethodCallToPropertyAccessConverter {
    
    /**
     * Converts a list of method calls to property access metadata.
     * Processes each method call and attempts to identify property access patterns.
     * 
     * @param methodCalls List of method calls to analyze
     * @return List of property access metadata extracted from method calls
     */
    fun convertMethodCallsToPropertyAccess(methodCalls: List<MethodCallMetadata>): List<PropertyAccessMetadata> {
        val propertyAccesses = mutableListOf<PropertyAccessMetadata>()
        
        methodCalls.forEach { methodCall ->
            try {
                val propertyAccess = convertSingleMethodCall(methodCall)
                if (propertyAccess != null) {
                    propertyAccesses.add(propertyAccess)
                }
            } catch (e: Exception) {
                // Log error but continue processing other method calls
                System.err.println("MethodCallToPropertyAccessConverter: Error converting method call '${methodCall.targetMethod}': ${e.message}")
            }
        }
        
        return propertyAccesses
    }
    
    /**
     * Converts a single method call to property access metadata if it matches
     * any known property access patterns.
     * 
     * @param methodCall The method call to analyze
     * @return PropertyAccessMetadata if the method call represents property access, null otherwise
     */
    private fun convertSingleMethodCall(methodCall: MethodCallMetadata): PropertyAccessMetadata? {
        val methodName = methodCall.targetMethod
        val receiverType = methodCall.receiverType
        
        // Try Kotlin setter pattern first
        extractPropertyNameFromKotlinSetter(methodName)?.let { propertyName ->
            return PropertyAccessMetadata(
                propertyName = propertyName,
                accessType = PropertyAccessType.SET,
                ownerClass = receiverType
            )
        }
        
        // Try Kotlin getter pattern
        extractPropertyNameFromKotlinGetter(methodName)?.let { propertyName ->
            return PropertyAccessMetadata(
                propertyName = propertyName,
                accessType = PropertyAccessType.GET,
                ownerClass = receiverType
            )
        }
        
        // Try Java-style getter pattern
        extractPropertyNameFromJavaStyleGetter(methodName, methodCall.parameters)?.let { propertyName ->
            return PropertyAccessMetadata(
                propertyName = propertyName,
                accessType = PropertyAccessType.GET,
                ownerClass = receiverType
            )
        }
        
        // Try Java-style setter pattern
        extractPropertyNameFromJavaStyleSetter(methodName, methodCall.parameters)?.let { propertyName ->
            return PropertyAccessMetadata(
                propertyName = propertyName,
                accessType = PropertyAccessType.SET,
                ownerClass = receiverType
            )
        }
        
        // No property access pattern matched
        return null
    }
    
    /**
     * Extracts property name from Kotlin setter method calls.
     * Matches pattern: <set-propertyName>
     * 
     * @param methodName The method name to analyze
     * @return Property name if pattern matches, null otherwise
     */
    fun extractPropertyNameFromKotlinSetter(methodName: String): String? {
        return try {
            if (methodName.startsWith("<set-") && methodName.endsWith(">")) {
                val propertyName = methodName.removePrefix("<set-").removeSuffix(">")
                if (propertyName.isNotBlank() && isValidPropertyName(propertyName)) {
                    propertyName
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            System.err.println("MethodCallToPropertyAccessConverter: Error extracting property name from Kotlin setter '$methodName': ${e.message}")
            null
        }
    }
    
    /**
     * Extracts property name from Kotlin getter method calls.
     * Matches pattern: <get-propertyName>
     * 
     * @param methodName The method name to analyze
     * @return Property name if pattern matches, null otherwise
     */
    fun extractPropertyNameFromKotlinGetter(methodName: String): String? {
        return try {
            if (methodName.startsWith("<get-") && methodName.endsWith(">")) {
                val propertyName = methodName.removePrefix("<get-").removeSuffix(">")
                if (propertyName.isNotBlank() && isValidPropertyName(propertyName)) {
                    propertyName
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            System.err.println("MethodCallToPropertyAccessConverter: Error extracting property name from Kotlin getter '$methodName': ${e.message}")
            null
        }
    }
    
    /**
     * Extracts property name from Java-style getter method calls.
     * Matches pattern: getPropertyName() with no parameters
     * 
     * @param methodName The method name to analyze
     * @param parameters The method parameters
     * @return Property name if pattern matches, null otherwise
     */
    fun extractPropertyNameFromJavaStyleGetter(methodName: String, parameters: List<String>): String? {
        return try {
            if (methodName.startsWith("get") && parameters.isEmpty()) {
                val propertyName = methodName.removePrefix("get")
                if (propertyName.isNotBlank() && isValidJavaStylePropertyName(propertyName)) {
                    // Convert first character to lowercase for property name
                    propertyName.replaceFirstChar { it.lowercase() }
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            System.err.println("MethodCallToPropertyAccessConverter: Error extracting property name from Java-style getter '$methodName': ${e.message}")
            null
        }
    }
    
    /**
     * Extracts property name from Java-style setter method calls.
     * Matches pattern: setPropertyName(value) with exactly one parameter
     * 
     * @param methodName The method name to analyze
     * @param parameters The method parameters
     * @return Property name if pattern matches, null otherwise
     */
    fun extractPropertyNameFromJavaStyleSetter(methodName: String, parameters: List<String>): String? {
        return try {
            if (methodName.startsWith("set") && parameters.size == 1) {
                val propertyName = methodName.removePrefix("set")
                if (propertyName.isNotBlank() && isValidJavaStylePropertyName(propertyName)) {
                    // Convert first character to lowercase for property name
                    propertyName.replaceFirstChar { it.lowercase() }
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            System.err.println("MethodCallToPropertyAccessConverter: Error extracting property name from Java-style setter '$methodName': ${e.message}")
            null
        }
    }
    
    /**
     * Validates if a string is a valid property name for Kotlin properties.
     * Checks for basic naming conventions and excludes invalid characters.
     * 
     * @param propertyName The property name to validate
     * @return true if valid, false otherwise
     */
    private fun isValidPropertyName(propertyName: String): Boolean {
        return try {
            // Basic validation: not empty, starts with letter or underscore, contains only valid characters
            propertyName.isNotBlank() &&
            (propertyName.first().isLetter() || propertyName.first() == '_') &&
            propertyName.all { it.isLetterOrDigit() || it == '_' }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Validates if a string is a valid property name for Java-style getters/setters.
     * Checks that the name starts with an uppercase letter (after removing get/set prefix).
     * 
     * @param propertyName The property name to validate (after removing get/set prefix)
     * @return true if valid, false otherwise
     */
    private fun isValidJavaStylePropertyName(propertyName: String): Boolean {
        return try {
            // Java-style property names should start with uppercase letter after get/set prefix
            propertyName.isNotBlank() &&
            propertyName.first().isUpperCase() &&
            propertyName.all { it.isLetterOrDigit() || it == '_' }
        } catch (e: Exception) {
            false
        }
    }
}