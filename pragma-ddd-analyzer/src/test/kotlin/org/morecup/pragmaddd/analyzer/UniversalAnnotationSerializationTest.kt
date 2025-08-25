package org.morecup.pragmaddd.analyzer

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.assertj.core.api.Assertions.assertThat
import org.morecup.pragmaddd.analyzer.model.AnnotationInfo
import org.morecup.pragmaddd.analyzer.serialization.*
import org.objectweb.asm.Type

/**
 * 通用注解序列化测试
 * 
 * 测试新的通用序列化方案，使用统一的字符串格式处理所有复杂类型
 */
@DisplayName("通用注解序列化测试")
class UniversalAnnotationSerializationTest {
    
    private val objectMapper = jacksonObjectMapper()
    
    @Nested
    @DisplayName("通用序列化器测试")
    inner class UniversalSerializerTest {
        
        @Test
        @DisplayName("应该正确序列化 KClass 类型")
        fun `should serialize KClass types correctly`() {
            val stringType = Type.getType(String::class.java)
            val intType = Type.getType(Int::class.java)
            
            val serializedString = UniversalAnnotationSerializer.serialize(stringType)
            val serializedInt = UniversalAnnotationSerializer.serialize(intType)
            
            assertThat(serializedString).isEqualTo("class:java.lang.String")
            assertThat(serializedInt).isEqualTo("class:int")
        }
        
        @Test
        @DisplayName("应该正确序列化枚举类型")
        fun `should serialize enum types correctly`() {
            // 模拟枚举序列化（实际中枚举会通过 visitEnum 处理）
            val enumString = "enum:javax.persistence.FetchType.LAZY"
            
            // 验证格式正确
            assertThat(enumString).startsWith("enum:")
            assertThat(enumString).contains("FetchType")
            assertThat(enumString).endsWith("LAZY")
        }
        
        @Test
        @DisplayName("应该正确序列化数组类型")
        fun `should serialize array types correctly`() {
            val stringType = Type.getType(String::class.java)
            val intType = Type.getType(Int::class.java)
            val array = arrayOf(stringType, intType)
            
            val serialized = UniversalAnnotationSerializer.serialize(array)
            
            assertThat(serialized).isInstanceOf(String::class.java)
            val serializedString = serialized as String
            assertThat(serializedString).startsWith("array:")
            assertThat(serializedString).contains("class:java.lang.String")
            assertThat(serializedString).contains("class:int")
        }
        
        @Test
        @DisplayName("应该正确序列化基本类型")
        fun `should serialize basic types correctly`() {
            assertThat(UniversalAnnotationSerializer.serialize("test")).isEqualTo("test")
            assertThat(UniversalAnnotationSerializer.serialize(123)).isEqualTo(123)
            assertThat(UniversalAnnotationSerializer.serialize(true)).isEqualTo(true)
            assertThat(UniversalAnnotationSerializer.serialize(null)).isNull()
        }
    }
    
    @Nested
    @DisplayName("通用反序列化器测试")
    inner class UniversalDeserializerTest {
        
        @Test
        @DisplayName("应该正确反序列化类类型")
        fun `should deserialize class types correctly`() {
            val classString = "class:java.lang.String"
            val deserialized = UniversalAnnotationDeserializer.deserialize(classString)
            
            assertThat(deserialized).isInstanceOf(ClassInfo::class.java)
            val classInfo = deserialized as ClassInfo
            assertThat(classInfo.className).isEqualTo("java.lang.String")
            assertThat(classInfo.simpleName).isEqualTo("String")
            assertThat(classInfo.packageName).isEqualTo("java.lang")
        }
        
        @Test
        @DisplayName("应该正确反序列化枚举类型")
        fun `should deserialize enum types correctly`() {
            val enumString = "enum:javax.persistence.FetchType.LAZY"
            val deserialized = UniversalAnnotationDeserializer.deserialize(enumString)
            
            assertThat(deserialized).isInstanceOf(UniversalEnumInfo::class.java)
            val enumInfo = deserialized as UniversalEnumInfo
            assertThat(enumInfo.enumClass).isEqualTo("javax.persistence.FetchType")
            assertThat(enumInfo.enumValue).isEqualTo("LAZY")
            assertThat(enumInfo.simpleClassName).isEqualTo("FetchType")
            assertThat(enumInfo.toString()).isEqualTo("FetchType.LAZY")
        }
        
        @Test
        @DisplayName("应该正确反序列化数组类型")
        fun `should deserialize array types correctly`() {
            val arrayString = "array:[class:java.lang.String,class:int]"
            val deserialized = UniversalAnnotationDeserializer.deserialize(arrayString)
            
            assertThat(deserialized).isInstanceOf(List::class.java)
            val list = deserialized as List<*>
            assertThat(list).hasSize(2)
            
            val firstItem = list[0] as ClassInfo
            assertThat(firstItem.className).isEqualTo("java.lang.String")
            
            val secondItem = list[1] as ClassInfo
            assertThat(secondItem.className).isEqualTo("int")
        }
        
        @Test
        @DisplayName("应该正确处理基本类型")
        fun `should handle basic types correctly`() {
            assertThat(UniversalAnnotationDeserializer.deserialize("test")).isEqualTo("test")
            assertThat(UniversalAnnotationDeserializer.deserialize(123)).isEqualTo(123)
            assertThat(UniversalAnnotationDeserializer.deserialize(true)).isEqualTo(true)
            assertThat(UniversalAnnotationDeserializer.deserialize(null)).isNull()
        }
    }
    
    @Nested
    @DisplayName("完整序列化流程测试")
    inner class CompleteSerializationFlowTest {
        
        @Test
        @DisplayName("应该正确处理 PolyListOrmFields 注解")
        fun `should handle PolyListOrmFields annotation correctly`() {
            // 模拟 ASM 解析后的参数
            val beijingAddressType = Type.getType("Lorg/example/BeijingAddress;")
            val hubeiAddressType = Type.getType("Lorg/example/HubeiAddress;")
            val notNullChoiceType = Type.getType("Lorg/example/NotNullColumnChoice;")
            
            val originalParameters = mapOf(
                "baseListName" to "goods:addressEntity",
                "baseColumnChoiceNames" to arrayOf("base:beijingAddress", "base:hubeiAddress"),
                "baseColumnChoiceTypes" to arrayOf(beijingAddressType, hubeiAddressType),
                "columnChoiceRule" to notNullChoiceType
            )
            
            // 1. 序列化参数
            val serializedParameters = UniversalAnnotationSerializer.serializeParameters(originalParameters)
            
            // 2. 创建注解信息
            val annotationInfo = AnnotationInfo(
                name = "PolyListOrmFields",
                descriptor = "Lorg/morecup/pragmaddd/core/annotation/PolyListOrmFields;",
                visible = true,
                parameters = serializedParameters
            )
            
            // 3. JSON 序列化（现在应该成功）
            val json = objectMapper.writeValueAsString(annotationInfo)
            assertThat(json).isNotNull()
            assertThat(json).contains("class:org.example.BeijingAddress")
            assertThat(json).contains("class:org.example.HubeiAddress")
            assertThat(json).contains("class:org.example.NotNullColumnChoice")
            
            // 4. JSON 反序列化
            val deserializedAnnotation = objectMapper.readValue(json, AnnotationInfo::class.java)
            assertThat(deserializedAnnotation.name).isEqualTo("PolyListOrmFields")
            
            // 5. 验证序列化后的参数格式
            assertThat(deserializedAnnotation.parameters["baseListName"]).isEqualTo("goods:addressEntity")
            assertThat(deserializedAnnotation.parameters["baseColumnChoiceTypes"]).isInstanceOf(String::class.java)
            assertThat(deserializedAnnotation.parameters["columnChoiceRule"]).isEqualTo("class:org.example.NotNullColumnChoice")
            
            val baseColumnChoiceTypes = deserializedAnnotation.parameters["baseColumnChoiceTypes"] as String
            assertThat(baseColumnChoiceTypes).startsWith("array:")
            assertThat(baseColumnChoiceTypes).contains("BeijingAddress")
            assertThat(baseColumnChoiceTypes).contains("HubeiAddress")
        }
        
        @Test
        @DisplayName("应该正确处理包含枚举的 JPA 注解")
        fun `should handle JPA annotations with enums correctly`() {
            // 创建包含枚举的注解参数（已序列化格式）
            val serializedParameters = mapOf(
                "fetch" to "enum:javax.persistence.FetchType.LAZY",
                "cascade" to "array:[enum:javax.persistence.CascadeType.PERSIST,enum:javax.persistence.CascadeType.MERGE]",
                "mappedBy" to "parent",
                "orphanRemoval" to true
            )
            
            val annotationInfo = AnnotationInfo(
                name = "OneToMany",
                descriptor = "Ljavax/persistence/OneToMany;",
                visible = true,
                parameters = serializedParameters
            )
            
            // JSON 序列化和反序列化
            val json = objectMapper.writeValueAsString(annotationInfo)
            val deserializedAnnotation = objectMapper.readValue(json, AnnotationInfo::class.java)
            
            // 验证序列化结果
            assertThat(deserializedAnnotation.parameters["fetch"]).isEqualTo("enum:javax.persistence.FetchType.LAZY")
            assertThat(deserializedAnnotation.parameters["cascade"]).isInstanceOf(String::class.java)
            assertThat(deserializedAnnotation.parameters["mappedBy"]).isEqualTo("parent")
            assertThat(deserializedAnnotation.parameters["orphanRemoval"]).isEqualTo(true)
            
            val cascadeArray = deserializedAnnotation.parameters["cascade"] as String
            assertThat(cascadeArray).startsWith("array:")
            assertThat(cascadeArray).contains("PERSIST")
            assertThat(cascadeArray).contains("MERGE")
        }
        
        @Test
        @DisplayName("应该正确处理参数反序列化")
        fun `should handle parameter deserialization correctly`() {
            val serializedParameters = mapOf(
                "simpleString" to "test",
                "classType" to "class:java.lang.String",
                "enumType" to "enum:javax.persistence.FetchType.LAZY",
                "arrayType" to "array:[class:java.lang.String,class:int]",
                "numberValue" to 123,
                "booleanValue" to true
            )
            
            // 反序列化参数
            val deserializedParameters = UniversalAnnotationDeserializer.deserializeParameters(serializedParameters)
            
            // 验证反序列化结果
            assertThat(deserializedParameters["simpleString"]).isEqualTo("test")
            assertThat(deserializedParameters["classType"]).isInstanceOf(ClassInfo::class.java)
            assertThat(deserializedParameters["enumType"]).isInstanceOf(UniversalEnumInfo::class.java)
            assertThat(deserializedParameters["arrayType"]).isInstanceOf(List::class.java)
            assertThat(deserializedParameters["numberValue"]).isEqualTo(123)
            assertThat(deserializedParameters["booleanValue"]).isEqualTo(true)
            
            val classInfo = deserializedParameters["classType"] as ClassInfo
            assertThat(classInfo.className).isEqualTo("java.lang.String")
            
            val enumInfo = deserializedParameters["enumType"] as UniversalEnumInfo
            assertThat(enumInfo.enumClass).isEqualTo("javax.persistence.FetchType")
            assertThat(enumInfo.enumValue).isEqualTo("LAZY")
            
            val arrayList = deserializedParameters["arrayType"] as List<*>
            assertThat(arrayList).hasSize(2)
        }
    }
    
    @Nested
    @DisplayName("工具类测试")
    inner class UtilityTest {
        
        @Test
        @DisplayName("应该正确处理各种类型")
        fun `should handle various types correctly`() {
            // 测试序列化
            assertThat(UniversalAnnotationSerializer.serialize(Type.getType(String::class.java))).isEqualTo("class:java.lang.String")
            assertThat(UniversalAnnotationSerializer.serialize("simple string")).isEqualTo("simple string")
            assertThat(UniversalAnnotationSerializer.serialize(123)).isEqualTo(123)
            assertThat(UniversalAnnotationSerializer.serialize(null)).isNull()

            // 测试反序列化
            assertThat(UniversalAnnotationDeserializer.deserialize("class:java.lang.String")).isInstanceOf(ClassInfo::class.java)
            assertThat(UniversalAnnotationDeserializer.deserialize("simple string")).isEqualTo("simple string")
            assertThat(UniversalAnnotationDeserializer.deserialize(123)).isEqualTo(123)
            assertThat(UniversalAnnotationDeserializer.deserialize(null)).isNull()
        }
    }
}
