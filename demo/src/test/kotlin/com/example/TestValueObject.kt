package com.example

import org.morecup.pragmaddd.core.annotation.ValueObj

/**
 * Test value object for testing DDD analyzer
 * Represents a test measurement value
 */
@ValueObj
data class TestValueObject(
    val value: Double,
    val unit: String,
    val precision: Int = 2
) {
    
    init {
        require(value >= 0) { "Value must be non-negative" }
        require(unit.isNotBlank()) { "Unit cannot be blank" }
        require(precision >= 0) { "Precision must be non-negative" }
    }
    
    /**
     * Converts to string representation
     * @return Formatted string with value and unit
     */
    fun toFormattedString(): String {
        val formattedValue = "%.${precision}f".format(value)
        return "$formattedValue $unit"
    }
    
    /**
     * Checks if this value is greater than another
     * @param other The other value to compare
     * @return True if this value is greater
     */
    fun isGreaterThan(other: TestValueObject): Boolean {
        require(this.unit == other.unit) { "Cannot compare values with different units" }
        return this.value > other.value
    }
    
    /**
     * Adds another value object
     * @param other The value to add
     * @return New value object with sum
     */
    fun plus(other: TestValueObject): TestValueObject {
        require(this.unit == other.unit) { "Cannot add values with different units" }
        return TestValueObject(
            value = this.value + other.value,
            unit = this.unit,
            precision = maxOf(this.precision, other.precision)
        )
    }
}