package org.morecup.pragmaddd.analyzer.repository

import org.objectweb.asm.*
import org.objectweb.asm.tree.MethodNode

/**
 * 聚合根扫描器
 */
class AggregateRootScanner : ClassVisitor(Opcodes.ASM9) {
    private var className: String = ""
    private var isAggregateRoot = false
    private val fieldNames = mutableListOf<String>()
    
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
    
    override fun visitField(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        value: Any?
    ): FieldVisitor? {
        name?.let { fieldNames.add(it) }
        return super.visitField(access, name, descriptor, signature, value)
    }
    
    fun getResult(): AggregateRootInfo? {
        return if (isAggregateRoot) {
            AggregateRootInfo(className, fieldNames.toList())
        } else {
            null
        }
    }
}

/**
 * Repository扫描器
 */
class RepositoryScanner : ClassVisitor(Opcodes.ASM9) {
    private var className: String = ""
    private var repositoryType: RepositoryType? = null
    private var genericAggregateRootType: String? = null
    private var annotationTargetType: String? = null
    
    override fun visit(
        version: Int,
        access: Int,
        name: String,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        this.className = name.replace('/', '.')
        
        // 检查是否继承DomainRepository泛型接口
        interfaces?.forEach { interfaceName ->
            if (interfaceName.startsWith("org/morecup/pragmaddd/core/repository/DomainRepository")) {
                repositoryType = RepositoryType.GENERIC_INTERFACE
                // 从signature中提取泛型参数
                signature?.let { sig ->
                    extractGenericParameter(sig)?.let { genericType ->
                        genericAggregateRootType = genericType
                    }
                }
            }
        }
        
        // 命名约定检查
        if (repositoryType == null && isRepositoryByNaming(className)) {
            repositoryType = RepositoryType.NAMING_CONVENTION
        }
        
        super.visit(version, access, name, signature, superName, interfaces)
    }
    
    override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
        if (descriptor == "Lorg/morecup/pragmaddd/core/annotation/DomainRepository;") {
            repositoryType = RepositoryType.ANNOTATED
            return object : AnnotationVisitor(Opcodes.ASM9) {
                override fun visit(name: String?, value: Any?) {
                    if (name == "targetType" && value is Type) {
                        annotationTargetType = value.className
                    }
                    super.visit(name, value)
                }
            }
        }
        return super.visitAnnotation(descriptor, visible)
    }
    
    private fun isRepositoryByNaming(className: String): Boolean {
        val simpleName = className.substringAfterLast('.')
        return simpleName.endsWith("Repository") || 
               simpleName.endsWith("RepositoryImpl") ||
               simpleName.endsWith("Repo") ||
               (simpleName.startsWith("I") && simpleName.contains("Repository"))
    }
    
    private fun extractGenericParameter(signature: String): String? {
        // 解析泛型签名，提取第一个类型参数
        val regex = "L([^;<]+);".toRegex()
        val matchResult = regex.find(signature)
        return matchResult?.groupValues?.get(1)?.replace('/', '.')
    }
    
    fun getResult(): RepositoryInfo? {
        return repositoryType?.let { type ->
            RepositoryInfo(
                className = className,
                repositoryType = type,
                genericAggregateRootType = genericAggregateRootType,
                annotationTargetType = annotationTargetType
            )
        }
    }
}

/**
 * 方法调用访问器
 */
class MethodCallVisitor(
    private val currentClass: String,
    private val currentMethod: String,
    private val currentMethodDescriptor: String
) : MethodVisitor(Opcodes.ASM9) {
    
    val repositoryCalls = mutableListOf<MethodCallInfo>()
    
    override fun visitMethodInsn(
        opcode: Int,
        owner: String,
        name: String,
        descriptor: String,
        isInterface: Boolean
    ) {
        val ownerClassName = owner.replace('/', '.')
        
        // 检查是否是Repository方法调用（通过类名模式匹配）
        if (isRepositoryClass(ownerClassName)) {
            repositoryCalls.add(
                MethodCallInfo(
                    repositoryClass = ownerClassName,
                    methodName = name,
                    methodDescriptor = descriptor
                )
            )
        }
        
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
    }
    
    private fun isRepositoryClass(className: String): Boolean {
        val simpleName = className.substringAfterLast('.')
        return simpleName.endsWith("Repository") || 
               simpleName.endsWith("RepositoryImpl") ||
               simpleName.endsWith("Repo") ||
               (simpleName.startsWith("I") && simpleName.contains("Repository"))
    }
}

/**
 * 聚合根方法调用访问器
 */
class AggregateRootMethodCallVisitor(
    private val aggregateRootClassInternalName: String
) : MethodVisitor(Opcodes.ASM9) {
    
    val aggregateRootCalls = mutableListOf<MethodCallInfo>()
    
    override fun visitMethodInsn(
        opcode: Int,
        owner: String,
        name: String,
        descriptor: String,
        isInterface: Boolean
    ) {
        if (owner == aggregateRootClassInternalName) {
            aggregateRootCalls.add(
                MethodCallInfo(
                    repositoryClass = owner.replace('/', '.'),
                    methodName = name,
                    methodDescriptor = descriptor
                )
            )
        }
        
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
    }
}

/**
 * 字段访问访问器
 */
class FieldAccessVisitor(
    private val targetClassInternalName: String
) : MethodVisitor(Opcodes.ASM9) {
    
    val accessedFields = mutableSetOf<String>()
    
    override fun visitFieldInsn(opcode: Int, owner: String, name: String, descriptor: String) {
        if (owner == targetClassInternalName && (opcode == Opcodes.GETFIELD || opcode == Opcodes.PUTFIELD)) {
            accessedFields.add(name)
        }
        super.visitFieldInsn(opcode, owner, name, descriptor)
    }
}

/**
 * 方法字段访问分析器
 */
class MethodFieldAccessAnalyzer(
    private val targetMethodName: String,
    private val targetMethodDescriptor: String
) : ClassVisitor(Opcodes.ASM9) {
    
    private var currentClassName: String = ""
    private val accessedFields = mutableSetOf<String>()
    
    override fun visit(
        version: Int,
        access: Int,
        name: String,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        this.currentClassName = name
        super.visit(version, access, name, signature, superName, interfaces)
    }
    
    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor? {
        return if (name == targetMethodName && descriptor == targetMethodDescriptor) {
            object : MethodVisitor(Opcodes.ASM9) {
                override fun visitFieldInsn(opcode: Int, owner: String, fieldName: String, descriptor: String) {
                    if (owner == currentClassName && (opcode == Opcodes.GETFIELD || opcode == Opcodes.PUTFIELD)) {
                        accessedFields.add(fieldName)
                    }
                    super.visitFieldInsn(opcode, owner, fieldName, descriptor)
                }
            }
        } else {
            super.visitMethod(access, name, descriptor, signature, exceptions)
        }
    }
    
    fun getAccessedFields(): List<String> = accessedFields.toList().sorted()
}