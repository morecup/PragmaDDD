package org.morecup.pragmaddd.analyzer

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SourceDocumentationParserTest {

    @TempDir
    lateinit var tempDir: File

    private val parser = SourceDocumentationParser()

    @Test
    fun `should parse Java file with Javadoc`() {
        // 创建测试 Java 文件
        val javaFile = File(tempDir, "TestEntity.java")
        javaFile.writeText("""
            package com.example;
            
            import org.morecup.pragmaddd.core.annotation.DomainEntity;
            
            /**
             * This is a test entity class
             * Used to demonstrate documentation analysis
             */
            @DomainEntity
            public class TestEntity {
                
                /**
                 * The unique identifier of the entity
                 */
                private Long id;
                
                /**
                 * The name of the entity
                 */
                private String name;
                
                /**
                 * Gets the entity information
                 * @return string representation of the entity
                 */
                public String getInfo() {
                    return "TestEntity(id=" + id + ", name=" + name + ")";
                }
            }
        """.trimIndent())
        
        // 解析文件
        val result = parser.parseSourceFile(javaFile, "com.example.TestEntity")
        
        // 验证结果
        assertNotNull(result)
        assertEquals("com.example.TestEntity", result.className)
        assertNotNull(result.classDocumentation)
        assertEquals("This is a test entity class\nUsed to demonstrate documentation analysis", result.classDocumentation?.trim())
        
        // 验证字段文档
        assertEquals("The unique identifier of the entity", result.fieldDocumentations["id"]?.trim())
        assertEquals("The name of the entity", result.fieldDocumentations["name"]?.trim())
        
        // 验证方法文档
        assertNotNull(result.methodDocumentations["getInfo"])
    }
    
    @Test
    fun `should parse Kotlin file with KDoc`() {
        // 创建测试 Kotlin 文件
        val kotlinFile = File(tempDir, "TestEntity.kt")
        kotlinFile.writeText("""
            package com.example
            
            import org.morecup.pragmaddd.core.annotation.DomainEntity
            
            /**
             * 这是一个测试实体类
             * 用于演示文档分析功能
             */
            @DomainEntity
            class TestEntity(
                /**
                 * 实体的唯一标识符
                 */
                private val id: Long,
                
                /**
                 * 实体的名称
                 */
                private val name: String
            ) {
                
                /**
                 * 获取实体信息
                 * @return 实体的字符串表示
                 */
                fun getInfo(): String {
                    return "TestEntity(id=${'$'}id, name=${'$'}name)"
                }
            }
        """.trimIndent())
        
        // 解析文件
        val result = parser.parseSourceFile(kotlinFile, "com.example.TestEntity")
        
        // 验证结果
        assertNotNull(result)
        assertEquals("com.example.TestEntity", result.className)
        assertNotNull(result.classDocumentation)
        assertEquals("这是一个测试实体类\n用于演示文档分析功能", result.classDocumentation?.trim())
        
        // 验证字段文档
        assertEquals("实体的唯一标识符", result.fieldDocumentations["id"]?.trim())
        assertEquals("实体的名称", result.fieldDocumentations["name"]?.trim())
        
        // 验证方法文档
        assertNotNull(result.methodDocumentations["getInfo"])
    }
    
    @Test
    fun `should return null for non-existent file`() {
        val nonExistentFile = File(tempDir, "NonExistent.kt")
        val result = parser.parseSourceFile(nonExistentFile, "com.example.NonExistent")
        
        assertNull(result)
    }
    
    @Test
    fun `should return null for unsupported file extension`() {
        val txtFile = File(tempDir, "test.txt")
        txtFile.writeText("some content")
        
        val result = parser.parseSourceFile(txtFile, "com.example.Test")
        
        assertNull(result)
    }
}