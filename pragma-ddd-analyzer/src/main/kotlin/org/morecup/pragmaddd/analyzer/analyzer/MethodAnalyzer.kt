package org.morecup.pragmaddd.analyzer.analyzer

import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.morecup.pragmaddd.analyzer.detector.AnnotationDetector
import org.morecup.pragmaddd.analyzer.model.*
import org.morecup.pragmaddd.analyzer.error.AnalysisError
import org.morecup.pragmaddd.analyzer.error.AnalysisWarning
import org.morecup.pragmaddd.analyzer.error.ErrorReporter

/**
 * Interface for analyzing method structure and extracting metadata
 */
interface MethodAnalyzer {
    fun analyzeMethod(irFunction: IrSimpleFunction): MethodMetadata?
    fun extractMethodCalls(irFunction: IrSimpleFunction): List<MethodCallMetadata>
    fun extractPropertyAccess(irFunction: IrSimpleFunction): List<PropertyAccessMetadata>
    fun extractDocumentation(irFunction: IrSimpleFunction): DocumentationMetadata?
}

/**
 * Implementation of MethodAnalyzer that analyzes IR method declarations
 * and extracts comprehensive metadata including method calls and property access patterns
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
class MethodAnalyzerImpl(
    private val annotationDetector: AnnotationDetector,
    private val documentationExtractor: DocumentationExtractor,
    private val propertyAnalyzer: PropertyAnalyzer,
    private val errorReporter: ErrorReporter,
    private val enableMethodAnalysis: Boolean = true
) : MethodAnalyzer {
    
    /**
     * Analyzes a method and generates comprehensive method metadata
     * Extracts method calls, property accesses, parameters, and documentation
     */
    override fun analyzeMethod(irFunction: IrSimpleFunction): MethodMetadata? {
        val methodName = irFunction.name.asString()
        val className = irFunction.parent.let { parent ->
            if (parent is org.jetbrains.kotlin.ir.declarations.IrClass) {
                parent.name.asString()
            } else {
                "Unknown"
            }
        }
        
        return try {
            val parameters = extractParameters(irFunction)
            val returnType = irFunction.returnType.getClass()?.name?.asString() ?: "Unit"
            val isPrivate = !irFunction.visibility.isPublicAPI
            
            // Extract method calls and property accesses using dedicated methods (if enabled)
            val methodCalls = if (enableMethodAnalysis) extractMethodCalls(irFunction) else emptyList()
            val propertyAccesses = if (enableMethodAnalysis) extractPropertyAccess(irFunction) else emptyList()
            
            val documentation = extractDocumentation(irFunction)
            val annotations = extractMethodAnnotations(irFunction)
            
            MethodMetadata(
                name = methodName,
                parameters = parameters,
                returnType = returnType,
                isPrivate = isPrivate,
                methodCalls = methodCalls,
                propertyAccesses = propertyAccesses,
                documentation = documentation,
                annotations = annotations
            )
        } catch (e: Exception) {
            errorReporter.reportError(
                AnalysisError.MethodAnalysisError(
                    className = className,
                    methodName = methodName,
                    message = "Failed to analyze method: ${e.message}",
                    cause = e
                )
            )
            null // Return null to allow compilation to continue
        }
    }
    
    /**
     * Extracts method calls from method body using IR visitor pattern
     */
    override fun extractMethodCalls(irFunction: IrSimpleFunction): List<MethodCallMetadata> {
        return try {
            val methodCallsCollector = MethodCallsCollector(errorReporter, irFunction.name.asString())
            irFunction.body?.acceptChildrenVoid(methodCallsCollector)
            methodCallsCollector.methodCalls
        } catch (e: Exception) {
            val className = irFunction.parent.let { parent ->
                if (parent is org.jetbrains.kotlin.ir.declarations.IrClass) {
                    parent.name.asString()
                } else {
                    "Unknown"
                }
            }
            errorReporter.reportWarning(
                AnalysisWarning(
                    message = "Failed to extract method calls: ${e.message}",
                    className = className,
                    elementName = irFunction.name.asString()
                )
            )
            emptyList()
        }
    }
    
    /**
     * Extracts property access patterns from method body using dedicated PropertyAnalyzer
     */
    override fun extractPropertyAccess(irFunction: IrSimpleFunction): List<PropertyAccessMetadata> {
        return try {
            propertyAnalyzer.extractPropertyAccess(irFunction)
        } catch (e: Exception) {
            val className = irFunction.parent.let { parent ->
                if (parent is org.jetbrains.kotlin.ir.declarations.IrClass) {
                    parent.name.asString()
                } else {
                    "Unknown"
                }
            }
            errorReporter.reportWarning(
                AnalysisWarning(
                    message = "Failed to extract property access: ${e.message}",
                    className = className,
                    elementName = irFunction.name.asString()
                )
            )
            emptyList()
        }
    }
    
    /**
     * Extracts method documentation from KDoc comments
     */
    override fun extractDocumentation(irFunction: IrSimpleFunction): DocumentationMetadata? {
        return try {
            documentationExtractor.extractMethodDocumentation(irFunction)
        } catch (e: Exception) {
            val className = irFunction.parent.let { parent ->
                if (parent is org.jetbrains.kotlin.ir.declarations.IrClass) {
                    parent.name.asString()
                } else {
                    "Unknown"
                }
            }
            errorReporter.reportWarning(
                AnalysisWarning(
                    message = "Failed to extract method documentation: ${e.message}",
                    className = className,
                    elementName = irFunction.name.asString()
                )
            )
            null
        }
    }
    
    /**
     * Extracts method parameters with type information and annotations
     */
    private fun extractParameters(irFunction: IrSimpleFunction): List<ParameterMetadata> {
        return irFunction.valueParameters.map { parameter ->
            ParameterMetadata(
                name = parameter.name.asString(),
                type = parameter.type.getClass()?.name?.asString() ?: "Unknown",
                annotations = extractParameterAnnotations(parameter)
            )
        }
    }
    
    /**
     * Extracts method-level annotations
     */
    private fun extractMethodAnnotations(irFunction: IrSimpleFunction): List<AnnotationMetadata> {
        val annotations = mutableListOf<AnnotationMetadata>()
        
        irFunction.annotations.forEach { annotation ->
            try {
                val annotationMetadata = annotationDetector.extractAnnotationMetadata(annotation)
                annotations.add(annotationMetadata)
            } catch (e: Exception) {
                val className = irFunction.parent.let { parent ->
                    if (parent is org.jetbrains.kotlin.ir.declarations.IrClass) {
                        parent.name.asString()
                    } else {
                        "Unknown"
                    }
                }
                errorReporter.reportWarning(
                    AnalysisWarning(
                        message = "Failed to extract method annotation metadata: ${e.message}",
                        className = className,
                        elementName = irFunction.name.asString()
                    )
                )
                // Continue with other annotations
            }
        }
        
        return annotations
    }
    
    /**
     * Extracts parameter-level annotations
     */
    private fun extractParameterAnnotations(parameter: IrValueParameter): List<AnnotationMetadata> {
        return parameter.annotations.map { annotation ->
            annotationDetector.extractAnnotationMetadata(annotation)
        }
    }
    
    /**
     * IR visitor for collecting method calls within method bodies
     * Implements comprehensive method call detection for various IR call expressions
     */
    private class MethodCallsCollector(
        private val errorReporter: ErrorReporter,
        private val methodName: String
    ) : IrElementVisitorVoid {
        val methodCalls = mutableListOf<MethodCallMetadata>()
        
        override fun visitCall(expression: IrCall) {
            super.visitCall(expression)
            
            try {
                val targetMethod = expression.symbol.owner.name.asString()
                val receiverType = expression.dispatchReceiver?.type?.getClass()?.fqNameWhenAvailable?.asString()
                    ?: expression.extensionReceiver?.type?.getClass()?.fqNameWhenAvailable?.asString()
                
                val parameters = (0 until expression.valueArgumentsCount).mapNotNull { index ->
                    expression.getValueArgument(index)?.type?.getClass()?.name?.asString()
                }
                
                methodCalls.add(
                    MethodCallMetadata(
                        targetMethod = targetMethod,
                        receiverType = receiverType,
                        parameters = parameters
                    )
                )
            } catch (e: Exception) {
                errorReporter.reportWarning(
                    AnalysisWarning(
                        message = "Error processing method call: ${e.message}",
                        elementName = methodName
                    )
                )
            }
        }
        
        override fun visitFunctionAccess(expression: IrFunctionAccessExpression) {
            super.visitFunctionAccess(expression)
            
            try {
                val targetMethod = expression.symbol.owner.name.asString()
                val receiverType = expression.dispatchReceiver?.type?.getClass()?.fqNameWhenAvailable?.asString()
                    ?: expression.extensionReceiver?.type?.getClass()?.fqNameWhenAvailable?.asString()
                
                val parameters = (0 until expression.valueArgumentsCount).mapNotNull { index ->
                    expression.getValueArgument(index)?.type?.getClass()?.name?.asString()
                }
                
                methodCalls.add(
                    MethodCallMetadata(
                        targetMethod = targetMethod,
                        receiverType = receiverType,
                        parameters = parameters
                    )
                )
            } catch (e: Exception) {
                errorReporter.reportWarning(
                    AnalysisWarning(
                        message = "Error processing function access: ${e.message}",
                        elementName = methodName
                    )
                )
            }
        }
        
        override fun visitConstructorCall(expression: IrConstructorCall) {
            super.visitConstructorCall(expression)
            
            try {
                val targetMethod = "<init>"
                val receiverType = expression.type.getClass()?.fqNameWhenAvailable?.asString()
                
                val parameters = (0 until expression.valueArgumentsCount).mapNotNull { index ->
                    expression.getValueArgument(index)?.type?.getClass()?.name?.asString()
                }
                
                methodCalls.add(
                    MethodCallMetadata(
                        targetMethod = targetMethod,
                        receiverType = receiverType,
                        parameters = parameters
                    )
                )
            } catch (e: Exception) {
                errorReporter.reportWarning(
                    AnalysisWarning(
                        message = "Error processing constructor call: ${e.message}",
                        elementName = methodName
                    )
                )
            }
        }
    }
    

}