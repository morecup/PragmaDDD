package com.example

import org.morecup.pragmaddd.core.annotation.AggregateRoot

/**
 * Test aggregate root for testing DDD analyzer
 * This class is used to verify test source analysis
 */
@AggregateRoot
class TestAggregateRoot(
    private val testId: String,
    private var testName: String
) {
    
    /**
     * Updates the test name
     * @param newName The new name to set
     */
    fun updateTestName(newName: String) {
        if (newName.isNotBlank()) {
            this.testName = newName
        }
    }
    
    /**
     * Gets the test identifier
     * @return The test ID
     */
    fun getTestId(): String {
        return this.testId
    }
    
    /**
     * Validates the test aggregate
     * @return True if valid, false otherwise
     */
    fun validateTest(): Boolean {
        return testId.isNotEmpty() && testName.isNotBlank()
    }
}