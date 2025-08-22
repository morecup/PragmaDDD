package com.example

import org.morecup.pragmaddd.core.annotation.DomainEntity

/**
 * Test domain entity for testing DDD analyzer
 * This entity is used in test scenarios
 */
@DomainEntity
class TestDomainEntity(
    private val entityId: Long,
    private var status: String = "ACTIVE"
) {
    
    private var lastModified: Long = System.currentTimeMillis()
    
    /**
     * Changes the entity status
     * @param newStatus The new status to set
     */
    fun changeStatus(newStatus: String) {
        if (isValidStatus(newStatus)) {
            this.status = newStatus
            this.lastModified = System.currentTimeMillis()
        }
    }
    
    /**
     * Validates if the status is valid
     * @param status The status to validate
     * @return True if valid, false otherwise
     */
    private fun isValidStatus(status: String): Boolean {
        val validStatuses = listOf("ACTIVE", "INACTIVE", "PENDING")
        return status in validStatuses
    }
    
    /**
     * Gets the current status
     * @return The current status
     */
    fun getCurrentStatus(): String {
        return this.status
    }
    
    /**
     * Gets the entity ID
     * @return The entity ID
     */
    fun getId(): Long {
        return this.entityId
    }
}