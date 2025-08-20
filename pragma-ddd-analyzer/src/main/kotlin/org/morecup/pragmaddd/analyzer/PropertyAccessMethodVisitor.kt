package org.morecup.pragmaddd.analyzer

import org.objectweb.asm.*

/**
 * 方法访问器，用于分析方法内的属性访问
 */
class PropertyAccessMethodVisitor(
    private val className: String,
    private val methodName: String,
    private val methodDescriptor: String,
    private val methods: MutableList<PropertyAccessInfo>
) : MethodVisitor(Opcodes.ASM9) {
    
    private val accessedProperties = mutableSetOf<String>()
    private val modifiedProperties = mutableSetOf<String>()
    
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
        // 检测 getter/setter 方法调用
        if (owner.replace('/', '.') == className) {
            when {
                name.startsWith("get") && name.length > 3 -> {
                    // getter 方法
                    val propertyName = name.substring(3).replaceFirstChar { it.lowercase() }
                    accessedProperties.add(propertyName)
                }
                name.startsWith("is") && name.length > 2 -> {
                    // boolean getter 方法
                    val propertyName = name.substring(2).replaceFirstChar { it.lowercase() }
                    accessedProperties.add(propertyName)
                }
                name.startsWith("set") && name.length > 3 -> {
                    // setter 方法
                    val propertyName = name.substring(3).replaceFirstChar { it.lowercase() }
                    modifiedProperties.add(propertyName)
                }
            }
        }
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
    }
    
    override fun visitEnd() {
        // 方法分析完成，记录结果
        if (accessedProperties.isNotEmpty() || modifiedProperties.isNotEmpty()) {
            methods.add(
                PropertyAccessInfo(
                    className = className,
                    methodName = methodName,
                    methodDescriptor = methodDescriptor,
                    accessedProperties = accessedProperties.toSet(),
                    modifiedProperties = modifiedProperties.toSet()
                )
            )
        }
        super.visitEnd()
    }
}