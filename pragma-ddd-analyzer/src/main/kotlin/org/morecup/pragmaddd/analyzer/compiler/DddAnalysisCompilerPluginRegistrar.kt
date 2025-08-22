package org.morecup.pragmaddd.analyzer.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration

/**
 * Compiler plugin registrar for DDD analysis
 * Registers the IR generation extension with the Kotlin compiler
 */
@OptIn(ExperimentalCompilerApi::class)
class DddAnalysisCompilerPluginRegistrar : CompilerPluginRegistrar() {
    
    override val supportsK2: Boolean = true
    
    init {
        // Add initialization logging
        println("DDD Analyzer: ComponentRegistrar initialized")
        System.err.println("DDD Analyzer: ComponentRegistrar initialized")
    }
    
    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        // Force output to multiple streams to ensure visibility
        println("DDD Analyzer: Compiler plugin registrar called - THIS SHOULD ALWAYS APPEAR!")
        System.out.println("DDD Analyzer: Compiler plugin registrar called - THIS SHOULD ALWAYS APPEAR!")
        System.err.println("DDD Analyzer: Compiler plugin registrar called - THIS SHOULD ALWAYS APPEAR!")
        
        // Also write to a file for debugging
        try {
            val debugFile = java.io.File("ddd-analyzer-debug.log")
            debugFile.appendText("DDD Analyzer: Compiler plugin registrar called at ${System.currentTimeMillis()}\n")
        } catch (e: Exception) {
            // Ignore file write errors
        }
        
        System.out.flush()
        System.err.flush()
        
        // Extract configuration values
        val outputDirectory = configuration.get(DddAnalysisCommandLineProcessor.OUTPUT_DIRECTORY_KEY)
        val jsonFileNaming = configuration.get(DddAnalysisCommandLineProcessor.JSON_FILE_NAMING_KEY) ?: "ddd-analysis"
        val enableMethodAnalysis = configuration.get(DddAnalysisCommandLineProcessor.ENABLE_METHOD_ANALYSIS_KEY) ?: true
        val enablePropertyAnalysis = configuration.get(DddAnalysisCommandLineProcessor.ENABLE_PROPERTY_ANALYSIS_KEY) ?: true
        val enableDocumentationExtraction = configuration.get(DddAnalysisCommandLineProcessor.ENABLE_DOCUMENTATION_EXTRACTION_KEY) ?: true
        val maxClassesPerCompilation = configuration.get(DddAnalysisCommandLineProcessor.MAX_CLASSES_PER_COMPILATION_KEY) ?: 1000
        val failOnAnalysisErrors = configuration.get(DddAnalysisCommandLineProcessor.FAIL_ON_ANALYSIS_ERRORS_KEY) ?: false
        
        println("DDD Analyzer: Configuration - outputDirectory: $outputDirectory, jsonFileNaming: $jsonFileNaming")
        println("DDD Analyzer: Analysis features - method: $enableMethodAnalysis, property: $enablePropertyAnalysis, documentation: $enableDocumentationExtraction")
        println("DDD Analyzer: Performance settings - maxClasses: $maxClassesPerCompilation, failOnErrors: $failOnAnalysisErrors")
        
        // Validate that we have an output directory
        if (outputDirectory == null) {
            println("DDD Analyzer: ERROR - No output directory configured! IR extension will not be registered.")
            System.err.println("DDD Analyzer: ERROR - No output directory configured! IR extension will not be registered.")
            return
        }
        
        // Create and register the IR generation extension
        val irExtension = DddAnalysisIrGenerationExtension(
            outputDirectory = outputDirectory,
            jsonFileNaming = jsonFileNaming,
            enableMethodAnalysis = enableMethodAnalysis,
            enablePropertyAnalysis = enablePropertyAnalysis,
            enableDocumentationExtraction = enableDocumentationExtraction,
            maxClassesPerCompilation = maxClassesPerCompilation,
            failOnAnalysisErrors = failOnAnalysisErrors
        )
        
        println("DDD Analyzer: Registering IR generation extension with outputDirectory: $outputDirectory")
        IrGenerationExtension.registerExtension(irExtension)
        println("DDD Analyzer: IR generation extension registered successfully")
    }
}