package org.morecup.pragmaddd.analyzer

import org.morecup.pragmaddd.analyzer.model.*
import org.objectweb.asm.*
import org.objectweb.asm.Opcodes.*

/**
 * 增强的 ASM 类访问器，用于收集 DDD 注解类的完整结构信息
 */
class DetailedDomainObjectClassVisitor : ClassVisitor(ASM9) {

    private var className: String = ""
    private var simpleName: String = ""
    private var packageName: String = ""
    private var modifiers: ModifierInfo = ModifierInfo(0)
    private var superClass: String? = null
    private var interfaces: MutableList<String> = mutableListOf()
    private var signature: String? = null
    private var sourceFile: String? = null
    private val annotations: MutableList<AnnotationInfo> = mutableListOf()
    private val fields: MutableList<DetailedFieldInfo> = mutableListOf()
    private val methods: MutableList<DetailedMethodInfo> = mutableListOf()

    private var domainObjectType: org.morecup.pragmaddd.analyzer.model.DomainObjectType? = null

    // 用于存储Kotlin属性注解的临时映射
    private val kotlinPropertyAnnotations: MutableMap<String, MutableList<AnnotationInfo>> = mutableMapOf()
    
    override fun visit(
        version: Int,
        access: Int,
        name: String,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        this.className = name.replace('/', '.')
        this.simpleName = name.substringAfterLast('/')
        this.packageName = this.className.substringBeforeLast('.', "")
        this.modifiers = createModifierInfo(access)
        this.signature = signature
        this.superClass = superName?.replace('/', '.')
        this.interfaces.addAll(interfaces?.map { it.replace('/', '.') } ?: emptyList())
        
        super.visit(version, access, name, signature, superName, interfaces)
    }
    
    override fun visitSource(source: String?, debug: String?) {
        this.sourceFile = source
        super.visitSource(source, debug)
    }
    
    override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
        // 过滤不需要的注解
        if (!shouldIncludeAnnotation(descriptor)) {
            return null
        }

        val annotationInfo = AnnotationInfo(
            name = extractAnnotationName(descriptor),
            descriptor = descriptor,
            visible = visible
        )

        // 检查是否是 DDD 注解
        when (descriptor) {
            "Lorg/morecup/pragmaddd/core/annotation/AggregateRoot;" ->
                domainObjectType = org.morecup.pragmaddd.analyzer.model.DomainObjectType.AGGREGATE_ROOT
            "Lorg/morecup/pragmaddd/core/annotation/DomainEntity;" ->
                domainObjectType = org.morecup.pragmaddd.analyzer.model.DomainObjectType.DOMAIN_ENTITY
            "Lorg/morecup/pragmaddd/core/annotation/ValueObject;" ->
                domainObjectType = org.morecup.pragmaddd.analyzer.model.DomainObjectType.VALUE_OBJECT
        }

        val annotationVisitor = DetailedAnnotationVisitor(annotationInfo)

        return object : AnnotationVisitor(ASM9) {
            override fun visit(name: String?, value: Any?) {
                annotationVisitor.visit(name, value)
            }

            override fun visitEnum(name: String?, descriptor: String?, value: String?) {
                annotationVisitor.visitEnum(name, descriptor, value)
            }

            override fun visitArray(name: String?): AnnotationVisitor? {
                return annotationVisitor.visitArray(name)
            }

            override fun visitEnd() {
                // 先调用内部visitor的visitEnd来更新注解信息
                annotationVisitor.visitEnd()
                // 然后获取更新后的注解信息并添加到列表中
                val updatedAnnotationInfo = annotationVisitor.getUpdatedAnnotationInfo()
                annotations.add(updatedAnnotationInfo)
                super.visitEnd()
            }
        }
    }
    
    override fun visitField(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        value: Any?
    ): FieldVisitor? {
        val fieldInfo = DetailedFieldInfo(
            name = name,
            descriptor = descriptor,
            signature = signature,
            value = value,
            modifiers = createModifierInfo(access),
            annotations = mutableListOf()
        )
        fields.add(fieldInfo)
        
        return DetailedFieldVisitor(fieldInfo)
    }
    
    override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor? {
        // 检查是否是Kotlin属性注解的合成方法
        if (isKotlinPropertyAnnotationMethod(name, access)) {
            val propertyName = extractPropertyNameFromAnnotationMethod(name)
            if (propertyName != null) {
                // 创建一个特殊的访问器来收集属性注解
                return KotlinPropertyAnnotationMethodVisitor(propertyName, kotlinPropertyAnnotations)
            }
        }

        val methodInfo = DetailedMethodInfo(
            name = name,
            descriptor = descriptor,
            signature = signature,
            modifiers = createModifierInfo(access),
            exceptions = exceptions?.map { it.replace('/', '.') } ?: emptyList(),
            annotations = mutableListOf(),
            returnType = extractReturnType(descriptor)
        )

        // 过滤掉合成方法，避免在最终JSON中显示
        if (!isSyntheticMethodToFilter(name, access)) {
            methods.add(methodInfo)
        }

        return DetailedMethodVisitor(methodInfo)
    }
    
    /**
     * 获取分析结果
     */
    fun getResult(): DetailedClassInfo? {
        return if (domainObjectType != null) {
            // 在返回结果前，将Kotlin属性注解合并到对应的字段上
            val mergedFields = mergeKotlinPropertyAnnotations()

            DetailedClassInfo(
                className = className,
                simpleName = simpleName,
                packageName = packageName,
                modifiers = modifiers,
                superClass = superClass,
                interfaces = interfaces,
                signature = signature,
                sourceFile = sourceFile,
                annotations = annotations,
                fields = mergedFields,
                methods = methods,
                domainObjectType = domainObjectType
            )
        } else {
            null
        }
    }
    
    /**
     * 创建修饰符信息
     */
    private fun createModifierInfo(access: Int): ModifierInfo {
        return ModifierInfo(
            access = access,
            isPublic = (access and ACC_PUBLIC) != 0,
            isPrivate = (access and ACC_PRIVATE) != 0,
            isProtected = (access and ACC_PROTECTED) != 0,
            isStatic = (access and ACC_STATIC) != 0,
            isFinal = (access and ACC_FINAL) != 0,
            isAbstract = (access and ACC_ABSTRACT) != 0,
            isSynthetic = (access and ACC_SYNTHETIC) != 0,
            isInterface = (access and ACC_INTERFACE) != 0,
            isEnum = (access and ACC_ENUM) != 0,
            isAnnotation = (access and ACC_ANNOTATION) != 0
        )
    }
    
    /**
     * 提取注解名称
     */
    private fun extractAnnotationName(descriptor: String): String {
        return descriptor.removePrefix("L").removeSuffix(";").substringAfterLast('/')
    }
    
    /**
     * 从方法描述符中提取返回类型
     */
    private fun extractReturnType(descriptor: String): String? {
        val returnTypeDescriptor = descriptor.substringAfter(')')
        return if (returnTypeDescriptor == "V") "void" else returnTypeDescriptor
    }

    /**
     * 判断是否应该包含该注解
     */
    private fun shouldIncludeAnnotation(descriptor: String): Boolean {
        // 移除前缀和后缀，获取完整的类名
        val className = descriptor.removePrefix("L").removeSuffix(";").replace('/', '.')

        return when {
            // 保留：DDD相关注解
            className.contains("pragmaddd") -> true

            // 保留：常见的业务验证注解
            className.endsWith("NotNull") -> true
            className.endsWith("Nullable") -> true
            className.endsWith("Valid") -> true
            className.endsWith("JvmField") -> true
            className.endsWith("JvmStatic") -> true
            className.endsWith("JvmOverloads") -> true
            className.endsWith("Deprecated") -> true
            className.endsWith("Override") -> true
            className.endsWith("SuppressWarnings") -> true

            // 保留：测试相关注解
            className.contains("junit") -> true
            className.contains("Test") -> true
            className.endsWith("DisplayName") -> true
            className.endsWith("BeforeEach") -> true
            className.endsWith("AfterEach") -> true

            // 过滤：Kotlin编译器注解
            className.startsWith("kotlin.Metadata") -> false
            className.startsWith("kotlin.jvm.internal") -> false
            className.startsWith("kotlin.coroutines.jvm.internal") -> false

            // 过滤：JVM内部注解
            className.startsWith("java.lang.invoke") -> false
            className.startsWith("jdk.internal") -> false

            // 过滤：合成注解
            className.contains("synthetic") -> false
            className.contains("Synthetic") -> false

            // 默认保留其他注解
            else -> true
        }
    }

    /**
     * 检查是否是Kotlin属性注解的合成方法
     */
    private fun isKotlinPropertyAnnotationMethod(methodName: String, access: Int): Boolean {
        return methodName.endsWith("\$annotations") && (access and ACC_SYNTHETIC) != 0
    }

    /**
     * 从注解方法名中提取属性名
     */
    private fun extractPropertyNameFromAnnotationMethod(methodName: String): String? {
        return if (methodName.endsWith("\$annotations")) {
            val baseName = methodName.removeSuffix("\$annotations")
            // 处理getter方法名，如 getTotalAmount -> totalAmount
            when {
                baseName.startsWith("get") && baseName.length > 3 -> {
                    val propertyName = baseName.removePrefix("get")
                    propertyName.replaceFirstChar { it.lowercase() }
                }
                baseName.startsWith("is") && baseName.length > 2 -> {
                    val propertyName = baseName.removePrefix("is")
                    propertyName.replaceFirstChar { it.lowercase() }
                }
                else -> baseName
            }
        } else {
            null
        }
    }

    /**
     * 检查是否是需要过滤的合成方法
     */
    private fun isSyntheticMethodToFilter(methodName: String, access: Int): Boolean {
        return (access and ACC_SYNTHETIC) != 0 && methodName.endsWith("\$annotations")
    }

    /**
     * 将Kotlin属性注解合并到对应的字段上
     */
    private fun mergeKotlinPropertyAnnotations(): List<DetailedFieldInfo> {
        return fields.map { field ->
            val propertyAnnotations = kotlinPropertyAnnotations[field.name] ?: emptyList()
            if (propertyAnnotations.isNotEmpty()) {
                // 合并原有注解和属性注解
                val mergedAnnotations = (field.annotations + propertyAnnotations).toMutableList()
                field.copy(annotations = mergedAnnotations)
            } else {
                field
            }
        }
    }
}

/**
 * 详细注解访问器
 */
class DetailedAnnotationVisitor(
    private var annotationInfo: AnnotationInfo
) : AnnotationVisitor(ASM9) {

    private val parameters = mutableMapOf<String, Any>()

    override fun visit(name: String?, value: Any?) {
        if (name != null && value != null) {
            parameters[name] = value
        }
        super.visit(name, value)
    }

    override fun visitEnum(name: String?, descriptor: String?, value: String?) {
        if (name != null && value != null) {
            // 枚举值处理
            parameters[name] = value
        }
        super.visitEnum(name, descriptor, value)
    }

    override fun visitArray(name: String?): AnnotationVisitor? {
        if (name != null) {
            val arrayValues = mutableListOf<Any>()
            parameters[name] = arrayValues

            return object : AnnotationVisitor(ASM9) {
                override fun visit(name: String?, value: Any?) {
                    if (value != null) {
                        arrayValues.add(value)
                    }
                }

                override fun visitEnum(name: String?, descriptor: String?, value: String?) {
                    if (value != null) {
                        arrayValues.add(value)
                    }
                }
            }
        }
        return super.visitArray(name)
    }

    override fun visitEnd() {
        // 创建新的注解信息，包含收集到的参数
        annotationInfo = annotationInfo.copy(parameters = parameters.toMap())
        super.visitEnd()
    }

    /**
     * 获取更新后的注解信息
     */
    fun getUpdatedAnnotationInfo(): AnnotationInfo {
        return annotationInfo
    }
}

/**
 * 详细字段访问器
 */
class DetailedFieldVisitor(
    private val fieldInfo: DetailedFieldInfo
) : FieldVisitor(ASM9) {

    override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
        // 过滤不需要的注解
        if (!shouldIncludeAnnotation(descriptor)) {
            return null
        }

        val annotationInfo = AnnotationInfo(
            name = descriptor.removePrefix("L").removeSuffix(";").substringAfterLast('/'),
            descriptor = descriptor,
            visible = visible
        )

        val annotationVisitor = DetailedAnnotationVisitor(annotationInfo)

        return object : AnnotationVisitor(ASM9) {
            override fun visit(name: String?, value: Any?) {
                annotationVisitor.visit(name, value)
            }

            override fun visitEnum(name: String?, descriptor: String?, value: String?) {
                annotationVisitor.visitEnum(name, descriptor, value)
            }

            override fun visitArray(name: String?): AnnotationVisitor? {
                return annotationVisitor.visitArray(name)
            }

            override fun visitEnd() {
                // 先调用内部visitor的visitEnd来更新注解信息
                annotationVisitor.visitEnd()
                // 然后获取更新后的注解信息并添加到字段注解列表
                val updatedAnnotationInfo = annotationVisitor.getUpdatedAnnotationInfo()
                (fieldInfo.annotations as? MutableList)?.add(updatedAnnotationInfo)
                super.visitEnd()
            }
        }
    }

    /**
     * 判断是否应该包含该注解（复用主类的逻辑）
     */
    private fun shouldIncludeAnnotation(descriptor: String): Boolean {
        val className = descriptor.removePrefix("L").removeSuffix(";").replace('/', '.')

        return when {
            className.contains("pragmaddd") -> true
            className.endsWith("NotNull") -> true
            className.endsWith("Nullable") -> true
            className.endsWith("Valid") -> true
            className.endsWith("JvmField") -> true
            className.endsWith("JvmStatic") -> true
            className.startsWith("kotlin.Metadata") -> false
            className.startsWith("kotlin.jvm.internal") -> false
            className.contains("synthetic") -> false
            else -> true
        }
    }
}

/**
 * Kotlin属性注解方法访问器
 * 专门用于处理Kotlin属性注解的合成方法
 */
class KotlinPropertyAnnotationMethodVisitor(
    private val propertyName: String,
    private val kotlinPropertyAnnotations: MutableMap<String, MutableList<AnnotationInfo>>
) : MethodVisitor(ASM9) {

    override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
        // 过滤不需要的注解
        if (!shouldIncludeAnnotation(descriptor)) {
            return null
        }

        val annotationInfo = AnnotationInfo(
            name = descriptor.removePrefix("L").removeSuffix(";").substringAfterLast('/'),
            descriptor = descriptor,
            visible = visible
        )

        val annotationVisitor = DetailedAnnotationVisitor(annotationInfo)

        return object : AnnotationVisitor(ASM9) {
            override fun visit(name: String?, value: Any?) {
                annotationVisitor.visit(name, value)
            }

            override fun visitEnum(name: String?, descriptor: String?, value: String?) {
                annotationVisitor.visitEnum(name, descriptor, value)
            }

            override fun visitArray(name: String?): AnnotationVisitor? {
                return annotationVisitor.visitArray(name)
            }

            override fun visitEnd() {
                // 先调用内部visitor的visitEnd来更新注解信息
                annotationVisitor.visitEnd()
                // 然后获取更新后的注解信息并添加到属性注解映射中
                val updatedAnnotationInfo = annotationVisitor.getUpdatedAnnotationInfo()
                kotlinPropertyAnnotations.getOrPut(propertyName) { mutableListOf() }.add(updatedAnnotationInfo)
                super.visitEnd()
            }
        }
    }

    /**
     * 判断是否应该包含该注解（复用主类的逻辑）
     */
    private fun shouldIncludeAnnotation(descriptor: String): Boolean {
        val className = descriptor.removePrefix("L").removeSuffix(";").replace('/', '.')

        return when {
            className.contains("pragmaddd") -> true
            className.endsWith("NotNull") -> true
            className.endsWith("Nullable") -> true
            className.endsWith("Valid") -> true
            className.endsWith("JvmField") -> true
            className.endsWith("JvmStatic") -> true
            className.endsWith("Deprecated") -> true
            className.endsWith("Override") -> true
            className.contains("junit") -> true
            className.contains("Test") -> true
            className.startsWith("kotlin.Metadata") -> false
            className.startsWith("kotlin.jvm.internal") -> false
            className.contains("synthetic") -> false
            else -> true
        }
    }
}

/**
 * 详细方法访问器
 */
class DetailedMethodVisitor(
    private val methodInfo: DetailedMethodInfo
) : MethodVisitor(ASM9) {

    override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
        // 过滤不需要的注解
        if (!shouldIncludeAnnotation(descriptor)) {
            return null
        }

        val annotationInfo = AnnotationInfo(
            name = descriptor.removePrefix("L").removeSuffix(";").substringAfterLast('/'),
            descriptor = descriptor,
            visible = visible
        )

        val annotationVisitor = DetailedAnnotationVisitor(annotationInfo)

        return object : AnnotationVisitor(ASM9) {
            override fun visit(name: String?, value: Any?) {
                annotationVisitor.visit(name, value)
            }

            override fun visitEnum(name: String?, descriptor: String?, value: String?) {
                annotationVisitor.visitEnum(name, descriptor, value)
            }

            override fun visitArray(name: String?): AnnotationVisitor? {
                return annotationVisitor.visitArray(name)
            }

            override fun visitEnd() {
                // 先调用内部visitor的visitEnd来更新注解信息
                annotationVisitor.visitEnd()
                // 然后获取更新后的注解信息并添加到方法注解列表
                val updatedAnnotationInfo = annotationVisitor.getUpdatedAnnotationInfo()
                (methodInfo.annotations as? MutableList)?.add(updatedAnnotationInfo)
                super.visitEnd()
            }
        }
    }

    override fun visitParameterAnnotation(
        parameter: Int,
        descriptor: String,
        visible: Boolean
    ): AnnotationVisitor? {
        // 处理参数注解
        return super.visitParameterAnnotation(parameter, descriptor, visible)
    }

    /**
     * 判断是否应该包含该注解（复用主类的逻辑）
     */
    private fun shouldIncludeAnnotation(descriptor: String): Boolean {
        val className = descriptor.removePrefix("L").removeSuffix(";").replace('/', '.')

        return when {
            className.contains("pragmaddd") -> true
            className.endsWith("NotNull") -> true
            className.endsWith("Nullable") -> true
            className.endsWith("Valid") -> true
            className.endsWith("JvmField") -> true
            className.endsWith("JvmStatic") -> true
            className.endsWith("Deprecated") -> true
            className.endsWith("Override") -> true
            className.contains("junit") -> true
            className.contains("Test") -> true
            className.startsWith("kotlin.Metadata") -> false
            className.startsWith("kotlin.jvm.internal") -> false
            className.contains("synthetic") -> false
            else -> true
        }
    }
}
