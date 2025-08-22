package org.morecup.pragmaddd.analyzer.analyzer

import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.isPropertyAccessor
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.morecup.pragmaddd.analyzer.detector.AnnotationDetector
import org.morecup.pragmaddd.analyzer.model.*
import org.morecup.pragmaddd.analyzer.error.AnalysisError
import org.morecup.pragmaddd.analyzer.error.AnalysisWarning
import org.morecup.pragmaddd.analyzer.error.ErrorReporter

/**
 * Interface for analyzing class structure and extracting metadata
 */
interface ClassAnalyzer {
    fun analyzeClass(irClass: IrClass): ClassMetadata?
    fun extractProperties(irClass: IrClass): List<PropertyMetadata>
    fun extractMethods(irClass: IrClass): List<MethodMetadata>
    fun extractDocumentation(irClass: IrClass): DocumentationMetadata?
}

/**
 * Implementation of ClassAnalyzer that analyzes IR class declarations
 * and extracts comprehensive metadata including properties, methods, and documentation
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
class ClassAnalyzerImpl(
    private val annotationDetector: AnnotationDetector,
    private val methodAnalyzer: MethodAnalyzer,
    private val documentationExtractor: DocumentationExtractor,
    private val errorReporter: ErrorReporter
) : ClassAnalyzer {
    
    /**
     * Analyzes a class and generates comprehensive class metadata
     * Only processes classes with DDD annotations (@AggregateRoot, @DomainEntity, @ValueObj)
     */
    override fun analyzeClass(irClass: IrClass): ClassMetadata? {
        val className = irClass.name.asString()
        
        return try {
            val annotationType = annotationDetector.getDddAnnotationType(irClass)
                ?: return null // Return null if no DDD annotation is present
            
            val packageName = irClass.fqNameWhenAvailable?.parent()?.asString() ?: ""
            
            val properties = extractProperties(irClass)
            val methods = extractMethods(irClass)
            val documentation = extractDocumentation(irClass)
            val annotations = extractClassAnnotations(irClass)
            
            ClassMetadata(
                className = className,
                packageName = packageName,
                annotationType = annotationType,
                properties = properties,
                methods = methods,
                documentation = documentation,
                annotations = annotations
            )
        } catch (e: Exception) {
            errorReporter.reportError(
                AnalysisError.ClassAnalysisError(
                    className = className,
                    message = "Failed to analyze class: ${e.message}",
                    cause = e
                )
            )
            null // Return null to allow compilation to continue
        }
    }
    
    /**
     * Extracts property metadata from IR class declarations
     * Handles both property declarations and standalone field declarations
     */
    override fun extractProperties(irClass: IrClass): List<PropertyMetadata> {
        val properties = mutableListOf<PropertyMetadata>()
        val className = irClass.name.asString()
        
        // Analyze property declarations
        irClass.declarations.filterIsInstance<IrProperty>().forEach { property ->
            try {
                val propertyMetadata = PropertyMetadata(
                    name = property.name.asString(),
                    type = property.backingField?.type?.getClass()?.name?.asString() 
                        ?: property.getter?.returnType?.getClass()?.name?.asString() 
                        ?: "Unknown",
                    isPrivate = !property.visibility.isPublicAPI,
                    isMutable = property.isVar,
                    documentation = documentationExtractor.extractPropertyDocumentation(property),
                    annotations = extractPropertyAnnotations(property)
                )
                properties.add(propertyMetadata)
            } catch (e: Exception) {
                errorReporter.reportError(
                    AnalysisError.PropertyAnalysisError(
                        className = className,
                        propertyName = property.name.asString(),
                        message = "Failed to analyze property: ${e.message}",
                        cause = e
                    )
                )
                // Continue with other properties
            }
        }
        
        // Analyze field declarations (for fields without corresponding properties)
        irClass.declarations.filterIsInstance<IrField>().forEach { field ->
            try {
                // Check if this field has already been processed through a property
                val hasCorrespondingProperty = irClass.declarations
                    .filterIsInstance<IrProperty>()
                    .any { it.backingField == field }
                
                if (!hasCorrespondingProperty) {
                    val fieldMetadata = PropertyMetadata(
                        name = field.name.asString(),
                        type = field.type.getClass()?.name?.asString() ?: "Unknown",
                        isPrivate = !field.visibility.isPublicAPI,
                        isMutable = !field.isFinal,
                        documentation = documentationExtractor.extractFieldDocumentation(field),
                        annotations = extractFieldAnnotations(field)
                    )
                    properties.add(fieldMetadata)
                }
            } catch (e: Exception) {
                errorReporter.reportError(
                    AnalysisError.PropertyAnalysisError(
                        className = className,
                        propertyName = field.name.asString(),
                        message = "Failed to analyze field: ${e.message}",
                        cause = e
                    )
                )
                // Continue with other fields
            }
        }
        
        return properties
    }
    
    /**
     * Extracts method metadata from IR class declarations
     * Filters out property accessors (getters/setters) to focus on business methods
     */
    override fun extractMethods(irClass: IrClass): List<MethodMetadata> {
        val methods = mutableListOf<MethodMetadata>()
        val className = irClass.name.asString()
        
        irClass.declarations
            .filterIsInstance<IrSimpleFunction>()
            .filter { !it.isPropertyAccessor } // Filter out property accessors
            .forEach { function ->
                try {
                    val methodMetadata = methodAnalyzer.analyzeMethod(function)
                    if (methodMetadata != null) {
                        methods.add(methodMetadata)
                    }
                } catch (e: Exception) {
                    errorReporter.reportError(
                        AnalysisError.MethodAnalysisError(
                            className = className,
                            methodName = function.name.asString(),
                            message = "Failed to analyze method: ${e.message}",
                            cause = e
                        )
                    )
                    // Continue with other methods
                }
            }
        
        return methods
    }
    
    /**
     * Extracts class-level documentation from KDoc comments
     */
    override fun extractDocumentation(irClass: IrClass): DocumentationMetadata? {
        return try {
            documentationExtractor.extractClassDocumentation(irClass)
        } catch (e: Exception) {
            errorReporter.reportWarning(
                AnalysisWarning(
                    message = "Failed to extract class documentation: ${e.message}",
                    className = irClass.name.asString()
                )
            )
            null
        }
    }
    
    /**
     * Extracts class-level annotation metadata
     */
    private fun extractClassAnnotations(irClass: IrClass): List<AnnotationMetadata> {
        val annotations = mutableListOf<AnnotationMetadata>()
        val className = irClass.name.asString()
        
        irClass.annotations.forEach { annotation ->
            try {
                val annotationMetadata = annotationDetector.extractAnnotationMetadata(annotation)
                annotations.add(annotationMetadata)
            } catch (e: Exception) {
                errorReporter.reportWarning(
                    AnalysisWarning(
                        message = "Failed to extract annotation metadata: ${e.message}",
                        className = className
                    )
                )
                // Continue with other annotations
            }
        }
        
        return annotations
    }
    
    /**
     * Extracts property-level annotation metadata
     */
    private fun extractPropertyAnnotations(property: IrProperty): List<AnnotationMetadata> {
        val annotations = mutableListOf<AnnotationMetadata>()
        
        property.annotations.forEach { annotation ->
            try {
                val annotationMetadata = annotationDetector.extractAnnotationMetadata(annotation)
                annotations.add(annotationMetadata)
            } catch (e: Exception) {
                errorReporter.reportWarning(
                    AnalysisWarning(
                        message = "Failed to extract property annotation metadata: ${e.message}",
                        elementName = property.name.asString()
                    )
                )
                // Continue with other annotations
            }
        }
        
        return annotations
    }
    
    /**
     * Extracts field-level annotation metadata
     */
    private fun extractFieldAnnotations(field: IrField): List<AnnotationMetadata> {
        val annotations = mutableListOf<AnnotationMetadata>()
        
        field.annotations.forEach { annotation ->
            try {
                val annotationMetadata = annotationDetector.extractAnnotationMetadata(annotation)
                annotations.add(annotationMetadata)
            } catch (e: Exception) {
                errorReporter.reportWarning(
                    AnalysisWarning(
                        message = "Failed to extract field annotation metadata: ${e.message}",
                        elementName = field.name.asString()
                    )
                )
                // Continue with other annotations
            }
        }
        
        return annotations
    }
}