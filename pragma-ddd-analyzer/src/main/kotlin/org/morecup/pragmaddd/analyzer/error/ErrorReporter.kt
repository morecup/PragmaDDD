package org.morecup.pragmaddd.analyzer.error

/**
 * Interface for reporting analysis errors and warnings during compilation
 */
interface ErrorReporter {
    /**
     * Reports a warning that doesn't prevent compilation from continuing
     */
    fun reportWarning(warning: AnalysisWarning)
    
    /**
     * Reports an error that may or may not prevent compilation
     */
    fun reportError(error: AnalysisError)
    
    /**
     * Determines whether the build should fail based on the collected errors
     */
    fun shouldFailBuild(errors: List<AnalysisError>): Boolean
    
    /**
     * Gets all reported warnings
     */
    fun getWarnings(): List<AnalysisWarning>
    
    /**
     * Gets all reported errors
     */
    fun getErrors(): List<AnalysisError>
    
    /**
     * Clears all reported warnings and errors
     */
    fun clear()
}

/**
 * Default implementation of ErrorReporter that logs to console
 * and provides configurable failure behavior
 */
class DefaultErrorReporter(
    private val failOnError: Boolean = false,
    private val logPrefix: String = "DDD Analyzer"
) : ErrorReporter {
    
    private val warnings = mutableListOf<AnalysisWarning>()
    private val errors = mutableListOf<AnalysisError>()
    
    override fun reportWarning(warning: AnalysisWarning) {
        warnings.add(warning)
        val location = buildLocationString(warning.className, warning.elementName)
        println("$logPrefix: WARNING$location: ${warning.message}")
    }
    
    override fun reportError(error: AnalysisError) {
        errors.add(error)
        val location = buildLocationString(getClassName(error), getElementName(error))
        println("$logPrefix: ERROR$location: ${error.message}")
        
        // Print stack trace for debugging if cause is available
        error.cause?.let { cause ->
            println("$logPrefix: Caused by: ${cause.javaClass.simpleName}: ${cause.message}")
            if (System.getProperty("ddd.analyzer.debug") == "true") {
                cause.printStackTrace()
            }
        }
    }
    
    override fun shouldFailBuild(errors: List<AnalysisError>): Boolean {
        if (!failOnError) return false
        
        // Only fail on critical errors that prevent proper analysis
        return errors.any { error ->
            when (error) {
                is AnalysisError.ConfigurationError -> true
                is AnalysisError.OutputGenerationError -> true
                is AnalysisError.JsonGenerationError -> true
                else -> false
            }
        }
    }
    
    override fun getWarnings(): List<AnalysisWarning> = warnings.toList()
    
    override fun getErrors(): List<AnalysisError> = errors.toList()
    
    override fun clear() {
        warnings.clear()
        errors.clear()
    }
    
    private fun buildLocationString(className: String?, elementName: String?): String {
        return when {
            className != null && elementName != null -> " in $className.$elementName"
            className != null -> " in $className"
            else -> ""
        }
    }
    
    private fun getClassName(error: AnalysisError): String? {
        return when (error) {
            is AnalysisError.ClassAnalysisError -> error.className
            is AnalysisError.MethodAnalysisError -> error.className
            is AnalysisError.PropertyAnalysisError -> error.className
            is AnalysisError.AnnotationDetectionError -> error.className
            is AnalysisError.DocumentationExtractionError -> error.className
            is AnalysisError.MetadataCollectionError -> error.className
            else -> null
        }
    }
    
    private fun getElementName(error: AnalysisError): String? {
        return when (error) {
            is AnalysisError.MethodAnalysisError -> error.methodName
            is AnalysisError.PropertyAnalysisError -> error.propertyName
            is AnalysisError.DocumentationExtractionError -> error.elementName
            else -> null
        }
    }
}

/**
 * Silent error reporter that collects errors but doesn't log them
 * Useful for testing scenarios
 */
class SilentErrorReporter(
    private val failOnError: Boolean = false
) : ErrorReporter {
    
    private val warnings = mutableListOf<AnalysisWarning>()
    private val errors = mutableListOf<AnalysisError>()
    
    override fun reportWarning(warning: AnalysisWarning) {
        warnings.add(warning)
    }
    
    override fun reportError(error: AnalysisError) {
        errors.add(error)
    }
    
    override fun shouldFailBuild(errors: List<AnalysisError>): Boolean {
        return failOnError && errors.isNotEmpty()
    }
    
    override fun getWarnings(): List<AnalysisWarning> = warnings.toList()
    
    override fun getErrors(): List<AnalysisError> = errors.toList()
    
    override fun clear() {
        warnings.clear()
        errors.clear()
    }
}