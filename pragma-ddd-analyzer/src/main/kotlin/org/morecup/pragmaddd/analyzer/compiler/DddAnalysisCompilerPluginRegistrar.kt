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
    
    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        println("DDD Analyzer: Compiler plugin registrar called")
        System.err.println("DDD Analyzer: Compiler plugin registrar called")
        
        // Extract configuration values
        val outputDirectory = configuration.get(DddAnalysisCommandLineProcessor.OUTPUT_DIRECTORY_KEY)
        val isTestCompilation = configuration.get(DddAnalysisCommandLineProcessor.IS_TEST_COMPILATION_KEY) ?: false
        val jsonFileNaming = configuration.get(DddAnalysisCommandLineProcessor.JSON_FILE_NAMING_KEY) ?: "ddd-analysis"
        val enableMethodAnalysis = configuration.get(DddAnalysisCommandLineProcessor.ENABLE_METHOD_ANALYSIS_KEY) ?: true
        val enablePropertyAnalysis = configuration.get(DddAnalysisCommandLineProcessor.ENABLE_PROPERTY_ANALYSIS_KEY) ?: true
        val enableDocumentationExtraction = configuration.get(DddAnalysisCommandLineProcessor.ENABLE_DOCUMENTATION_EXTRACTION_KEY) ?: true
        val maxClassesPerCompilation = configuration.get(DddAnalysisCommandLineProcessor.MAX_CLASSES_PER_COMPILATION_KEY) ?: 1000
        val failOnAnalysisErrors = configuration.get(DddAnalysisCommandLineProcessor.FAIL_ON_ANALYSIS_ERRORS_KEY) ?: false
        
        println("DDD Analyzer: Configuration - outputDirectory: $outputDirectory, isTestCompilation: $isTestCompilation, jsonFileNaming: $jsonFileNaming")
        println("DDD Analyzer: Analysis features - method: $enableMethodAnalysis, property: $enablePropertyAnalysis, documentation: $enableDocumentationExtraction")
        println("DDD Analyzer: Performance settings - maxClasses: $maxClassesPerCompilation, failOnErrors: $failOnAnalysisErrors")
        
        // Create and register the IR generation extension
        val irExtension = DddAnalysisIrGenerationExtension(
            outputDirectory = outputDirectory,
            isTestCompilation = isTestCompilation,
            jsonFileNaming = jsonFileNaming,
            enableMethodAnalysis = enableMethodAnalysis,
            enablePropertyAnalysis = enablePropertyAnalysis,
            enableDocumentationExtraction = enableDocumentationExtraction,
            maxClassesPerCompilation = maxClassesPerCompilation,
            failOnAnalysisErrors = failOnAnalysisErrors
        )
        
        println("DDD Analyzer: Registering IR generation extension")
        IrGenerationExtension.registerExtension(irExtension)
        println("DDD Analyzer: IR generation extension registered successfully")
    }
}