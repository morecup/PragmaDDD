package org.morecup.pragmaddd.analyzer.serialization

import org.objectweb.asm.Type

/**
 * 通用注解序列化器
 *
 * 将复杂类型转换为统一的字符串格式：
 * - KClass: "class:org.example.ClassName"
 * - 枚举: "enum:org.example.EnumClass.VALUE"
 * - 数组: "array:[item1,item2,item3]"
 */
object UniversalAnnotationSerializer {

    /**
     * 序列化注解参数值
     */
    fun serialize(value: Any?): Any? = when (value) {
        null -> null
        is String, is Boolean, is Number -> value
        is Type -> "class:${value.className}"
        is Enum<*> -> "enum:${value.javaClass.name}.${value.name}"
        is Array<*> -> serializeArray(value.toList())
        is List<*> -> serializeArray(value)
        else -> value.toString()
    }

    /**
     * 序列化数组
     */
    private fun serializeArray(list: List<*>): String {
        val items = list.map { serialize(it)?.toString()?.replace(",", "\\,") ?: "null" }
        return "array:[${items.joinToString(",")}]"
    }

    /**
     * 批量序列化参数
     */
    fun serializeParameters(parameters: Map<String, Any>): Map<String, Any?> =
        parameters.mapValues { (_, value) -> serialize(value) }
}

/**
 * 通用注解反序列化器
 */
object UniversalAnnotationDeserializer {

    /**
     * 反序列化参数值
     */
    fun deserialize(value: Any?): Any? = when (value) {
        null -> null
        is Boolean, is Number -> value
        is String -> when {
            value.startsWith("class:") -> ClassInfo(value.removePrefix("class:"))
            value.startsWith("enum:") -> parseEnum(value)
            value.startsWith("array:") -> parseArray(value)
            else -> value
        }
        else -> value
    }

    /**
     * 解析枚举
     */
    private fun parseEnum(value: String): UniversalEnumInfo {
        val enumString = value.removePrefix("enum:")
        val lastDot = enumString.lastIndexOf('.')
        require(lastDot != -1) { "Invalid enum format: $value" }

        return UniversalEnumInfo(
            enumClass = enumString.substring(0, lastDot),
            enumValue = enumString.substring(lastDot + 1)
        )
    }

    /**
     * 解析数组
     */
    private fun parseArray(value: String): List<Any?> {
        val content = value.removePrefix("array:[").removeSuffix("]")
        if (content.isEmpty()) return emptyList()

        return content.split(",").map { item ->
            deserialize(item.replace("\\,", ","))
        }
    }


    /**
     * 批量反序列化参数
     */
    fun deserializeParameters(parameters: Map<String, Any?>): Map<String, Any?> =
        parameters.mapValues { (_, value) -> deserialize(value) }
}

/**
 * 类信息
 */
data class ClassInfo(val className: String) {
    val simpleName: String get() = className.substringAfterLast('.')
    val packageName: String get() = className.substringBeforeLast('.', "")
}

/**
 * 枚举信息
 */
data class UniversalEnumInfo(val enumClass: String, val enumValue: String) {
    val simpleClassName: String get() = enumClass.substringAfterLast('.')
    override fun toString(): String = "$simpleClassName.$enumValue"
}


