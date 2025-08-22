package org.morecup.pragmaddd.analyzer

import org.gradle.api.provider.Property
import java.io.File

/**
 * Configuration extension for the Pragma DDD Analyzer plugin
 * Provides configuration options for JSON file generation and analysis features
 */
abstract class PragmaDddAnalyzerExtension {
    
    /**
     * Output directory for generated JSON files
     * FIXED PATH: "build/generated/pragmaddd/main/resources" - NOT CONFIGURABLE BY USERS
     * This is an internal property and should not be modified by users.
     */
    internal abstract val outputDirectory: Property<String>
    

    
    /**
     * JSON file naming convention
     * FIXED FILENAME: "domain-analyzer.json" - NOT CONFIGURABLE BY USERS
     * This is an internal property and should not be modified by users.
     */
    internal abstract val jsonFileNaming: Property<String>
    
    /**
     * Enable method analysis (method calls and property access within methods)
     * Default: true
     * When disabled, method bodies will not be analyzed for calls and property access
     */
    abstract val enableMethodAnalysis: Property<Boolean>
    
    /**
     * Enable property analysis (property access pattern detection)
     * Default: true
     * When disabled, property access patterns will not be detected
     */
    abstract val enablePropertyAnalysis: Property<Boolean>
    
    /**
     * Enable documentation extraction from KDoc comments
     * Default: true
     * When disabled, KDoc comments will not be extracted and included in metadata
     */
    abstract val enableDocumentationExtraction: Property<Boolean>
    
    /**
     * Maximum number of classes to analyze per compilation
     * Default: 1000
     * Used to prevent excessive memory usage on very large projects
     */
    abstract val maxClassesPerCompilation: Property<Int>
    
    /**
     * Whether to fail the build on analysis errors
     * Default: false
     * When true, compilation will fail if any class analysis encounters errors
     * When false, errors are logged as warnings and compilation continues
     */
    abstract val failOnAnalysisErrors: Property<Boolean>
    
    /**
     * Validates the configuration settings
     * @throws IllegalArgumentException if configuration is invalid
     */
    fun validate() {
        // Note: outputDirectory and jsonFileNaming are now fixed and don't need validation
        // Fixed path: build/generated/pragmaddd/main/resources/META-INF/pragma-ddd-analyzer/domain-analyzer.json
        validateMaxClassesPerCompilation()
        validateAnalysisFeatureConfiguration()
    }
    
    /**
     * Validates the output directory configuration
     */
    private fun validateOutputDirectory() {
        val outputDir = outputDirectory.orNull
        if (outputDir.isNullOrBlank()) {
            throw IllegalArgumentException("Output directory cannot be null or blank")
        }
        
        // Check if the path contains only valid characters
        val invalidPathChars = listOf('*', '?', '"', '<', '>', '|')
        if (outputDir.any { it in invalidPathChars }) {
            throw IllegalArgumentException("Output directory contains invalid path characters: ${invalidPathChars.joinToString("")}")
        }
        
        // Validate that the path is not just whitespace
        if (outputDir.trim() != outputDir) {
            throw IllegalArgumentException("Output directory cannot have leading or trailing whitespace")
        }
    }
    
    /**
     * Validates the JSON file naming configuration
     */
    private fun validateJsonFileNaming() {
        val jsonNaming = jsonFileNaming.orNull
        if (jsonNaming.isNullOrBlank()) {
            throw IllegalArgumentException("JSON file naming cannot be null or blank")
        }
        
        // Validate JSON file naming pattern (no special characters that would cause file system issues)
        val invalidChars = listOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')
        if (jsonNaming.any { it in invalidChars }) {
            throw IllegalArgumentException("JSON file naming contains invalid characters: ${invalidChars.joinToString("")}")
        }
        
        // Check for reserved names on Windows
        val reservedNames = listOf("CON", "PRN", "AUX", "NUL", "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9", "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9")
        if (reservedNames.any { jsonNaming.uppercase().startsWith(it) }) {
            throw IllegalArgumentException("JSON file naming cannot use reserved system names: ${reservedNames.joinToString(", ")}")
        }
        
        // Validate length (reasonable limit for file names)
        if (jsonNaming.length > 100) {
            throw IllegalArgumentException("JSON file naming is too long (maximum 100 characters)")
        }
    }
    
    /**
     * Validates the maximum classes per compilation setting
     */
    private fun validateMaxClassesPerCompilation() {
        val maxClasses = maxClassesPerCompilation.orNull
        if (maxClasses != null && maxClasses <= 0) {
            throw IllegalArgumentException("Maximum classes per compilation must be a positive number, got: $maxClasses")
        }
        
        if (maxClasses != null && maxClasses > 10000) {
            throw IllegalArgumentException("Maximum classes per compilation is too high (maximum 10000), got: $maxClasses")
        }
    }
    
    /**
     * Validates the analysis feature configuration for logical consistency
     */
    private fun validateAnalysisFeatureConfiguration() {
        val methodAnalysis = enableMethodAnalysis.orNull ?: true
        val propertyAnalysis = enablePropertyAnalysis.orNull ?: true
        
        // Property analysis requires method analysis to be meaningful
        if (propertyAnalysis && !methodAnalysis) {
            throw IllegalArgumentException("Property analysis requires method analysis to be enabled")
        }
    }
    
    /**
     * Returns a summary of the current configuration
     */
    fun getConfigurationSummary(): String {
        return buildString {
            appendLine("Pragma DDD Analyzer Configuration:")
            appendLine("  Output Path: $FIXED_COMPLETE_JSON_PATH (FIXED - NOT CONFIGURABLE)")
            appendLine("  Enable Method Analysis: ${enableMethodAnalysis.orNull}")
            appendLine("  Enable Property Analysis: ${enablePropertyAnalysis.orNull}")
            appendLine("  Enable Documentation Extraction: ${enableDocumentationExtraction.orNull}")
            appendLine("  Max Classes Per Compilation: ${maxClassesPerCompilation.orNull}")
            appendLine("  Fail On Analysis Errors: ${failOnAnalysisErrors.orNull}")
        }
    }
    
    /**
     * Returns the resolved output directory as a File object (always fixed path)
     * Fixed path: build/generated/pragmaddd/main/resources - NOT CONFIGURABLE
     * @param projectDir the project directory to resolve relative paths against
     */
    fun getResolvedOutputDirectory(projectDir: File): File {
        // Always return the FIXED output directory - NOT configurable by users
        return File(projectDir, FIXED_OUTPUT_DIRECTORY)
    }
    
    /**
     * Returns the main source JSON file name (always fixed)
     * Fixed filename: domain-analyzer.json - NOT CONFIGURABLE
     */
    fun getMainSourceJsonFileName(): String {
        return FIXED_JSON_FILENAME
    }
    
    /**
     * Returns the complete fixed path for the JSON file
     * Fixed path: build/generated/pragmaddd/main/resources/META-INF/pragma-ddd-analyzer/domain-analyzer.json
     * This path is NOT configurable by users
     */
    fun getFixedJsonFilePath(): String {
        return FIXED_COMPLETE_JSON_PATH
    }
    
    companion object {
        // Fixed paths that cannot be configured by users
        const val FIXED_OUTPUT_DIRECTORY = "build/generated/pragmaddd/main/resources"
        const val FIXED_JSON_FILENAME = "domain-analyzer.json"
        const val FIXED_META_INF_PATH = "META-INF/pragma-ddd-analyzer"
        const val FIXED_COMPLETE_JSON_PATH = "$FIXED_OUTPUT_DIRECTORY/$FIXED_META_INF_PATH/$FIXED_JSON_FILENAME"
    }
    
    /**
     * Checks if any analysis features are enabled
     */
    fun hasAnyAnalysisFeaturesEnabled(): Boolean {
        return enableMethodAnalysis.get() || 
               enablePropertyAnalysis.get() || 
               enableDocumentationExtraction.get()
    }
}