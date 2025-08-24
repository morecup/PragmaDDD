package org.morecup.pragmaddd.analyzer

import org.gradle.api.internal.tasks.testing.junit.JUnitTestEventAdapter.methodName
import org.gradle.internal.impldep.org.junit.experimental.ParallelComputer.methods
import org.objectweb.asm.*

/**
 * 方法访问器，用于分析方法内的属性访问和方法调用
 */
class PropertyAccessMethodVisitor(
    private val className: String,
    private val methodName: String,
    private val methodDescriptor: String,
    private val methods: MutableList<PropertyAccessInfo>,
    private val dddAnnotatedClasses: Map<String, Set<String>> = emptyMap() // 类名 -> 注解集合
) : MethodVisitor(Opcodes.ASM9) {
    
    private val accessedProperties = mutableSetOf<String>()
    private val modifiedProperties = mutableSetOf<String>()
    private val calledMethods = mutableMapOf<String, MethodCallInfo>()
    private val lambdaExpressions = mutableSetOf<LambdaInfo>()
    private val externalPropertyAccesses = mutableSetOf<ExternalPropertyAccessInfo>()
    
    // 用于跟踪指令顺序和关联
    private val instructionSequence = mutableListOf<InstructionInfo>()
    
    private sealed class InstructionInfo {
        data class MethodCall(val methodKey: String, val methodCall: MethodCallInfo) : InstructionInfo()
        data class LambdaCreation(val lambdaInfo: LambdaInfo) : InstructionInfo()
    }
    
    override fun visitFieldInsn(opcode: Int, owner: String, name: String, descriptor: String) {
        val ownerClassName = owner.replace('/', '.')
        
        if (ownerClassName == className) {
            // 当前类的字段访问
            when (opcode) {
                Opcodes.GETFIELD -> {
                    accessedProperties.add(name)
                }
                Opcodes.PUTFIELD -> {
                    modifiedProperties.add(name)
                }
            }
        } else {
            // 外部类的字段访问 - 检查是否是DDD注解类
            val annotations = dddAnnotatedClasses[ownerClassName]
            if (annotations != null && annotations.isNotEmpty()) {
                val accessType = when (opcode) {
                    Opcodes.GETFIELD -> PropertyAccessType.READ
                    Opcodes.PUTFIELD -> PropertyAccessType.WRITE
                    else -> null
                }
                
                if (accessType != null) {
                    val externalAccess = ExternalPropertyAccessInfo(
                        targetClassName = ownerClassName,
                        propertyName = name,
                        accessType = accessType,
                        hasAggregateRootAnnotation = annotations.contains("AggregateRoot"),
                        hasDomainEntityAnnotation = annotations.contains("DomainEntity"),
                        hasValueObjectAnnotation = annotations.contains("ValueObject")
                    )
                    externalPropertyAccesses.add(externalAccess)
                }
            }
        }
        super.visitFieldInsn(opcode, owner, name, descriptor)
    }
    
    override fun visitMethodInsn(
        opcode: Int,
        owner: String,
        name: String,
        descriptor: String,
        isInterface: Boolean
    ) {
        val ownerClassName = owner.replace('/', '.')

        // 记录所有方法调用，不进行过滤
        val methodKey = "$ownerClassName.$name$descriptor"
        val methodCallInfo = MethodCallInfo(ownerClassName, name, descriptor, 1, emptySet())
        
        val existingCall = calledMethods[methodKey]
        if (existingCall != null) {
            // 如果已经记录过这个方法调用，增加调用次数
            calledMethods[methodKey] = existingCall.copy(callCount = existingCall.callCount + 1)
        } else {
            // 新的方法调用，分别记录类名和方法名
            calledMethods[methodKey] = methodCallInfo
        }
        
        // 记录指令序列
        instructionSequence.add(InstructionInfo.MethodCall(methodKey, methodCallInfo))
        
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
    }
    
    override fun visitInvokeDynamicInsn(
        name: String,
        descriptor: String,
        bootstrapMethodHandle: Handle,
        vararg bootstrapMethodArguments: Any
    ) {
        // 检查是否是 Lambda 表达式
        if (isLambdaBootstrap(bootstrapMethodHandle)) {
            val lambdaType = extractLambdaType(descriptor)
            
            // 查找 Lambda 实现方法
            bootstrapMethodArguments.filterIsInstance<Handle>().forEach { handle ->
                when (handle.tag) {
                    Opcodes.H_INVOKESTATIC,
                    Opcodes.H_INVOKEVIRTUAL,
                    Opcodes.H_INVOKESPECIAL,
                    Opcodes.H_INVOKEINTERFACE -> {
                        val lambdaInfo = LambdaInfo(
                            className = handle.owner.replace('/', '.'),
                            methodName = handle.name,
                            methodDescriptor = handle.desc,
                            lambdaType = lambdaType
                        )
                        
                        lambdaExpressions.add(lambdaInfo)
                        
                        // 记录 Lambda 创建指令
                        instructionSequence.add(InstructionInfo.LambdaCreation(lambdaInfo))
                    }
                }
            }
        }
        
        super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, *bootstrapMethodArguments)
    }
    
    private fun isLambdaBootstrap(handle: Handle): Boolean {
        return handle.owner == "java/lang/invoke/LambdaMetafactory" &&
                (handle.name == "metafactory" || handle.name == "altMetafactory")
    }
    
    private fun extractLambdaType(descriptor: String): String {
        // 从方法描述符中提取函数式接口类型
        // 例如: ()Ljava/util/function/Consumer; -> java.util.function.Consumer
        val returnTypeStart = descriptor.lastIndexOf(')')
        if (returnTypeStart != -1 && returnTypeStart < descriptor.length - 1) {
            val returnType = descriptor.substring(returnTypeStart + 1)
            if (returnType.startsWith('L') && returnType.endsWith(';')) {
                return returnType.substring(1, returnType.length - 1).replace('/', '.')
            }
        }
        return "unknown"
    }
    
    private fun associateLambdasWithMethodCalls(): Map<String, Set<LambdaInfo>> {
        val associations = mutableMapOf<String, MutableSet<LambdaInfo>>()
        
        // 分析指令序列，将 Lambda 关联到相关的方法调用
        for (i in instructionSequence.indices) {
            val instruction = instructionSequence[i]
            
            if (instruction is InstructionInfo.LambdaCreation) {
                var associated = false
                
                // 首先查找这个 Lambda 之后的方法调用
                for (j in i + 1 until instructionSequence.size) {
                    val nextInstruction = instructionSequence[j]
                    if (nextInstruction is InstructionInfo.MethodCall) {
                        if (methodAcceptsFunctionalInterface(nextInstruction.methodCall, instruction.lambdaInfo)) {
                            associations.getOrPut(nextInstruction.methodKey) { mutableSetOf() }
                                .add(instruction.lambdaInfo)
                            associated = true
                            break
                        }
                    }
                }
                
                // 如果没有找到后续的方法调用，查找之前的方法调用
                if (!associated) {
                    for (j in i - 1 downTo 0) {
                        val prevInstruction = instructionSequence[j]
                        if (prevInstruction is InstructionInfo.MethodCall) {
                            if (methodAcceptsFunctionalInterface(prevInstruction.methodCall, instruction.lambdaInfo)) {
                                associations.getOrPut(prevInstruction.methodKey) { mutableSetOf() }
                                    .add(instruction.lambdaInfo)
                                break
                            }
                        }
                    }
                }
            }
        }
        
        return associations.mapValues { it.value.toSet() }
    }

    /**
     * 检查方法调用是否接受函数式接口类型的 Lambda 表达式,可能不准，对应到json里是associatedLambdas，但不影响
     */
    private fun methodAcceptsFunctionalInterface(methodCall: MethodCallInfo, lambdaInfo: LambdaInfo): Boolean {
        val descriptor = methodCall.methodDescriptor
        val lambdaTypeDescriptor = "L${lambdaInfo.lambdaType.replace('.', '/')};"
        
        // 检查是否直接匹配 lambda 类型
        if (descriptor.contains(lambdaTypeDescriptor)) {
            return true
        }
        
        // Kotlin 函数式接口模式
        val kotlinFunctionPatterns = listOf(
            // Kotlin 函数类型 (Function0, Function1, etc.)
            "Lkotlin/jvm/functions/Function",
            // Kotlin suspend 函数
            "Lkotlin/coroutines/SuspendFunction",
            // Kotlin 扩展函数
            "Lkotlin/ExtensionFunction"
        )
        
        // Java 函数式接口模式
        val javaFunctionPatterns = listOf(
            "Ljava/util/function/",
            "Ljava/lang/Runnable;",
            "Ljava/util/concurrent/Callable;",
            "Ljava/util/Comparator;",
            "Ljava/io/Serializable;"
        )
        
        // 检查 Kotlin 和 Java 函数式接口模式
        val matches = kotlinFunctionPatterns.any { descriptor.contains(it) } ||
                     javaFunctionPatterns.any { descriptor.contains(it) } ||
                     // 检查是否包含 Object 类型参数（可能是 lambda）
                     descriptor.contains("Ljava/lang/Object;")
        
        // 调试信息
        if (matches) {
            println("Lambda ${lambdaInfo.lambdaType} matches method ${methodCall.className}.${methodCall.methodName}${methodCall.methodDescriptor}")
        }
        
        return matches
    }
    
    override fun visitEnd() {
        // 分析指令序列，建立 Lambda 与方法调用的关联
        val lambdaAssociations = associateLambdasWithMethodCalls()
        
        // 将 Lambda 关联到对应的方法调用
        val updatedCalledMethods = calledMethods.mapValues { (methodKey, methodCall) ->
            val associatedLambdas = lambdaAssociations[methodKey] ?: emptySet()
            methodCall.copy(associatedLambdas = associatedLambdas)
        }.values.toSet()
        
        // 方法分析完成，记录结果
        // 记录所有方法，不管是否有属性访问、属性修改或方法调用
        methods.add(
            PropertyAccessInfo(
                className = className,
                methodName = methodName,
                methodDescriptor = methodDescriptor,
                accessedProperties = accessedProperties.toSet(),
                modifiedProperties = modifiedProperties.toSet(),
                calledMethods = updatedCalledMethods,
                lambdaExpressions = lambdaExpressions.toSet(),
                externalPropertyAccesses = externalPropertyAccesses.toSet()
            )
        )
        super.visitEnd()
    }
}