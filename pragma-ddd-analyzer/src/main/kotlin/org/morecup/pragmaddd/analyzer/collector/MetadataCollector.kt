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
     * Adds metadata to test sources collection
     */
    fun addToTestSources(metadata: ClassMetadata)
    
    /**
     * Gets all main sources metadata
     */
    fun getMainSourcesMetadata(): List<ClassMetadata>
    
    /**
     * Gets all test sources metadata
     */
    fun getTestSourcesMetadata(): List<ClassMetadata>
    
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
    fun clear()
    
    /**
     * Gets total count of collected metadata
     */
    fun getTotalCount(): Int
    
    /**
     * Checks if any metadata has been collected
     */
    fun hasMetadata(): Boolean
}

/**
 * Result of metadata validation
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<ValidationError> = emptyList(),
    val warnings: List<ValidationWarning> = emptyList()
)

/**
 * Validation error for metadata inconsistencies
 */
data class ValidationError(
    val className: String,
    val message: String,
    val type: ValidationErrorType
)

/**
 * Validation warning for potential issues
 */
data class ValidationWarning(
    val className: String,
    val message: String,
    val type: ValidationWarningType
)

/**
 * Types of validation errors
 */
enum class ValidationErrorType {
    DUPLICATE_CLASS,
    INVALID_PACKAGE_NAME,
    MISSING_REQUIRED_FIELD,
    INVALID_ANNOTATION_TYPE
}

/**
 * Types of validation warnings
 */
enum class ValidationWarningType {
    MISSING_DOCUMENTATION,
    EMPTY_METHOD_LIST,
    EMPTY_PROPERTY_LIST,
    INCONSISTENT_NAMING
}

/**
 * Implementation of MetadataCollector that collects and manages analysis results
 * with validation and aggregation capabilities, optimized for memory usage
 */
class MetadataCollectorImpl(
    private val classAnalyzer: ClassAnalyzer,
    private val errorReporter: ErrorReporter,
    private val maxMetadataEntries: Int = 1000
) : MetadataCollector {
    
    // Use ArrayList with initial capacity to reduce memory allocations
    private val mainSourcesMetadata = ArrayList<ClassMetadata>(50)
    private val testSourcesMetadata = ArrayList<ClassMetadata>(50)
    
    // Track memory usage
    private var approximateMemoryUsage = 0L
    
    /**
     * Collects class metadata from IR analysis
     */
    override fun collectClassMetadata(irClass: IrClass): ClassMetadata? {
        return try {
            classAnalyzer.analyzeClass(irClass)
        } catch (e: Exception) {
            errorReporter.reportError(
                AnalysisError.MetadataCollectionError(
                    className = irClass.name.asString(),
                    message = "Failed to collect class metadata: ${e.message}",
                    cause = e
                )
            )
            null
        }
    }
    
    /**
     * Adds metadata to main sources with validation and memory management
     */
    override fun addToMainSources(metadata: ClassMetadata) {
        // Check memory limits before adding
        if (getTotalCount() >= maxMetadataEntries) {
            errorReporter.reportWarning(
                AnalysisWarning(
                    message = "Maximum metadata entries ($maxMetadataEntries) reached, skipping class: ${metadata.className}",
                    className = "${metadata.packageName}.${metadata.className}"
                )
            )
            return
        }
        
        if (validateSingleMetadata(metadata, mainSourcesMetadata)) {
            mainSourcesMetadata.add(metadata)
            updateMemoryUsage(metadata)
        }
    }
    
    /**
     * Adds metadata to test sources with validation and memory management
     */
    override fun addToTestSources(metadata: ClassMetadata) {
        // Check memory limits before adding
        if (getTotalCount() >= maxMetadataEntries) {
            errorReporter.reportWarning(
                AnalysisWarning(
                    message = "Maximum metadata entries ($maxMetadataEntries) reached, skipping class: ${metadata.className}",
                    className = "${metadata.packageName}.${metadata.className}"
                )
            )
            return
        }
        
        if (validateSingleMetadata(metadata, testSourcesMetadata)) {
            testSourcesMetadata.add(metadata)
            updateMemoryUsage(metadata)
        }
    }
    
    /**
     * Gets all main sources metadata as immutable list
     */
    override fun getMainSourcesMetadata(): List<ClassMetadata> {
        return mainSourcesMetadata.toList()
    }
    
    /**
     * Gets all test sources metadata as immutable list
     */
    override fun getTestSourcesMetadata(): List<ClassMetadata> {
        return testSourcesMetadata.toList()
    }
    
    /**
     * Validates all collected metadata for consistency and integrity
     */
    override fun validateMetadata(): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        val warnings = mutableListOf<ValidationWarning>()
        
        // Validate main sources
        validateMetadataList(mainSourcesMetadata, errors, warnings)
        
        // Validate test sources
        validateMetadataList(testSourcesMetadata, errors, warnings)
        
        // Check for duplicates between main and test sources
        checkCrossSourceDuplicates(errors)
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
    
    /**
     * Aggregates metadata from another collector
     */
    override fun aggregateMetadata(otherCollector: MetadataCollector) {
        // Add main sources metadata
        otherCollector.getMainSourcesMetadata().forEach { metadata ->
            addToMainSources(metadata)
        }
        
        // Add test sources metadata
        otherCollector.getTestSourcesMetadata().forEach { metadata ->
            addToTestSources(metadata)
        }
    }
    
    /**
     * Clears all collected metadata and resets memory tracking
     */
    override fun clear() {
        mainSourcesMetadata.clear()
        testSourcesMetadata.clear()
        approximateMemoryUsage = 0L
        
        // Suggest garbage collection after clearing large amounts of data
        if (approximateMemoryUsage > 10_000_000) { // 10MB threshold
            System.gc()
        }
    }
    
    /**
     * Updates approximate memory usage tracking
     */
    private fun updateMemoryUsage(metadata: ClassMetadata) {
        // Rough estimation of memory usage per metadata entry
        val estimatedSize = estimateMetadataSize(metadata)
        approximateMemoryUsage += estimatedSize
        
        // Log memory usage warnings
        if (approximateMemoryUsage > 50_000_000) { // 50MB threshold
            errorReporter.reportWarning(
                AnalysisWarning(
                    message = "High memory usage detected: ${approximateMemoryUsage / 1_000_000}MB",
                    className = "MetadataCollector"
                )
            )
        }
    }
    
    /**
     * Estimates memory size of a metadata entry
     */
    private fun estimateMetadataSize(metadata: ClassMetadata): Long {
        var size = 0L
        
        // Base object overhead
        size += 100
        
        // String fields
        size += metadata.className.length * 2L
        size += metadata.packageName.length * 2L
        
        // Properties
        size += metadata.properties.size * 200L // Rough estimate per property
        
        // Methods
        size += metadata.methods.size * 300L // Rough estimate per method
        
        // Annotations
        size += metadata.annotations.size * 100L // Rough estimate per annotation
        
        // Documentation
        metadata.documentation?.let { doc ->
            size += (doc.summary?.length ?: 0) * 2L
            size += (doc.description?.length ?: 0) * 2L
            size += doc.parameters.values.sumOf { it.length * 2L }
            size += (doc.returnDescription?.length ?: 0) * 2L
        }
        
        return size
    }
    
    /**
     * Gets current approximate memory usage in bytes
     */
    fun getApproximateMemoryUsage(): Long {
        return approximateMemoryUsage
    }
    
    /**
     * Gets total count of all collected metadata
     */
    override fun getTotalCount(): Int {
        return mainSourcesMetadata.size + testSourcesMetadata.size
    }
    
    /**
     * Checks if any metadata has been collected
     */
    override fun hasMetadata(): Boolean {
        return mainSourcesMetadata.isNotEmpty() || testSourcesMetadata.isNotEmpty()
    }
    
    /**
     * Validates a single metadata entry before adding to collection
     */
    private fun validateSingleMetadata(metadata: ClassMetadata, existingMetadata: List<ClassMetadata>): Boolean {
        val fullClassName = "${metadata.packageName}.${metadata.className}"
        
        // Check for duplicate class names
        val isDuplicate = existingMetadata.any { existing ->
            "${existing.packageName}.${existing.className}" == fullClassName
        }
        
        if (isDuplicate) {
            errorReporter.reportWarning(
                AnalysisWarning(
                    message = "Duplicate class detected: $fullClassName",
                    className = fullClassName
                )
            )
        }
        
        // Basic validation - ensure required fields are present
        if (metadata.className.isBlank()) {
            errorReporter.reportError(
                AnalysisError.MetadataCollectionError(
                    className = fullClassName,
                    message = "Class name cannot be blank",
                    cause = null
                )
            )
            return false
        }
        
        return true
    }
    
    /**
     * Validates a list of metadata entries
     */
    private fun validateMetadataList(
        metadataList: List<ClassMetadata>,
        errors: MutableList<ValidationError>,
        warnings: MutableList<ValidationWarning>
    ) {
        val classNames = mutableSetOf<String>()
        
        metadataList.forEach { metadata ->
            val fullClassName = "${metadata.packageName}.${metadata.className}"
            
            // Check for duplicates within the same source type
            if (!classNames.add(fullClassName)) {
                errors.add(ValidationError(
                    className = fullClassName,
                    message = "Duplicate class found in the same source set",
                    type = ValidationErrorType.DUPLICATE_CLASS
                ))
            }
            
            // Validate required fields
            if (metadata.className.isBlank()) {
                errors.add(ValidationError(
                    className = fullClassName,
                    message = "Class name cannot be blank",
                    type = ValidationErrorType.MISSING_REQUIRED_FIELD
                ))
            }
            
            // Validate package name format
            if (metadata.packageName.isNotBlank() && !isValidPackageName(metadata.packageName)) {
                errors.add(ValidationError(
                    className = fullClassName,
                    message = "Invalid package name format: ${metadata.packageName}",
                    type = ValidationErrorType.INVALID_PACKAGE_NAME
                ))
            }
            
            // Check for missing documentation
            if (metadata.documentation == null) {
                warnings.add(ValidationWarning(
                    className = fullClassName,
                    message = "Class has no documentation",
                    type = ValidationWarningType.MISSING_DOCUMENTATION
                ))
            }
            
            // Check for empty method list
            if (metadata.methods.isEmpty()) {
                warnings.add(ValidationWarning(
                    className = fullClassName,
                    message = "Class has no methods",
                    type = ValidationWarningType.EMPTY_METHOD_LIST
                ))
            }
            
            // Check for empty property list
            if (metadata.properties.isEmpty()) {
                warnings.add(ValidationWarning(
                    className = fullClassName,
                    message = "Class has no properties",
                    type = ValidationWarningType.EMPTY_PROPERTY_LIST
                ))
            }
        }
    }
    
    /**
     * Checks for duplicate classes between main and test sources
     */
    private fun checkCrossSourceDuplicates(errors: MutableList<ValidationError>) {
        val mainClassNames = mainSourcesMetadata.map { "${it.packageName}.${it.className}" }.toSet()
        val testClassNames = testSourcesMetadata.map { "${it.packageName}.${it.className}" }.toSet()
        
        val duplicates = mainClassNames.intersect(testClassNames)
        duplicates.forEach { className ->
            errors.add(ValidationError(
                className = className,
                message = "Class exists in both main and test sources",
                type = ValidationErrorType.DUPLICATE_CLASS
            ))
        }
    }
    
    /**
     * Validates package name format
     */
    private fun isValidPackageName(packageName: String): Boolean {
        if (packageName.isBlank()) return true // Empty package is valid
        
        val packageRegex = Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*$")
        return packageRegex.matches(packageName)
    }
}