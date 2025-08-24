package org.morecup.pragmaddd.analyzer

import org.objectweb.asm.*

/**
 * ASM 类访问器，用于检测 DDD 注解并分析方法
 */
class DomainObjectClassVisitor(
    private val dddAnnotatedClasses: Map<String, Set<String>> = emptyMap()
) : ClassVisitor(Opcodes.ASM9) {
    
    private var className: String = ""
    private var isAggregateRoot: Boolean = false
    private var isDomainEntity: Boolean = false
    private var isValueObject: Boolean = false
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
        when (descriptor) {
            "Lorg/morecup/pragmaddd/core/annotation/AggregateRoot;" -> isAggregateRoot = true
            "Lorg/morecup/pragmaddd/core/annotation/DomainEntity;" -> isDomainEntity = true
            "Lorg/morecup/pragmaddd/core/annotation/ValueObject;" -> isValueObject = true
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
        // 分析所有 DDD 注解的类的方法
        return if (hasDddAnnotation()) {
            PropertyAccessMethodVisitor(className, name, descriptor, methods, dddAnnotatedClasses)
        } else {
            super.visitMethod(access, name, descriptor, signature, exceptions)
        }
    }
    
    private fun hasDddAnnotation(): Boolean {
        return isAggregateRoot || isDomainEntity || isValueObject
    }
    
    fun getResult(): ClassAnalysisResult? {
        return if (hasDddAnnotation()) {
            val domainObjectType = when {
                isAggregateRoot -> DomainObjectType.AGGREGATE_ROOT
                isDomainEntity -> DomainObjectType.DOMAIN_ENTITY
                isValueObject -> DomainObjectType.VALUE_OBJECT
                else -> throw IllegalStateException("Should not reach here")
            }
            
            ClassAnalysisResult(
                className = className,
                domainObjectType = domainObjectType,
                methods = methods.toList()
            )
        } else {
            null
        }
    }
}