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
        val propertyAccessCollector = PropertyAccessCollector()
        irFunction.body?.acceptChildrenVoid(propertyAccessCollector)
        return propertyAccessCollector.propertyAccesses
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
                
                propertyAccesses.add(
                    PropertyAccessMetadata(
                        propertyName = propertyName,
                        accessType = PropertyAccessType.READ,
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
                
                propertyAccesses.add(
                    PropertyAccessMetadata(
                        propertyName = propertyName,
                        accessType = PropertyAccessType.WRITE,
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
                // Handle property access through getter calls
                val symbol = expression.symbol
                if (symbol.owner is org.jetbrains.kotlin.ir.declarations.IrProperty) {
                    val property = symbol.owner as org.jetbrains.kotlin.ir.declarations.IrProperty
                    val propertyName = property.name.asString()
                    val ownerClass = property.parent.let { parent ->
                        if (parent is org.jetbrains.kotlin.ir.declarations.IrClass) {
                            parent.fqNameWhenAvailable?.asString()
                        } else null
                    }
                    
                    propertyAccesses.add(
                        PropertyAccessMetadata(
                            propertyName = propertyName,
                            accessType = PropertyAccessType.READ,
                            ownerClass = ownerClass
                        )
                    )
                }
            } catch (e: Exception) {
                // Log error but continue processing other accesses
                println("Error processing value access: ${e.message}")
            }
        }
        
        override fun visitSetValue(expression: IrSetValue) {
            super.visitSetValue(expression)
            
            try {
                // Handle property writes through setter calls
                val symbol = expression.symbol
                if (symbol.owner is org.jetbrains.kotlin.ir.declarations.IrProperty) {
                    val property = symbol.owner as org.jetbrains.kotlin.ir.declarations.IrProperty
                    val propertyName = property.name.asString()
                    val ownerClass = property.parent.let { parent ->
                        if (parent is org.jetbrains.kotlin.ir.declarations.IrClass) {
                            parent.fqNameWhenAvailable?.asString()
                        } else null
                    }
                    
                    propertyAccesses.add(
                        PropertyAccessMetadata(
                            propertyName = propertyName,
                            accessType = PropertyAccessType.WRITE,
                            ownerClass = ownerClass
                        )
                    )
                }
            } catch (e: Exception) {
                // Log error but continue processing other accesses
                println("Error processing value write: ${e.message}")
            }
        }
        
        override fun visitCall(expression: IrCall) {
            super.visitCall(expression)
            
            try {
                // Detect getter/setter method calls
                val methodName = expression.symbol.owner.name.asString()
                val receiverType = expression.dispatchReceiver?.type?.getClass()?.fqNameWhenAvailable?.asString()
                
                // Check if this is a getter call (starts with "get" and has no parameters)
                if (methodName.startsWith("get") && expression.valueArgumentsCount == 0) {
                    val propertyName = methodName.removePrefix("get").replaceFirstChar { it.lowercase() }
                    propertyAccesses.add(
                        PropertyAccessMetadata(
                            propertyName = propertyName,
                            accessType = PropertyAccessType.READ,
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
                            accessType = PropertyAccessType.WRITE,
                            ownerClass = receiverType
                        )
                    )
                }
                
                // Handle method chains - if the receiver is another method call
                expression.dispatchReceiver?.let { receiver ->
                    if (receiver is IrCall) {
                        // This is a method chain, analyze the receiver call for property access
                        receiver.acceptChildrenVoid(this)
                    }
                }
                
            } catch (e: Exception) {
                // Log error but continue processing other calls
                println("Error processing method call for property access: ${e.message}")
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
                        accessType = PropertyAccessType.READ,
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
                        accessType = PropertyAccessType.WRITE,
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
                            accessType = PropertyAccessType.READ,
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
                            accessType = PropertyAccessType.WRITE,
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
                            accessType = PropertyAccessType.READ,
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
                                        accessType = PropertyAccessType.READ,
                                        ownerClass = receiverType
                                    )
                                )
                            } else if (receiver.valueArgumentsCount == 0) {
                                // Kotlin property access
                                propertyAccesses.add(
                                    PropertyAccessMetadata(
                                        propertyName = receiverMethodName,
                                        accessType = PropertyAccessType.READ,
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
                                    accessType = PropertyAccessType.READ,
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
                            accessType = PropertyAccessType.READ,
                            ownerClass = receiverType
                        )
                    )
                } else if (methodName.startsWith("set") && expression.valueArgumentsCount == 1) {
                    val propertyName = methodName.removePrefix("set").replaceFirstChar { it.lowercase() }
                    propertyAccesses.add(
                        PropertyAccessMetadata(
                            propertyName = propertyName,
                            accessType = PropertyAccessType.WRITE,
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