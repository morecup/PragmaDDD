package org.morecup.pragmaddd.analyzer

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.morecup.pragmaddd.analyzer.serialization.UniversalAnnotationDeserializer
import org.morecup.pragmaddd.analyzer.serialization.ClassInfo
import org.morecup.pragmaddd.analyzer.serialization.UniversalEnumInfo
import org.assertj.core.api.Assertions.assertThat

/**
 * 反序列化使用示例测试
 */
class DeserializationExampleTest {

    @Test
    @DisplayName("完整的反序列化使用示例")
    fun `complete deserialization example`() {
        // 模拟从 JSON 分析结果中获取的序列化参数
        val serializedParameters = mapOf(
            "message" to "This is a test annotation with parameters",
            "level" to "enum:kotlin.DeprecationLevel.WARNING",
            "targetClasses" to "array:[class:java.lang.String,class:java.lang.Integer]",
            "columnName" to "totalAmount",
            "enabled" to true,
            "count" to 42
        )

        println("=== 原始序列化参数 ===")
        serializedParameters.forEach { (key, value) ->
            println("$key: $value (${value::class.simpleName})")
        }

        // 反序列化所有参数
        val deserializedParams = UniversalAnnotationDeserializer.deserializeParameters(serializedParameters)

        println("\n=== 反序列化后的参数 ===")
        deserializedParams.forEach { (key, value) ->
            when (value) {
                is ClassInfo -> {
                    println("$key: 类型信息")
                    println("  - 完整类名: ${value.className}")
                    println("  - 简单类名: ${value.simpleName}")
                    println("  - 包名: ${value.packageName}")
                }
                is UniversalEnumInfo -> {
                    println("$key: 枚举信息")
                    println("  - 枚举类: ${value.enumClass}")
                    println("  - 枚举值: ${value.enumValue}")
                    println("  - 简单类名: ${value.simpleClassName}")
                    println("  - 字符串表示: ${value}")
                }
                is List<*> -> {
                    println("$key: 数组信息 (${value.size} 个元素)")
                    value.forEachIndexed { index, item ->
                        when (item) {
                            is ClassInfo -> println("  [$index] 类: ${item.className}")
                            else -> println("  [$index] 值: $item")
                        }
                    }
                }
                else -> {
                    println("$key: $value (${value?.let { it::class.simpleName } ?: "null"})")
                }
            }
        }

        // 验证反序列化结果
        assertThat(deserializedParams["message"]).isEqualTo("This is a test annotation with parameters")
        assertThat(deserializedParams["enabled"]).isEqualTo(true)
        assertThat(deserializedParams["count"]).isEqualTo(42)

        val levelEnum = deserializedParams["level"] as UniversalEnumInfo
        assertThat(levelEnum.enumClass).isEqualTo("kotlin.DeprecationLevel")
        assertThat(levelEnum.enumValue).isEqualTo("WARNING")
        assertThat(levelEnum.simpleClassName).isEqualTo("DeprecationLevel")

        val targetClasses = deserializedParams["targetClasses"] as List<*>
        assertThat(targetClasses).hasSize(2)
        
        val stringClass = targetClasses[0] as ClassInfo
        assertThat(stringClass.className).isEqualTo("java.lang.String")
        assertThat(stringClass.simpleName).isEqualTo("String")
        assertThat(stringClass.packageName).isEqualTo("java.lang")

        val integerClass = targetClasses[1] as ClassInfo
        assertThat(integerClass.className).isEqualTo("java.lang.Integer")
        assertThat(integerClass.simpleName).isEqualTo("Integer")
        assertThat(integerClass.packageName).isEqualTo("java.lang")
    }

    @Test
    @DisplayName("处理真实的 demo 项目分析结果")
    fun `process real demo analysis result`() {
        // 模拟真实的分析结果片段
        val realAnnotationParams = mapOf(
            "message" to "This is a test annotation with parameters",
            "level" to "enum:kotlin.DeprecationLevel.WARNING"
        )

        val ormFieldParams = mapOf(
            "columnName" to "totalAmount"
        )

        val ormObjectParams = mapOf(
            "objectNameList" to "array:[order]"
        )

        println("=== 处理 @Deprecated 注解 ===")
        processDeprecatedAnnotation(realAnnotationParams)

        println("\n=== 处理 @OrmField 注解 ===")
        processOrmFieldAnnotation(ormFieldParams)

        println("\n=== 处理 @OrmObject 注解 ===")
        processOrmObjectAnnotation(ormObjectParams)
    }

    private fun processDeprecatedAnnotation(params: Map<String, Any?>) {
        val deserializedParams = UniversalAnnotationDeserializer.deserializeParameters(params)
        
        val message = deserializedParams["message"] as? String
        val level = deserializedParams["level"] as? UniversalEnumInfo

        println("弃用消息: $message")
        if (level != null) {
            println("弃用级别: ${level.enumValue}")
            println("来自枚举类: ${level.simpleClassName}")
            
            // 验证
            assertThat(level.enumClass).isEqualTo("kotlin.DeprecationLevel")
            assertThat(level.enumValue).isEqualTo("WARNING")
        }
    }

    private fun processOrmFieldAnnotation(params: Map<String, Any?>) {
        val deserializedParams = UniversalAnnotationDeserializer.deserializeParameters(params)
        
        val columnName = deserializedParams["columnName"] as? String
        println("数据库列名: $columnName")
        
        // 验证
        assertThat(columnName).isEqualTo("totalAmount")
    }

    private fun processOrmObjectAnnotation(params: Map<String, Any?>) {
        val deserializedParams = UniversalAnnotationDeserializer.deserializeParameters(params)
        
        val objectNameList = deserializedParams["objectNameList"] as? List<*>
        if (objectNameList != null) {
            println("对象名称列表:")
            objectNameList.forEach { name ->
                println("  - $name")
            }
            
            // 验证
            assertThat(objectNameList).hasSize(1)
            assertThat(objectNameList[0]).isEqualTo("order")
        }
    }

    @Test
    @DisplayName("错误处理示例")
    fun `error handling example`() {
        // 测试各种边界情况
        val testCases = mapOf(
            "null_value" to null,
            "empty_string" to "",
            "normal_string" to "hello",
            "invalid_enum" to "enum:InvalidFormat",
            "empty_array" to "array:[]",
            "malformed_class" to "class:",
            "number" to 123,
            "boolean" to true
        )

        println("=== 错误处理测试 ===")
        testCases.forEach { (key, value) ->
            try {
                val result = UniversalAnnotationDeserializer.deserialize(value)
                println("$key: $value -> $result (${result?.let { it::class.simpleName } ?: "null"})")
            } catch (e: Exception) {
                println("$key: $value -> 错误: ${e.message}")
            }
        }
    }
}
