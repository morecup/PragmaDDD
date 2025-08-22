package org.morecup.pragmaddd.analyzer.collector

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.morecup.pragmaddd.analyzer.analyzer.ClassAnalyzer
import org.morecup.pragmaddd.analyzer.model.ClassMetadata
import org.morecup.pragmaddd.analyzer.error.AnalysisError
import org.morecup.pragmaddd.analyzer.error.AnalysisWarning
import org.morecup.pragmaddd.analyzer.error.ErrorReporter

/**
 * Interface for collecting and aggregating metadata from IR analysis
 */
interface MetadataCollector {
    /**
     * Collects class metadata from IR class declaration
     */
    fun collectClassMetadata(irClass: IrClass): ClassMetadata?
    
    /**
     * Adds metadata to main sources collection
     */
    fun addToMainSources(metadata: ClassMetadata)
    
    /**
     * Gets all main sources metadata
     */
    fun getMainSourcesMetadata(): List<ClassMetadata>
    
    /**
     * Validates metadata consistency and integrity
     */
    fun validateMetadata(): ValidationResult
    
    /**
     * Aggregates metadata from multiple sources
     */
    fun aggregateMetadata(otherCollector: MetadataCollector)
    
    /**
     * Clears all collected metadata
     */
    fun clearMetadata()
    
    /**
     * Gets memory usage statistics
     */
    fun getMemoryUsage(): MemoryUsageStats
}



/**
 * Memory usage statistics for monitoring
 */
data class MemoryUsageStats(
    val mainSourcesCount: Int,
    val approximateMemoryUsage: Long,
    val maxMemoryThreshold: Long
)

/**
 * Implementation of MetadataCollector with validation and aggregation capabilities, optimized for memory usage
 */
class MetadataCollectorImpl(
    private val classAnalyzer: ClassAnalyzer,
    private val errorReporter: ErrorReporter,
    private val maxMetadataEntries: Int = 1000
) : MetadataCollector {
    
    private val mainSourcesMetadata = mutableListOf<ClassMetadata>()
    
    /**
     * Collects class metadata from IR class declaration with error handling
     */
    override fun collectClassMetadata(irClass: IrClass): ClassMetadata? {
        return try {
            val metadata = classAnalyzer.analyzeClass(irClass)
            
            // Check if metadata was generated
            if (metadata == null) {
                errorReporter.reportError(
                    AnalysisError.ClassAnalysisError(
                        className = irClass.name.asString(),
                        message = "Class analyzer returned null metadata",
                        cause = null
                    )
                )
                return null
            }
            
            // Validate metadata before adding
            if (validateClassMetadata(metadata)) {
                metadata
            } else {
                errorReporter.reportError(
                    AnalysisError.ClassAnalysisError(
                        className = irClass.name.asString(),
                        message = "Generated metadata failed validation",
                        cause = IllegalStateException("Invalid metadata structure")
                    )
                )
                null
            }
        } catch (e: Exception) {
            errorReporter.reportError(
                AnalysisError.ClassAnalysisError(
                    className = irClass.name.asString(),
                    message = "Failed to collect class metadata: ${e.message}",
                    cause = e
                )
            )
            null
        }
    }
    
    /**
     * Adds metadata to main sources collection with memory monitoring
     */
    override fun addToMainSources(metadata: ClassMetadata) {
        // Check memory limits before adding
        if (mainSourcesMetadata.size >= maxMetadataEntries) {
            errorReporter.reportWarning(
                AnalysisWarning(
                    message = "Maximum metadata entries reached ($maxMetadataEntries), skipping additional entries",
                    className = metadata.className
                )
            )
            return
        }
        
        // Check for duplicates
        val existingMetadata = mainSourcesMetadata.find { it.className == metadata.className }
        if (existingMetadata != null) {
            errorReporter.reportWarning(
                AnalysisWarning(
                    message = "Duplicate class metadata found, replacing existing entry",
                    className = metadata.className
                )
            )
            mainSourcesMetadata.removeAll { it.className == metadata.className }
        }
        
        mainSourcesMetadata.add(metadata)
        
        // Monitor memory usage
        monitorMemoryUsage()
    }
    
    /**
     * Gets all main sources metadata as immutable list
     */
    override fun getMainSourcesMetadata(): List<ClassMetadata> {
        return mainSourcesMetadata.toList()
    }
    
    /**
     * Validates metadata consistency and integrity
     */
    override fun validateMetadata(): ValidationResult {
        val errors = mutableListOf<AnalysisError>()
        val warnings = mutableListOf<AnalysisWarning>()
        
        // Validate main sources
        validateMetadataCollection(mainSourcesMetadata, "main", errors, warnings)
        
        // Check for overall consistency
        val totalClasses = mainSourcesMetadata.size
        if (totalClasses == 0) {
            warnings.add(
                AnalysisWarning(
                    message = "No DDD classes found in any source set",
                    className = "MetadataCollector"
                )
            )
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
    
    /**
     * Validates a collection of metadata
     */
    private fun validateMetadataCollection(
        metadata: List<ClassMetadata>,
        sourceType: String,
        errors: MutableList<AnalysisError>,
        warnings: MutableList<AnalysisWarning>
    ) {
        // Check for duplicate class names
        val classNames = metadata.map { it.className }
        val duplicates = classNames.groupBy { it }.filter { it.value.size > 1 }.keys
        
        duplicates.forEach { className ->
            errors.add(
                AnalysisError.MetadataCollectionError(
                    className = className,
                    message = "Duplicate class found in $sourceType sources: $className",
                    cause = null
                )
            )
        }
        
        // Validate individual metadata entries
        metadata.forEach { classMetadata ->
            if (!validateClassMetadata(classMetadata)) {
                errors.add(
                    AnalysisError.MetadataCollectionError(
                        className = classMetadata.className,
                        message = "Invalid metadata structure for class: ${classMetadata.className}",
                        cause = null
                    )
                )
            }
        }
        
        // Check for reasonable metadata size
        if (metadata.size > maxMetadataEntries / 2) {
            warnings.add(
                AnalysisWarning(
                    message = "Large number of classes in $sourceType sources: ${metadata.size}",
                    className = "MetadataCollector"
                )
            )
        }
    }
    
    /**
     * Validates individual class metadata
     */
    private fun validateClassMetadata(metadata: ClassMetadata): Boolean {
        return try {
            // Basic validation checks
            metadata.className.isNotBlank() &&
            metadata.packageName.isNotBlank() &&
            metadata.properties.isNotEmpty() || metadata.methods.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Aggregates metadata from another collector
     */
    override fun aggregateMetadata(otherCollector: MetadataCollector) {
        // Add main sources from other collector
        otherCollector.getMainSourcesMetadata().forEach { metadata ->
            addToMainSources(metadata)
        }
    }
    
    /**
     * Clears all collected metadata
     */
    override fun clearMetadata() {
        mainSourcesMetadata.clear()
    }
    
    /**
     * Gets memory usage statistics
     */
    override fun getMemoryUsage(): MemoryUsageStats {
        val approximateMemoryUsage = calculateApproximateMemoryUsage()
        
        return MemoryUsageStats(
            mainSourcesCount = mainSourcesMetadata.size,
            approximateMemoryUsage = approximateMemoryUsage,
            maxMemoryThreshold = maxMetadataEntries * 50_000L // Rough estimate: 50KB per metadata entry
        )
    }
    
    /**
     * Calculates approximate memory usage
     */
    private fun calculateApproximateMemoryUsage(): Long {
        // Rough estimation based on metadata structure
        val baseClassSize = 1000L // Base size per class metadata
        val methodSize = 200L // Average size per method
        val propertySize = 100L // Average size per property
        
        return mainSourcesMetadata.sumOf { metadata ->
            baseClassSize + 
            (metadata.methods.size * methodSize) + 
            (metadata.properties.size * propertySize)
        }
    }
    
    /**
     * Monitors memory usage and reports warnings if thresholds are exceeded
     */
    private fun monitorMemoryUsage() {
        val memoryStats = getMemoryUsage()
        val approximateMemoryUsage = memoryStats.approximateMemoryUsage
        
        // Report warning if memory usage is high
        if (approximateMemoryUsage > memoryStats.maxMemoryThreshold) {
            errorReporter.reportWarning(
                AnalysisWarning(
                    message = "High memory usage detected: ${approximateMemoryUsage / 1_000_000}MB",
                    className = "MetadataCollector"
                )
            )
        }
    }
}