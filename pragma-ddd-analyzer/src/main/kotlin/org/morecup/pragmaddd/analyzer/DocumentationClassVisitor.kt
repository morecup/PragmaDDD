package org.morecup.pragmaddd.analyzer

import org.objectweb.asm.*
import java.io.File

/**
 * 用于收集类文档信息的ASM访问器
 */
class DocumentationClassVisitor(
    private val sourceSet: String,
    private val sourceFiles: Map<String, File> = emptyMap(), // 类名到源文件的映射
    private val documentationParser: SourceDocumentationParser = SourceDocumentationParser()
) : ClassVisitor(Opcodes.ASM9) {
    
    private var className: String = ""
    private var packageName: String = ""
    private var access: Int = 0
    private var signature: String? = null
    private var superName: String? = null
    private var interfaces: List<String> = emptyList()
    private var classDocumentation: String? = null
    private val classAnnotations = mutableListOf<AnnotationInfo>()
    private val properties = mutableListOf<PropertyInfo>()
    private val methods = mutableListOf<MethodInfo>()
    
    private var isAggregateRoot: Boolean = false
    private var isDomainEntity: Boolean = false
    private var isValueObject: Boolean = false
    
    // 缓存解析的源代码文档信息
    private var sourceDocInfo: SourceDocumentationInfo? = null
    
    override fun visit(
        version: Int,
        access: Int,
        name: String,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        this.className = name.replace('/', '.')
        this.packageName = if (className.contains('.')) {
            className.substringBeforeLast('.')
        } else {
            ""
        }
        this.access = access
        this.signature = signature
        this.superName = superName?.replace('/', '.')
        this.interfaces = interfaces?.map { it.replace('/', '.') } ?: emptyList()
        
        // 解析源文件文档信息
        parseSourceDocumentation()
        
        super.visit(version, access, name, signature, superName, interfaces)
    }
    
    override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
        val annotationName = when (descriptor) {
            "Lorg/morecup/pragmaddd/core/annotation/AggregateRoot;" -> {
                isAggregateRoot = true
                "AggregateRoot"
            }
            "Lorg/morecup/pragmaddd/core/annotation/DomainEntity;" -> {
                isDomainEntity = true
                "DomainEntity"
            }
            "Lorg/morecup/pragmaddd/core/annotation/ValueObject;" -> {
                isValueObject = true
                "ValueObject"
            }
            else -> {
                // 提取其他注解名称
                descriptor.removeSurrounding("L", ";").substringAfterLast('/').substringAfterLast('$')
            }
        }
        
        return AnnotationParameterVisitor(annotationName, descriptor) { annotationInfo ->
            classAnnotations.add(annotationInfo)
        }
    }
    
    override fun visitField(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        value: Any?
    ): FieldVisitor? {
        val fieldAnnotations = mutableListOf<AnnotationInfo>()
        val fieldDocumentation = sourceDocInfo?.fieldDocumentations?.get(name)
        
        val propertyInfo = PropertyInfo(
            name = name,
            type = Type.getType(descriptor).className,
            descriptor = descriptor,
            access = access,
            signature = signature,
            value = value,
            documentation = fieldDocumentation,
            annotations = fieldAnnotations
        )
        
        properties.add(propertyInfo)
        
        return object : FieldVisitor(Opcodes.ASM9) {
            override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
                val annotationName = descriptor.removeSurrounding("L", ";").substringAfterLast('/').substringAfterLast('$')
                return AnnotationParameterVisitor(annotationName, descriptor) { annotationInfo ->
                    fieldAnnotations.add(annotationInfo)
                }
            }
        }
    }
    
    override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor? {
        val methodAnnotations = mutableListOf<AnnotationInfo>()
        val methodDocumentation = sourceDocInfo?.methodDocumentations?.get(name)
        
        val methodInfo = MethodInfo(
            name = name,
            descriptor = descriptor,
            access = access,
            signature = signature,
            exceptions = exceptions?.map { it.replace('/', '.') } ?: emptyList(),
            documentation = methodDocumentation,
            annotations = methodAnnotations
        )
        
        methods.add(methodInfo)
        
        return object : MethodVisitor(Opcodes.ASM9) {
            override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
                val annotationName = descriptor.removeSurrounding("L", ";").substringAfterLast('/').substringAfterLast('$')
                return AnnotationParameterVisitor(annotationName, descriptor) { annotationInfo ->
                    methodAnnotations.add(annotationInfo)
                }
            }
        }
    }
    
    private fun hasDddAnnotation(): Boolean {
        return isAggregateRoot || isDomainEntity || isValueObject
    }
    
    private fun getDomainObjectType(): DomainObjectType {
        return when {
            isAggregateRoot -> DomainObjectType.AGGREGATE_ROOT
            isDomainEntity -> DomainObjectType.DOMAIN_ENTITY
            isValueObject -> DomainObjectType.VALUE_OBJECT
            else -> throw IllegalStateException("No DDD annotation found")
        }
    }
    
    fun getResult(): ClassDocumentationInfo? {
        return if (hasDddAnnotation()) {
            ClassDocumentationInfo(
                className = className,
                packageName = packageName,
                access = access,
                signature = signature,
                superName = superName,
                interfaces = interfaces,
                documentation = sourceDocInfo?.classDocumentation,
                annotations = classAnnotations.toList(),
                properties = properties.toList(),
                methods = methods.toList(),
                domainObjectType = getDomainObjectType(),
                sourceSet = sourceSet
            )
        } else {
            null
        }
    }
    
    /**
     * 解析源文件文档信息
     */
    private fun parseSourceDocumentation() {
        val sourceFile = sourceFiles[className]
        if (sourceFile != null) {
            sourceDocInfo = documentationParser.parseSourceFile(sourceFile, className)
        }
    }
}

/**
 * 注解参数访问器
 */
private class AnnotationParameterVisitor(
    private val annotationName: String,
    private val descriptor: String,
    private val onComplete: (AnnotationInfo) -> Unit
) : AnnotationVisitor(Opcodes.ASM9) {
    
    private val parameters = mutableMapOf<String, Any>()
    
    override fun visit(name: String?, value: Any?) {
        if (name != null && value != null) {
            parameters[name] = value
        }
    }
    
    override fun visitEnum(name: String?, descriptor: String?, value: String?) {
        if (name != null && value != null) {
            parameters[name] = value
        }
    }
    
    override fun visitArray(name: String?): AnnotationVisitor? {
        val arrayValues = mutableListOf<Any>()
        return object : AnnotationVisitor(Opcodes.ASM9) {
            override fun visit(name: String?, value: Any?) {
                if (value != null) {
                    arrayValues.add(value)
                }
            }
            
            override fun visitEnd() {
                if (name != null) {
                    parameters[name] = arrayValues.toList()
                }
            }
        }
    }
    
    override fun visitEnd() {
        val annotationInfo = AnnotationInfo(
            name = annotationName,
            descriptor = descriptor,
            parameters = parameters.toMap()
        )
        onComplete(annotationInfo)
    }
}