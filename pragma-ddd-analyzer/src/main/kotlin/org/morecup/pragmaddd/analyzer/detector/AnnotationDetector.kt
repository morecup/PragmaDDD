package org.morecup.pragmaddd.analyzer.detector

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.name.FqName
import org.morecup.pragmaddd.analyzer.model.AnnotationMetadata
import org.morecup.pragmaddd.analyzer.model.DddAnnotationType

/**
 * Annotation detector interface for DDD annotations
 */
interface AnnotationDetector {
    fun hasAggregateRootAnnotation(irClass: IrClass): Boolean
    fun hasDomainEntityAnnotation(irClass: IrClass): Boolean
    fun hasValueObjAnnotation(irClass: IrClass): Boolean
    fun getDddAnnotationType(irClass: IrClass): DddAnnotationType?
    fun hasDddAnnotation(irClass: IrClass): Boolean
    fun extractAnnotationMetadata(irConstructorCall: IrConstructorCall): AnnotationMetadata
    fun extractAllAnnotationMetadata(irClass: IrClass): List<AnnotationMetadata>
}

/**
 * Implementation of annotation detector for DDD-related annotations
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
class AnnotationDetectorImpl : AnnotationDetector {
    
    companion object {
        private val AGGREGATE_ROOT_FQ_NAME = FqName("org.morecup.pragmaddd.core.annotation.AggregateRoot")
        private val DOMAIN_ENTITY_FQ_NAME = FqName("org.morecup.pragmaddd.core.annotation.DomainEntity")
        private val VALUE_OBJ_FQ_NAME = FqName("org.morecup.pragmaddd.core.annotation.ValueObj")
    }
    
    /**
     * Checks if a class has the @AggregateRoot annotation
     */
    override fun hasAggregateRootAnnotation(irClass: IrClass): Boolean {
        return irClass.hasAnnotation(AGGREGATE_ROOT_FQ_NAME)
    }
    
    /**
     * Checks if a class has the @DomainEntity annotation
     */
    override fun hasDomainEntityAnnotation(irClass: IrClass): Boolean {
        return irClass.hasAnnotation(DOMAIN_ENTITY_FQ_NAME)
    }
    
    /**
     * Checks if a class has the @ValueObj annotation
     */
    override fun hasValueObjAnnotation(irClass: IrClass): Boolean {
        return irClass.hasAnnotation(VALUE_OBJ_FQ_NAME)
    }
    
    /**
     * Gets the DDD annotation type for a class
     */
    override fun getDddAnnotationType(irClass: IrClass): DddAnnotationType? {
        return when {
            hasAggregateRootAnnotation(irClass) -> DddAnnotationType.AGGREGATE_ROOT
            hasDomainEntityAnnotation(irClass) -> DddAnnotationType.DOMAIN_ENTITY
            hasValueObjAnnotation(irClass) -> DddAnnotationType.VALUE_OBJ
            else -> null
        }
    }
    
    /**
     * Checks if a class has any DDD annotation
     */
    override fun hasDddAnnotation(irClass: IrClass): Boolean {
        return hasAggregateRootAnnotation(irClass) || 
               hasDomainEntityAnnotation(irClass) || 
               hasValueObjAnnotation(irClass)
    }
    
    /**
     * Extracts annotation metadata from IR constructor calls
     */
    override fun extractAnnotationMetadata(irConstructorCall: IrConstructorCall): AnnotationMetadata {
        val constructor = irConstructorCall.symbol.owner
        val annotationClass = constructor.parent as? org.jetbrains.kotlin.ir.declarations.IrClass
        val annotationName = annotationClass?.name?.asString() ?: "Unknown"
        
        // Extract annotation parameters
        val parameters = mutableMapOf<String, Any>()
        
        // Iterate through constructor parameters and their corresponding values
        for (i in 0 until irConstructorCall.valueArgumentsCount) {
            val valueArgument = irConstructorCall.getValueArgument(i)
            val parameterName = constructor.valueParameters.getOrNull(i)?.name?.asString()
            
            if (parameterName != null && valueArgument != null) {
                val parameterValue = extractParameterValue(valueArgument)
                if (parameterValue != null) {
                    parameters[parameterName] = parameterValue
                }
            }
        }
        
        return AnnotationMetadata(
            name = annotationName,
            parameters = parameters
        )
    }
    
    /**
     * Extracts all annotation metadata from a class
     */
    override fun extractAllAnnotationMetadata(irClass: IrClass): List<AnnotationMetadata> {
        return irClass.annotations.map { annotation ->
            extractAnnotationMetadata(annotation)
        }
    }
    
    /**
     * Extracts parameter value from IR expression
     */
    private fun extractParameterValue(expression: org.jetbrains.kotlin.ir.expressions.IrExpression): Any? {
        return when (expression) {
            is IrConst -> {
                // Handle constant values (strings, numbers, booleans, etc.)
                expression.value
            }
            is IrGetValue -> {
                // Handle references to values (could be enum constants, etc.)
                expression.symbol.owner.name.asString()
            }
            else -> {
                // For more complex expressions, we might need additional handling
                // For now, return a string representation
                expression.toString()
            }
        }
    }
}