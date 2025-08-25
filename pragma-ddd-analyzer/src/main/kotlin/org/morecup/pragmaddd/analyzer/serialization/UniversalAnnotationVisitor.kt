package org.morecup.pragmaddd.analyzer.serialization

import org.morecup.pragmaddd.analyzer.model.AnnotationInfo
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.Opcodes.ASM9
import org.objectweb.asm.Type

/**
 * 通用注解访问器
 */
class UniversalAnnotationVisitor(
    private var annotationInfo: AnnotationInfo
) : AnnotationVisitor(ASM9) {

    private val parameters = mutableMapOf<String, Any?>()

    override fun visit(name: String?, value: Any?) {
        if (name != null) {
            parameters[name] = UniversalAnnotationSerializer.serialize(value)
        }
    }

    override fun visitEnum(name: String?, descriptor: String?, value: String?) {
        if (name != null && descriptor != null && value != null) {
            val enumClass = Type.getType(descriptor).className
            parameters[name] = "enum:$enumClass.$value"
        }
    }

    override fun visitArray(name: String?): AnnotationVisitor? {
        if (name != null) {
            val arrayValues = mutableListOf<Any?>()

            return object : AnnotationVisitor(ASM9) {
                override fun visit(name: String?, value: Any?) {
                    arrayValues.add(UniversalAnnotationSerializer.serialize(value))
                }

                override fun visitEnum(name: String?, descriptor: String?, value: String?) {
                    if (descriptor != null && value != null) {
                        val enumClass = Type.getType(descriptor).className
                        arrayValues.add("enum:$enumClass.$value")
                    }
                }

                override fun visitEnd() {
                    val items = arrayValues.map { it.toString().replace(",", "\\,") }
                    parameters[name] = "array:[${items.joinToString(",")}]"
                }
            }
        }
        return null
    }

    override fun visitEnd() {
        annotationInfo = annotationInfo.copy(parameters = parameters.toMap())
    }

    fun getUpdatedAnnotationInfo(): AnnotationInfo = annotationInfo
}

/**
 * 通用注解访问器工厂
 */
object UniversalAnnotationVisitorFactory {
    fun createVisitor(annotationInfo: AnnotationInfo): UniversalAnnotationVisitor =
        UniversalAnnotationVisitor(annotationInfo)
}


