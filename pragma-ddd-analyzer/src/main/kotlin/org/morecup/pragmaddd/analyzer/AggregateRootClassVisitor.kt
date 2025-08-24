package org.morecup.pragmaddd.analyzer

import org.objectweb.asm.*

/**
 * ASM 类访问器，用于检测 @AggregateRoot 注解并分析方法
 */
class AggregateRootClassVisitor(
    private val dddAnnotatedClasses: Map<String, Set<String>> = emptyMap()
) : ClassVisitor(Opcodes.ASM9) {
    
    private var className: String = ""
    private var isAggregateRoot: Boolean = false
    private val methods = mutableListOf<PropertyAccessInfo>()
    
    override fun visit(
        version: Int,
        access: Int,
        name: String,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        this.className = name.replace('/', '.')
        super.visit(version, access, name, signature, superName, interfaces)
    }
    
    override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
        if (descriptor == "Lorg/morecup/pragmaddd/core/annotation/AggregateRoot;") {
            isAggregateRoot = true
        }
        return super.visitAnnotation(descriptor, visible)
    }
    
    override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor? {
        // 只分析被 @AggregateRoot 注解的类的方法
        return if (isAggregateRoot) {
            PropertyAccessMethodVisitor(className, name, descriptor, methods, dddAnnotatedClasses)
        } else {
            super.visitMethod(access, name, descriptor, signature, exceptions)
        }
    }
    
    fun getResult(): ClassAnalysisResult? {
        return if (isAggregateRoot) {
            ClassAnalysisResult(className, isAggregateRoot, methods.toList())
        } else {
            null
        }
    }
}