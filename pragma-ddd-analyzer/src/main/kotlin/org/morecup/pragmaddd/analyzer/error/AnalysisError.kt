package org.morecup.pragmaddd.analyzer.error

/**
 * Sealed class hierarchy representing different types of analysis errors
 * that can occur during DDD class analysis
 */
sealed class AnalysisError {
    abstract val message: String
    abstract val cause: Throwable?
    
    /**
     * Error that occurs during class analysis
     */
    data class ClassAnalysisError(
        val className: String,
        override val message: String,
        override val cause: Throwable?
    ) : AnalysisError()
    
    /**
     * Error that occurs during method analysis
     */
    data class MethodAnalysisError(
        val className: String,
        val methodName: String,
        override val message: String,
        override val cause: Throwable?
    ) : AnalysisError()
    
    /**
     * Error that occurs during property analysis
     */
    data class PropertyAnalysisError(
        val className: String,
        val propertyName: String,
        override val message: String,
        override val cause: Throwable?
    ) : AnalysisError()
    
    /**
     * Error that occurs during annotation detection
     */
    data class AnnotationDetectionError(
        val className: String,
        override val message: String,
        override val cause: Throwable?
    ) : AnalysisError()
    
    /**
     * Error that occurs during documentation extraction
     */
    data class DocumentationExtractionError(
        val className: String,
        val elementName: String,
        override val message: String,
        override val cause: Throwable?
    ) : AnalysisError()
    
    /**
     * Error that occurs during JSON generation
     */
    data class JsonGenerationError(
        override val message: String,
        override val cause: Throwable?
    ) : AnalysisError()
    
    /**
     * Error that occurs during output file generation
     */
    data class OutputGenerationError(
        val outputPath: String,
        override val message: String,
        override val cause: Throwable?
    ) : AnalysisError()
    
    /**
     * Error that occurs during metadata collection
     */
    data class MetadataCollectionError(
        val className: String,
        override val message: String,
        override val cause: Throwable?
    ) : AnalysisError()
    
    /**
     * Configuration-related error
     */
    data class ConfigurationError(
        override val message: String,
        override val cause: Throwable?
    ) : AnalysisError()
}

/**
 * Represents a warning during analysis that doesn't prevent compilation
 */
data class AnalysisWarning(
    val message: String,
    val className: String? = null,
    val elementName: String? = null
)