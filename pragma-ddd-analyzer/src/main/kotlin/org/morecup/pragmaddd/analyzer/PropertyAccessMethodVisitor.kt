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
    private val methods: MutableList<PropertyAccessInfo>
) : MethodVisitor(Opcodes.ASM9) {
    
    private val accessedProperties = mutableSetOf<String>()
    private val modifiedProperties = mutableSetOf<String>()
    private val calledMethods = mutableMapOf<String, MethodCallInfo>()
    
    override fun visitFieldInsn(opcode: Int, owner: String, name: String, descriptor: String) {
        // 只关注当前类的字段访问
        if (owner.replace('/', '.') == className) {
            when (opcode) {
                Opcodes.GETFIELD -> {
                    // 读取字段
                    accessedProperties.add(name)
                }
                Opcodes.PUTFIELD -> {
                    // 写入字段
                    modifiedProperties.add(name)
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

        // 只关注当前类的方法调用
        if (ownerClassName == className) {
                // 记录方法调用
                val methodKey = "$name$descriptor"
                val existingCall = calledMethods[methodKey]
                if (existingCall != null) {
                    // 如果已经记录过这个方法调用，增加调用次数
                    calledMethods[methodKey] = existingCall.copy(callCount = existingCall.callCount + 1)
                } else {
                    // 新的方法调用
                    calledMethods[methodKey] = MethodCallInfo(name, descriptor, 1)
                }
        }
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
    }
    
    override fun visitEnd() {
        // 方法分析完成，记录结果
        // 只要有属性访问、属性修改或方法调用，就记录这个方法
        if (accessedProperties.isNotEmpty() || modifiedProperties.isNotEmpty() || calledMethods.isNotEmpty()) {
            methods.add(
                PropertyAccessInfo(
                    className = className,
                    methodName = methodName,
                    methodDescriptor = methodDescriptor,
                    accessedProperties = accessedProperties.toSet(),
                    modifiedProperties = modifiedProperties.toSet(),
                    calledMethods = calledMethods.values.toSet()
                )
            )
        }
        super.visitEnd()
    }
}