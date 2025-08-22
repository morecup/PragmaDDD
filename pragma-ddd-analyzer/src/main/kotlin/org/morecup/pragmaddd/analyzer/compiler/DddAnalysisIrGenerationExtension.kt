package org.morecup.pragmaddd.analyzer.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.name.FqName
import org.morecup.pragmaddd.analyzer.detector.AnnotationDetector
import org.morecup.pragmaddd.analyzer.detector.AnnotationDetectorImpl
import org.morecup.pragmaddd.analyzer.analyzer.ClassAnalyzer
import org.morecup.pragmaddd.analyzer.analyzer.ClassAnalyzerImpl
import org.morecup.pragmaddd.analyzer.analyzer.MethodAnalyzer
import org.morecup.pragmaddd.analyzer.analyzer.MethodAnalyzerImpl
import org.morecup.pragmaddd.analyzer.analyzer.PropertyAnalyzer
import org.morecup.pragmaddd.analyzer.analyzer.PropertyAnalyzerImpl
import org.morecup.pragmaddd.analyzer.analyzer.DocumentationExtractor
import org.morecup.pragmaddd.analyzer.analyzer.DocumentationExtractorImpl
import org.morecup.pragmaddd.analyzer.collector.MetadataCollector
import org.morecup.pragmaddd.analyzer.collector.MetadataCollectorImpl
import org.morecup.pragmaddd.analyzer.error.AnalysisError
import org.morecup.pragmaddd.analyzer.generator.JsonGenerator
import org.morecup.pragmaddd.analyzer.generator.JsonGeneratorImpl
import org.morecup.pragmaddd.analyzer.writer.ResourceWriter
import org.morecup.pragmaddd.analyzer.writer.ResourceWriterImpl
import org.morecup.pragmaddd.analyzer.compiler.ClassCollectorVisitor
import org.morecup.pragmaddd.analyzer.error.ErrorReporter
import org.morecup.pragmaddd.analyzer.error.DefaultErrorReporter

/**
 * IR Generation Extension for DDD analysis
 * Analyzes classes during IR generation phase and collects metadata
 */
class DddAnalysisIrGenerationExtension(
    private val outputDirectory: String?,
    private val jsonFileNaming: String,
    private val enableMethodAnalysis: Boolean,
    private val enablePropertyAnalysis: Boolean,
    private val enableDocumentationExtraction: Boolean,
    private val maxClassesPerCompilation: Int = 1000,
    private val failOnAnalysisErrors: Boolean = false
) : IrGenerationExtension {
    
    companion object {
        // Fixed paths that cannot be configured by users
        private const val FIXED_OUTPUT_DIRECTORY = "build/generated/pragmaddd/main/resources"
        private const val FIXED_JSON_FILENAME = "domain-analyzer.json"
        private const val FIXED_META_INF_PATH = "META-INF/pragma-ddd-analyzer"
        private const val FIXED_COMPLETE_PATH = "$FIXED_OUTPUT_DIRECTORY/$FIXED_META_INF_PATH/$FIXED_JSON_FILENAME"
    }
    
    private val errorReporter: ErrorReporter = DefaultErrorReporter(
        failOnError = false,
        logPrefix = "DDD Analyzer"
    )
    private val annotationDetector: AnnotationDetector = AnnotationDetectorImpl()
    private val documentationExtractor: DocumentationExtractor = if (enableDocumentationExtraction) {
        DocumentationExtractorImpl()
    } else {
        // Create a no-op implementation if documentation extraction is disabled
        object : DocumentationExtractor {
            override fun extractClassDocumentation(irClass: org.jetbrains.kotlin.ir.declarations.IrClass) = null
            override fun extractMethodDocumentation(irFunction: org.jetbrains.kotlin.ir.declarations.IrSimpleFunction) = null
            override fun extractPropertyDocumentation(irProperty: org.jetbrains.kotlin.ir.declarations.IrProperty) = null
            override fun extractFieldDocumentation(irField: org.jetbrains.kotlin.ir.declarations.IrField) = null
            override fun parseKDoc(kdocText: String) = org.morecup.pragmaddd.analyzer.model.DocumentationMetadata(null, null, emptyMap(), null)
        }
    }
    private val propertyAnalyzer: PropertyAnalyzer = if (enablePropertyAnalysis) {
        PropertyAnalyzerImpl()
    } else {
        // Create a no-op implementation if property analysis is disabled
        object : PropertyAnalyzer {
            override fun extractPropertyAccess(irFunction: org.jetbrains.kotlin.ir.declarations.IrSimpleFunction) = emptyList<org.morecup.pragmaddd.analyzer.model.PropertyAccessMetadata>()
            override fun detectDirectFieldAccess(irFunction: org.jetbrains.kotlin.ir.declarations.IrSimpleFunction) = emptyList<org.morecup.pragmaddd.analyzer.model.PropertyAccessMetadata>()
            override fun detectGetterSetterCalls(irFunction: org.jetbrains.kotlin.ir.declarations.IrSimpleFunction) = emptyList<org.morecup.pragmaddd.analyzer.model.PropertyAccessMetadata>()
            override fun detectMethodChainPropertyAccess(irFunction: org.jetbrains.kotlin.ir.declarations.IrSimpleFunction) = emptyList<org.morecup.pragmaddd.analyzer.model.PropertyAccessMetadata>()
        }
    }
    private val methodAnalyzer: MethodAnalyzer = if (enableMethodAnalysis) {
        MethodAnalyzerImpl(
            annotationDetector = annotationDetector,
            documentationExtractor = documentationExtractor,
            propertyAnalyzer = propertyAnalyzer,
            errorReporter = errorReporter,
            enableMethodAnalysis = enableMethodAnalysis
        )
    } else {
        // Create a no-op implementation if method analysis is disabled
        object : MethodAnalyzer {
            override fun analyzeMethod(irFunction: org.jetbrains.kotlin.ir.declarations.IrSimpleFunction) = 
                org.morecup.pragmaddd.analyzer.model.MethodMetadata(
                    name = irFunction.name.asString(),
                    parameters = emptyList(),
                    returnType = "Unit",
                    isPrivate = false,
                    methodCalls = emptyList(),
                    propertyAccesses = emptyList(),
                    documentation = null,
                    annotations = emptyList()
                )
            override fun extractMethodCalls(irFunction: org.jetbrains.kotlin.ir.declarations.IrSimpleFunction) = emptyList<org.morecup.pragmaddd.analyzer.model.MethodCallMetadata>()
            override fun extractPropertyAccess(irFunction: org.jetbrains.kotlin.ir.declarations.IrSimpleFunction) = emptyList<org.morecup.pragmaddd.analyzer.model.PropertyAccessMetadata>()
            override fun extractDocumentation(irFunction: org.jetbrains.kotlin.ir.declarations.IrSimpleFunction) = null
        }
    }
    private val classAnalyzer: ClassAnalyzer = ClassAnalyzerImpl(
        annotationDetector = annotationDetector,
        methodAnalyzer = methodAnalyzer,
        documentationExtractor = documentationExtractor,
        errorReporter = errorReporter
    )
    private val metadataCollector: MetadataCollector = MetadataCollectorImpl(
        classAnalyzer = classAnalyzer,
        errorReporter = errorReporter,
        maxMetadataEntries = maxClassesPerCompilation
    )
    private val jsonGenerator: JsonGenerator = JsonGeneratorImpl(errorReporter)
    private val resourceWriter: ResourceWriterImpl = ResourceWriterImpl()
    
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        println("DDD Analyzer: IR Generation Extension CALLED - This should always appear!")
        System.err.println("DDD Analyzer: IR Generation Extension CALLED - This should always appear!")
        System.out.println("DDD Analyzer: IR Generation Extension CALLED - This should always appear!")
        
        // Force flush all output streams
        System.out.flush()
        System.err.flush()
        
        val startTime = System.currentTimeMillis()
        val sourceType = "main"
        println("DDD Analyzer: IR Generation Extension invoked for module: ${moduleFragment.name} (source type: $sourceType)")
        System.err.println("DDD Analyzer: IR Generation Extension invoked for module: ${moduleFragment.name} (source type: $sourceType)")
        
        // Debug: Print current working directory and output directory
        val currentDir = System.getProperty("user.dir")
        println("DDD Analyzer: Current working directory: $currentDir")
        println("DDD Analyzer: Configured output directory: $outputDirectory")
        
        // Only proceed if we have an output directory configured
        if (outputDirectory == null) {
            println("DDD Analyzer: No output directory configured, skipping analysis")
            return
        }
        
        println("DDD Analyzer: Output directory configured: $outputDirectory")
        
        // Find all classes in the module with performance optimization
        val allClasses = mutableListOf<IrClass>()
        val classCollectionStart = System.currentTimeMillis()
        moduleFragment.accept(ClassCollectorVisitor(allClasses), null)
        val classCollectionTime = System.currentTimeMillis() - classCollectionStart
        
        println("DDD Analyzer: Found ${allClasses.size} total classes in module (${classCollectionTime}ms)")
        
        // Early exit if too many classes to prevent memory issues
        if (allClasses.size > maxClassesPerCompilation) {
            println("DDD Analyzer: Too many classes (${allClasses.size}) exceeds limit ($maxClassesPerCompilation), skipping analysis to prevent memory issues")
            return
        }
        
        // Filter classes that have DDD annotations with optimized detection
        val annotationFilterStart = System.currentTimeMillis()
        val dddAnnotatedClasses = allClasses.filter { irClass ->
            try {
                annotationDetector.hasDddAnnotation(irClass)
            } catch (e: Exception) {
                println("DDD Analyzer: Error checking annotations for class ${irClass.name}: ${e.message}")
                false
            }
        }
        val annotationFilterTime = System.currentTimeMillis() - annotationFilterStart
        
        println("DDD Analyzer: Found ${dddAnnotatedClasses.size} DDD-annotated classes for analysis in main sources (${annotationFilterTime}ms)")
        
        // If no DDD classes found, provide minimal debugging info
        if (dddAnnotatedClasses.isEmpty()) {
            println("DDD Analyzer: No DDD-annotated classes found in main sources")
            if (allClasses.size <= 10) {
                // Only log class names for small projects to avoid spam
                allClasses.forEach { irClass ->
                    println("  - ${irClass.name}")
                }
            }
            return
        }
        
        // Analyze each DDD-annotated class structure using MetadataCollector with performance tracking
        val analysisStart = System.currentTimeMillis()
        var successfulAnalyses = 0
        var failedAnalyses = 0
        
        dddAnnotatedClasses.forEach { irClass ->
            val classAnalysisStart = System.currentTimeMillis()
            
            try {
                val annotationType = annotationDetector.getDddAnnotationType(irClass)
                println("DDD Analyzer: Analyzing $annotationType class: ${irClass.name} (source: main)")
                
                // Collect class metadata with timeout protection
                val classMetadata = metadataCollector.collectClassMetadata(irClass)
                if (classMetadata != null) {
                    // Add to main source collection only
                    metadataCollector.addToMainSources(classMetadata)
                    
                    val classAnalysisTime = System.currentTimeMillis() - classAnalysisStart
                    println("DDD Analyzer: Successfully analyzed class ${classMetadata.className} (${classAnalysisTime}ms)")
                    println("  - Properties: ${classMetadata.properties.size}")
                    println("  - Methods: ${classMetadata.methods.size}")
                    println("  - Annotations: ${classMetadata.annotations.size}")
                    successfulAnalyses++
                } else {
                    println("DDD Analyzer: Failed to analyze class ${irClass.name} - no metadata generated")
                    failedAnalyses++
                }
            } catch (e: Exception) {
                val classAnalysisTime = System.currentTimeMillis() - classAnalysisStart
                println("DDD Analyzer: Error analyzing class ${irClass.name} (${classAnalysisTime}ms): ${e.message}")
                
                // Only print stack trace in debug mode to reduce noise
                if (System.getProperty("ddd.analyzer.debug") == "true") {
                    e.printStackTrace()
                }
                
                failedAnalyses++
                
                // Fail fast if configured to do so
                if (failOnAnalysisErrors) {
                    throw RuntimeException("DDD Analysis failed for class ${irClass.name}", e)
                }
            }
        }
        
        val totalAnalysisTime = System.currentTimeMillis() - analysisStart
        println("DDD Analyzer: Completed analysis of ${dddAnnotatedClasses.size} classes (${totalAnalysisTime}ms)")
        println("  - Successful: $successfulAnalyses")
        println("  - Failed: $failedAnalyses")
        
        // Validate collected metadata
        val validationResult = metadataCollector.validateMetadata()
        if (!validationResult.isValid) {
            println("DDD Analyzer: Metadata validation failed with ${validationResult.errors.size} errors:")
            validationResult.errors.forEach { error ->
                when (error) {
                    is AnalysisError.MetadataCollectionError -> println("  - ERROR: ${error.className}: ${error.message}")
                    is AnalysisError.ClassAnalysisError -> println("  - ERROR: ${error.className}: ${error.message}")
                    else -> println("  - ERROR: ${error.message}")
                }
            }
        }
        
        if (validationResult.warnings.isNotEmpty()) {
            println("DDD Analyzer: Metadata validation warnings (${validationResult.warnings.size}):")
            validationResult.warnings.forEach { warning ->
                val classInfo = if (warning.className != null) "${warning.className}: " else ""
                println("  - WARNING: $classInfo${warning.message}")
            }
        }
        
        val mainSourcesCount = metadataCollector.getMainSourcesMetadata().size
        println("DDD Analyzer: Successfully collected metadata - Main: $mainSourcesCount")
        
        // Generate and write JSON files to META-INF directory
        try {
            generateAndWriteJsonFiles()
            println("DDD Analyzer: Successfully generated and packaged JSON metadata files for main sources")
        } catch (e: Exception) {
            println("DDD Analyzer: Error generating JSON files: ${e.message}")
            if (System.getProperty("ddd.analyzer.debug") == "true") {
                e.printStackTrace()
            }
        }
        
        // Report error summary
        val errors = errorReporter.getErrors()
        val warnings = errorReporter.getWarnings()
        
        if (errors.isNotEmpty()) {
            println("DDD Analyzer: Analysis completed with ${errors.size} errors and ${warnings.size} warnings")
            
            // Check if build should fail
            if (errorReporter.shouldFailBuild(errors)) {
                throw RuntimeException("DDD Analysis failed with critical errors. See logs for details.")
            }
        } else if (warnings.isNotEmpty()) {
            println("DDD Analyzer: Analysis completed with ${warnings.size} warnings")
        } else {
            println("DDD Analyzer: Analysis completed successfully with no errors or warnings")
        }
    }
    
    /**
     * Generates JSON files and writes them to fixed META-INF directory for JAR packaging
     * Fixed path: build/generated/pragmaddd/main/resources/META-INF/pragma-ddd-analyzer/domain-analyzer.json
     * This path is hardcoded and cannot be configured by users
     */
    private fun generateAndWriteJsonFiles() {
        // Use the output directory passed from Gradle plugin (should be absolute path)
        val actualOutputDir = outputDirectory ?: FIXED_OUTPUT_DIRECTORY
        
        println("DDD Analyzer: Using output directory: $actualOutputDir")
        println("DDD Analyzer: Current working directory: ${System.getProperty("user.dir")}")
        
        // The output directory from Gradle should already be absolute, but let's ensure it
        val resolvedOutputDir = java.io.File(actualOutputDir).let { file ->
            if (file.isAbsolute) {
                file.absolutePath
            } else {
                // If it's relative, resolve it against the current working directory
                java.io.File(System.getProperty("user.dir"), actualOutputDir).absolutePath
            }
        }
        
        println("DDD Analyzer: Resolved output directory: $resolvedOutputDir")
        
        // Verify the directory exists and create it if necessary
        val outputDirFile = java.io.File(resolvedOutputDir)
        if (!outputDirFile.exists()) {
            println("DDD Analyzer: Output directory does not exist, creating: $resolvedOutputDir")
            val created = outputDirFile.mkdirs()
            println("DDD Analyzer: Directory creation result: $created")
        }
        
        // Ensure output directory is writable
        if (!resourceWriter.isOutputDirectoryWritable(resolvedOutputDir)) {
            println("DDD Analyzer: Output directory is not writable: $resolvedOutputDir")
            println("DDD Analyzer: Directory exists: ${outputDirFile.exists()}")
            println("DDD Analyzer: Directory is directory: ${outputDirFile.isDirectory}")
            println("DDD Analyzer: Directory can write: ${outputDirFile.canWrite()}")
            return
        }
        
        // Generate and write main sources JSON if we have main source metadata
        val mainSourcesMetadata = metadataCollector.getMainSourcesMetadata()
        if (mainSourcesMetadata.isNotEmpty()) {
            println("DDD Analyzer: Generating JSON for ${mainSourcesMetadata.size} classes")
            val mainJson = jsonGenerator.generateMainSourcesJson(mainSourcesMetadata)
            
            // Write to the resolved output directory
            try {
                resourceWriter.writeJsonToResource(mainJson, "pragma-ddd-analyzer/domain-analyzer.json", resolvedOutputDir)
                println("DDD Analyzer: Successfully wrote JSON to: $resolvedOutputDir/META-INF/pragma-ddd-analyzer/domain-analyzer.json")
                
                // Verify the file was written successfully
                if (resourceWriter.verifyJsonFileWritten(resolvedOutputDir, "pragma-ddd-analyzer/domain-analyzer.json")) {
                    println("DDD Analyzer: JSON file verification successful")
                } else {
                    println("DDD Analyzer: Warning - JSON file verification failed")
                }
            } catch (e: Exception) {
                println("DDD Analyzer: Error writing JSON file: ${e.message}")
                e.printStackTrace()
            }
        } else {
            println("DDD Analyzer: No main source metadata to generate JSON for")
        }
        
        // List all generated JSON files for confirmation
        try {
            val jsonFiles = resourceWriter.listJsonFiles(resolvedOutputDir)
            if (jsonFiles.isNotEmpty()) {
                println("DDD Analyzer: Generated JSON files in META-INF/pragma-ddd-analyzer/ directory:")
                jsonFiles.forEach { file ->
                    println("  - ${file.name} (${file.length()} bytes) at ${file.absolutePath}")
                }
            } else {
                println("DDD Analyzer: No JSON files found in output directory")
            }
        } catch (e: Exception) {
            println("DDD Analyzer: Error listing JSON files: ${e.message}")
        }
    }
    

}