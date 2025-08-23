package org.morecup.pragmaddd.analyzer.analyzer

import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.morecup.pragmaddd.analyzer.model.PropertyAccessMetadata
import org.morecup.pragmaddd.analyzer.model.PropertyAccessType

/**
 * Interface for analyzing property access patterns within method bodies
 */
interface PropertyAnalyzer {
    fun extractPropertyAccess(irFunction: IrSimpleFunction): List<PropertyAccessMetadata>
    fun detectDirectFieldAccess(irFunction: IrSimpleFunction): List<PropertyAccessMetadata>
    fun detectGetterSetterCalls(irFunction: IrSimpleFunction): List<PropertyAccessMetadata>
    fun detectMethodChainPropertyAccess(irFunction: IrSimpleFunction): List<PropertyAccessMetadata>
}

/**
 * Implementation of PropertyAnalyzer that analyzes IR method bodies
 * to detect various property access patterns including direct field access,
 * getter/setter method calls, and property access through method chains
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
class PropertyAnalyzerImpl : PropertyAnalyzer {
    
    /**
     * Extracts all property access patterns from method body using comprehensive analysis
     */
    override fun extractPropertyAccess(irFunction: IrSimpleFunction): List<PropertyAccessMetadata> {
        val methodName = irFunction.name.asString()
        val className = irFunction.parent.let { parent ->
            if (parent is org.jetbrains.kotlin.ir.declarations.IrClass) {
                parent.name.asString()
            } else "Unknown"
        }
        
        val accesses = mutableListOf<PropertyAccessMetadata>()
        
        // First, try to collect property accesses from IR
        val propertyAccessCollector = PropertyAccessCollector()
        irFunction.body?.acceptChildrenVoid(propertyAccessCollector)
        accesses.addAll(propertyAccessCollector.propertyAccesses)
        
        // Then, try to infer property access from method patterns
        val inferredAccesses = inferPropertyAccessFromMethodPattern(irFunction, className)
        accesses.addAll(inferredAccesses)
        
        return accesses.distinctBy { "${it.propertyName}_${it.accessType}_${it.ownerClass}" }
    }
    
    /**
     * Infers property access from method naming patterns and signatures
     */
    private fun inferPropertyAccessFromMethodPattern(irFunction: IrSimpleFunction, className: String): List<PropertyAccessMetadata> {
        val methodName = irFunction.name.asString()
        val accesses = mutableListOf<PropertyAccessMetadata>()
        
        // Get the class that contains this method
        val ownerClass = irFunction.parent.let { parent ->
            if (parent is org.jetbrains.kotlin.ir.declarations.IrClass) {
                parent.fqNameWhenAvailable?.asString()
            } else null
        }
        
        // Infer property access from getter methods
        if (methodName.startsWith("get") && irFunction.valueParameters.isEmpty()) {
            val propertyName = methodName.removePrefix("get").replaceFirstChar { it.lowercase() }
            
            // Check if there's a corresponding property in the class
            if (hasCorrespondingProperty(irFunction, propertyName)) {
                accesses.add(
                    PropertyAccessMetadata(
                        propertyName = propertyName,
                        accessType = PropertyAccessType.GET,
                        ownerClass = ownerClass
                    )
                )
            }
        }
        
        // Infer property access from setter methods
        if (methodName.startsWith("set") && irFunction.valueParameters.size == 1) {
            val propertyName = methodName.removePrefix("set").replaceFirstChar { it.lowercase() }
            
            // Check if there's a corresponding property in the class
            if (hasCorrespondingProperty(irFunction, propertyName)) {
                accesses.add(
                    PropertyAccessMetadata(
                        propertyName = propertyName,
                        accessType = PropertyAccessType.SET,
                        ownerClass = ownerClass
                    )
                )
            }
        }
        
        // Infer property access from Kotlin property getter methods (no "get" prefix)
        if (!methodName.startsWith("get") && !methodName.startsWith("set") && 
            !methodName.startsWith("<") && irFunction.valueParameters.isEmpty() &&
            irFunction.returnType.getClass()?.name?.asString() != "Unit") {
            
            // This might be a Kotlin property getter
            if (hasCorrespondingProperty(irFunction, methodName)) {
                accesses.add(
                    PropertyAccessMetadata(
                        propertyName = methodName,
                        accessType = PropertyAccessType.GET,
                        ownerClass = ownerClass
                    )
                )
            }
        }
        
        return accesses
    }
    
    /**
     * Checks if there's a corresponding property in the class for the given property name
     */
    private fun hasCorrespondingProperty(irFunction: IrSimpleFunction, propertyName: String): Boolean {
        val parentClass = irFunction.parent as? org.jetbrains.kotlin.ir.declarations.IrClass ?: return false
        
        // Check if there's a property with this name
        return parentClass.declarations.any { declaration ->
            when (declaration) {
                is org.jetbrains.kotlin.ir.declarations.IrProperty -> {
                    declaration.name.asString() == propertyName
                }
                is org.jetbrains.kotlin.ir.declarations.IrField -> {
                    declaration.name.asString() == propertyName
                }
                else -> false
            }
        }
    }
    
    /**
     * Detects direct field access (GETFIELD/PUTFIELD equivalent in IR)
     */
    override fun detectDirectFieldAccess(irFunction: IrSimpleFunction): List<PropertyAccessMetadata> {
        val directFieldAccessCollector = DirectFieldAccessCollector()
        irFunction.body?.acceptChildrenVoid(directFieldAccessCollector)
        return directFieldAccessCollector.propertyAccesses
    }
    
    /**
     * Detects getter/setter method call recognition
     */
    override fun detectGetterSetterCalls(irFunction: IrSimpleFunction): List<PropertyAccessMetadata> {
        val getterSetterCollector = GetterSetterCallCollector()
        irFunction.body?.acceptChildrenVoid(getterSetterCollector)
        return getterSetterCollector.propertyAccesses
    }
    
    /**
     * Detects property access through method chains
     */
    override fun detectMethodChainPropertyAccess(irFunction: IrSimpleFunction): List<PropertyAccessMetadata> {
        val methodChainCollector = MethodChainPropertyAccessCollector()
        irFunction.body?.acceptChildrenVoid(methodChainCollector)
        return methodChainCollector.propertyAccesses
    }
    
    /**
     * Comprehensive IR visitor for collecting all types of property access patterns
     */
    private class PropertyAccessCollector : IrElementVisitorVoid {
        val propertyAccesses = mutableListOf<PropertyAccessMetadata>()
        
        override fun visitGetField(expression: IrGetField) {
            super.visitGetField(expression)
            
            try {
                val propertyName = expression.symbol.owner.name.asString()
                val ownerClass = expression.symbol.owner.parent.let { parent ->
                    if (parent is org.jetbrains.kotlin.ir.declarations.IrClass) {
                        parent.fqNameWhenAvailable?.asString()
                    } else null
                }
                
                System.err.println("PropertyAnalyzer: Found direct field GET access - property: $propertyName, owner: $ownerClass")
                
                propertyAccesses.add(
                    PropertyAccessMetadata(
                        propertyName = propertyName,
                        accessType = PropertyAccessType.GET,
                        ownerClass = ownerClass
                    )
                )
            } catch (e: Exception) {
                // Log error but continue processing other accesses
                println("Error processing field read: ${e.message}")
            }
        }
        
        override fun visitSetField(expression: IrSetField) {
            super.visitSetField(expression)
            
            try {
                val propertyName = expression.symbol.owner.name.asString()
                val ownerClass = expression.symbol.owner.parent.let { parent ->
                    if (parent is org.jetbrains.kotlin.ir.declarations.IrClass) {
                        parent.fqNameWhenAvailable?.asString()
                    } else null
                }
                
                System.err.println("PropertyAnalyzer: Found direct field SET access - property: $propertyName, owner: $ownerClass")
                
                propertyAccesses.add(
                    PropertyAccessMetadata(
                        propertyName = propertyName,
                        accessType = PropertyAccessType.SET,
                        ownerClass = ownerClass
                    )
                )
            } catch (e: Exception) {
                // Log error but continue processing other accesses
                println("Error processing field write: ${e.message}")
            }
        }
        
        override fun visitGetValue(expression: IrGetValue) {
            super.visitGetValue(expression)
            
            try {
                // Handle local variable and parameter access - check if it's a property backing field
                val symbol = expression.symbol
                val valueDeclaration = symbol.owner
                
                System.err.println("PropertyAnalyzer: visitGetValue - symbol: ${symbol.owner.name}, type: ${valueDeclaration::class.simpleName}")
                
                // Check if this is accessing a property backing field
                if (valueDeclaration is org.jetbrains.kotlin.ir.declarations.IrValueParameter) {
                    // This might be accessing 'this' or a parameter - we're interested in 'this' for property access
                    if (valueDeclaration.name.asString() == "<this>") {
                        // This is accessing 'this', but we need to look at the context to determine property access
                        // This will be handled by visitCall for property getter calls
                        println("PropertyAnalyzer: Found 'this' access")
                    }
                } else if (valueDeclaration is org.jetbrains.kotlin.ir.declarations.IrVariable) {
                    val variableName = valueDeclaration.name.asString()
                    println("PropertyAnalyzer: Found variable access: $variableName")
                }
            } catch (e: Exception) {
                // Log error but continue processing other accesses
                println("Error processing value access: ${e.message}")
            }
        }
        
        override fun visitSetValue(expression: IrSetValue) {
            super.visitSetValue(expression)
            
            try {
                // Handle property writes through direct assignment
                val symbol = expression.symbol
                val valueDeclaration = symbol.owner
                
                // Check if this is a property assignment
                if (valueDeclaration is org.jetbrains.kotlin.ir.declarations.IrVariable) {
                    val variableName = valueDeclaration.name.asString()
                    // For now, we'll rely on visitCall to catch property setter calls
                }
            } catch (e: Exception) {
                // Log error but continue processing other accesses
                println("Error processing value write: ${e.message}")
            }
        }
        
        override fun visitCall(expression: IrCall) {
            super.visitCall(expression)
            
            try {
                val function = expression.symbol.owner
                val methodName = function.name.asString()
                
                // Get the receiver type (the class that owns the property)
                val receiverType = expression.dispatchReceiver?.type?.getClass()?.fqNameWhenAvailable?.asString()
                
                System.err.println("PropertyAnalyzer: Processing call to method '$methodName' on receiver '$receiverType'")
                
                // Check if this is a property getter call
                if (function.correspondingPropertySymbol != null) {
                    val property = function.correspondingPropertySymbol!!.owner
                    val propertyName = property.name.asString()
                    
                    // Determine if this is a getter or setter based on the function
                    val accessType = if (function == property.getter) {
                        PropertyAccessType.GET
                    } else if (function == property.setter) {
                        PropertyAccessType.SET
                    } else {
                        PropertyAccessType.GET // Default to GET if unclear
                    }
                    
                    println("PropertyAnalyzer: Found property access via correspondingPropertySymbol - property: $propertyName, access: $accessType")
                    
                    propertyAccesses.add(
                        PropertyAccessMetadata(
                            propertyName = propertyName,
                            accessType = accessType,
                            ownerClass = receiverType
                        )
                    )
                } else {
                    // Fallback: detect by method name patterns
                    when {
                        // Special Kotlin setter pattern (e.g., "<set-propertyName>")
                        methodName.startsWith("<set-") && methodName.endsWith(">") -> {
                            val propertyName = methodName.removePrefix("<set-").removeSuffix(">")
                            println("PropertyAnalyzer: Found Kotlin setter pattern - property: $propertyName")
                            propertyAccesses.add(
                                PropertyAccessMetadata(
                                    propertyName = propertyName,
                                    accessType = PropertyAccessType.SET,
                                    ownerClass = receiverType
                                )
                            )
                        }
                        
                        // Special Kotlin getter pattern (e.g., "<get-propertyName>")
                        methodName.startsWith("<get-") && methodName.endsWith(">") -> {
                            val propertyName = methodName.removePrefix("<get-").removeSuffix(">")
                            println("PropertyAnalyzer: Found Kotlin getter pattern - property: $propertyName")
                            propertyAccesses.add(
                                PropertyAccessMetadata(
                                    propertyName = propertyName,
                                    accessType = PropertyAccessType.GET,
                                    ownerClass = receiverType
                                )
                            )
                        }
                        
                        // Java-style getter (starts with "get", no parameters)
                        methodName.startsWith("get") && expression.valueArgumentsCount == 0 -> {
                            val propertyName = methodName.removePrefix("get").replaceFirstChar { it.lowercase() }
                            println("PropertyAnalyzer: Found Java-style getter - property: $propertyName")
                            propertyAccesses.add(
                                PropertyAccessMetadata(
                                    propertyName = propertyName,
                                    accessType = PropertyAccessType.GET,
                                    ownerClass = receiverType
                                )
                            )
                        }
                        
                        // Java-style setter (starts with "set", one parameter)
                        methodName.startsWith("set") && expression.valueArgumentsCount == 1 -> {
                            val propertyName = methodName.removePrefix("set").replaceFirstChar { it.lowercase() }
                            println("PropertyAnalyzer: Found Java-style setter - property: $propertyName")
                            propertyAccesses.add(
                                PropertyAccessMetadata(
                                    propertyName = propertyName,
                                    accessType = PropertyAccessType.SET,
                                    ownerClass = receiverType
                                )
                            )
                        }
                        
                        // Skip this overly broad rule that incorrectly identifies method calls as property access
                        // We'll rely on correspondingPropertySymbol and explicit getter/setter patterns instead
                        
                        else -> {
                            println("PropertyAnalyzer: Method '$methodName' does not match any property access pattern")
                        }
                    }
                }
                
                // Handle method chains and property access through receivers
                expression.dispatchReceiver?.let { receiver ->
                    System.err.println("PropertyAnalyzer: Found dispatchReceiver of type: ${receiver::class.simpleName}")
                    
                    when (receiver) {
                        is IrCall -> {
                            // This is a method chain, analyze the receiver call for property access
                            System.err.println("PropertyAnalyzer: Receiver is IrCall: ${receiver.symbol.owner.name}")
                            receiver.acceptChildrenVoid(this)
                        }
                        is IrGetField -> {
                            // Direct property access as receiver (e.g., items.add())
                            val propertyName = receiver.symbol.owner.name.asString()
                            val ownerClass = receiver.symbol.owner.parent.let { parent ->
                                if (parent is org.jetbrains.kotlin.ir.declarations.IrClass) {
                                    parent.fqNameWhenAvailable?.asString()
                                } else null
                            }
                            
                            System.err.println("PropertyAnalyzer: Found property access as receiver - property: $propertyName, owner: $ownerClass")
                            
                            propertyAccesses.add(
                                PropertyAccessMetadata(
                                    propertyName = propertyName,
                                    accessType = PropertyAccessType.GET,
                                    ownerClass = ownerClass
                                )
                            )
                        }
                        is IrGetValue -> {
                            // This might be a property access through a getter method
                            val symbol = receiver.symbol
                            val valueDeclaration = symbol.owner
                            System.err.println("PropertyAnalyzer: Receiver is IrGetValue: ${valueDeclaration.name}")
                            
                            // Check if this is accessing a property through 'this'
                            if (valueDeclaration is org.jetbrains.kotlin.ir.declarations.IrValueParameter && 
                                valueDeclaration.name.asString() == "<this>") {
                                // This might be accessing a property on 'this', but we need more context
                                System.err.println("PropertyAnalyzer: Receiver accesses 'this' - method: $methodName")
                            }
                        }
                        else -> {
                            System.err.println("PropertyAnalyzer: Unknown receiver type: ${receiver::class.simpleName}")
                        }
                    }
                }
                
            } catch (e: Exception) {
                // Log error but continue processing other calls
                println("Error processing method call for property access: ${e.message}")
            }
        }
        
        override fun visitReturn(expression: IrReturn) {
            super.visitReturn(expression)
            
            try {
                System.err.println("PropertyAnalyzer: Found return statement")
                
                // Check if the return value is a property access
                val returnValue = expression.value
                when (returnValue) {
                    is IrGetField -> {
                        val propertyName = returnValue.symbol.owner.name.asString()
                        val ownerClass = returnValue.symbol.owner.parent.let { parent ->
                            if (parent is org.jetbrains.kotlin.ir.declarations.IrClass) {
                                parent.fqNameWhenAvailable?.asString()
                            } else null
                        }
                        
                        println("PropertyAnalyzer: Return statement accesses field - property: $propertyName, owner: $ownerClass")
                        
                        propertyAccesses.add(
                            PropertyAccessMetadata(
                                propertyName = propertyName,
                                accessType = PropertyAccessType.GET,
                                ownerClass = ownerClass
                            )
                        )
                    }
                    is IrCall -> {
                        // The return value is a method call, analyze it for property access
                        println("PropertyAnalyzer: Return statement calls method: ${returnValue.symbol.owner.name}")
                        returnValue.acceptChildrenVoid(this)
                    }
                    is IrGetValue -> {
                        val symbol = returnValue.symbol
                        val valueDeclaration = symbol.owner
                        println("PropertyAnalyzer: Return statement gets value: ${valueDeclaration.name}")
                    }
                    else -> {
                        println("PropertyAnalyzer: Return statement with type: ${returnValue::class.simpleName}")
                    }
                }
            } catch (e: Exception) {
                println("Error processing return statement: ${e.message}")
            }
        }
    }
    
    /**
     * Specialized visitor for detecting direct field access (GETFIELD/PUTFIELD equivalent)
     */
    private class DirectFieldAccessCollector : IrElementVisitorVoid {
        val propertyAccesses = mutableListOf<PropertyAccessMetadata>()
        
        override fun visitGetField(expression: IrGetField) {
            super.visitGetField(expression)
            
            try {
                val propertyName = expression.symbol.owner.name.asString()
                val ownerClass = expression.symbol.owner.parent.let { parent ->
                    if (parent is org.jetbrains.kotlin.ir.declarations.IrClass) {
                        parent.fqNameWhenAvailable?.asString()
                    } else null
                }
                
                propertyAccesses.add(
                    PropertyAccessMetadata(
                        propertyName = propertyName,
                        accessType = PropertyAccessType.GET,
                        ownerClass = ownerClass
                    )
                )
            } catch (e: Exception) {
                println("Error processing direct field read: ${e.message}")
            }
        }
        
        override fun visitSetField(expression: IrSetField) {
            super.visitSetField(expression)
            
            try {
                val propertyName = expression.symbol.owner.name.asString()
                val ownerClass = expression.symbol.owner.parent.let { parent ->
                    if (parent is org.jetbrains.kotlin.ir.declarations.IrClass) {
                        parent.fqNameWhenAvailable?.asString()
                    } else null
                }
                
                propertyAccesses.add(
                    PropertyAccessMetadata(
                        propertyName = propertyName,
                        accessType = PropertyAccessType.SET,
                        ownerClass = ownerClass
                    )
                )
            } catch (e: Exception) {
                println("Error processing direct field write: ${e.message}")
            }
        }
    }
    
    /**
     * Specialized visitor for detecting getter/setter method calls
     */
    private class GetterSetterCallCollector : IrElementVisitorVoid {
        val propertyAccesses = mutableListOf<PropertyAccessMetadata>()
        
        override fun visitCall(expression: IrCall) {
            super.visitCall(expression)
            
            try {
                val methodName = expression.symbol.owner.name.asString()
                val receiverType = expression.dispatchReceiver?.type?.getClass()?.fqNameWhenAvailable?.asString()
                
                // Check if this is a getter call (starts with "get" and has no parameters)
                if (methodName.startsWith("get") && expression.valueArgumentsCount == 0) {
                    val propertyName = methodName.removePrefix("get").replaceFirstChar { it.lowercase() }
                    propertyAccesses.add(
                        PropertyAccessMetadata(
                            propertyName = propertyName,
                            accessType = PropertyAccessType.GET,
                            ownerClass = receiverType
                        )
                    )
                }
                
                // Check if this is a setter call (starts with "set" and has one parameter)
                if (methodName.startsWith("set") && expression.valueArgumentsCount == 1) {
                    val propertyName = methodName.removePrefix("set").replaceFirstChar { it.lowercase() }
                    propertyAccesses.add(
                        PropertyAccessMetadata(
                            propertyName = propertyName,
                            accessType = PropertyAccessType.SET,
                            ownerClass = receiverType
                        )
                    )
                }
                
                // Also check for Kotlin property access patterns (property names directly)
                if (expression.valueArgumentsCount == 0 && !methodName.startsWith("get") && !methodName.startsWith("set")) {
                    // This might be a Kotlin property getter
                    propertyAccesses.add(
                        PropertyAccessMetadata(
                            propertyName = methodName,
                            accessType = PropertyAccessType.GET,
                            ownerClass = receiverType
                        )
                    )
                }
                
            } catch (e: Exception) {
                println("Error processing getter/setter call: ${e.message}")
            }
        }
    }
    
    /**
     * Specialized visitor for detecting property access through method chains
     */
    private class MethodChainPropertyAccessCollector : IrElementVisitorVoid {
        val propertyAccesses = mutableListOf<PropertyAccessMetadata>()
        
        override fun visitCall(expression: IrCall) {
            super.visitCall(expression)
            
            try {
                // Handle method chains - if the receiver is another method call
                expression.dispatchReceiver?.let { receiver ->
                    when (receiver) {
                        is IrCall -> {
                            // This is a method chain, analyze the receiver call
                            val receiverMethodName = receiver.symbol.owner.name.asString()
                            val receiverType = receiver.dispatchReceiver?.type?.getClass()?.fqNameWhenAvailable?.asString()
                            
                            // Check if the receiver method is a property getter
                            if (receiverMethodName.startsWith("get") && receiver.valueArgumentsCount == 0) {
                                val propertyName = receiverMethodName.removePrefix("get").replaceFirstChar { it.lowercase() }
                                propertyAccesses.add(
                                    PropertyAccessMetadata(
                                        propertyName = propertyName,
                                        accessType = PropertyAccessType.GET,
                                        ownerClass = receiverType
                                    )
                                )
                            } else if (receiver.valueArgumentsCount == 0) {
                                // Kotlin property access
                                propertyAccesses.add(
                                    PropertyAccessMetadata(
                                        propertyName = receiverMethodName,
                                        accessType = PropertyAccessType.GET,
                                        ownerClass = receiverType
                                    )
                                )
                            }
                            
                            // Recursively analyze the receiver for deeper chains
                            receiver.acceptChildrenVoid(this)
                        }
                        is IrGetField -> {
                            // Direct field access in method chain
                            val propertyName = receiver.symbol.owner.name.asString()
                            val ownerClass = receiver.symbol.owner.parent.let { parent ->
                                if (parent is org.jetbrains.kotlin.ir.declarations.IrClass) {
                                    parent.fqNameWhenAvailable?.asString()
                                } else null
                            }
                            
                            propertyAccesses.add(
                                PropertyAccessMetadata(
                                    propertyName = propertyName,
                                    accessType = PropertyAccessType.GET,
                                    ownerClass = ownerClass
                                )
                            )
                        }
                    }
                }
                
                // Also check the current method call for property access patterns
                val methodName = expression.symbol.owner.name.asString()
                val receiverType = expression.dispatchReceiver?.type?.getClass()?.fqNameWhenAvailable?.asString()
                
                if (methodName.startsWith("get") && expression.valueArgumentsCount == 0) {
                    val propertyName = methodName.removePrefix("get").replaceFirstChar { it.lowercase() }
                    propertyAccesses.add(
                        PropertyAccessMetadata(
                            propertyName = propertyName,
                            accessType = PropertyAccessType.GET,
                            ownerClass = receiverType
                        )
                    )
                } else if (methodName.startsWith("set") && expression.valueArgumentsCount == 1) {
                    val propertyName = methodName.removePrefix("set").replaceFirstChar { it.lowercase() }
                    propertyAccesses.add(
                        PropertyAccessMetadata(
                            propertyName = propertyName,
                            accessType = PropertyAccessType.SET,
                            ownerClass = receiverType
                        )
                    )
                }
                
            } catch (e: Exception) {
                println("Error processing method chain property access: ${e.message}")
            }
        }
    }
}