package org.morecup.pragmaddd.analyzer

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class JavaParserTest {

    @TempDir
    lateinit var tempDir: File

    private val parser = SourceDocumentationParser()

    @Test
    fun `should parse Java file with JavaParser`() {
        // 创建测试 Java 文件
        val javaFile = File(tempDir, "TestEntity.java")
        javaFile.writeText("""
            package com.example;
            
            /**
             * This is a test entity class
             * Used to demonstrate JavaParser functionality
             */
            public class TestEntity {
                
                /**
                 * The unique identifier
                 */
                private Long id;
                
                /**
                 * Gets the ID
                 * @return the ID value
                 */
                public Long getId() {
                    return id;
                }
            }
        """.trimIndent())
        
        // 解析文件
        val result = parser.parseSourceFile(javaFile, "com.example.TestEntity")
        
        // 打印结果用于调试
        println("Java parsing result:")
        println("Class: ${result?.className}")
        println("Class doc: ${result?.classDocumentation}")
        println("Field docs: ${result?.fieldDocumentations}")
        println("Method docs: ${result?.methodDocumentations}")
        
        // 基本验证
        assert(result != null)
        assert(result?.className == "com.example.TestEntity")
    }
    
    @Test
    fun `should parse Kotlin file with regex parser`() {
        // 创建测试 Kotlin 文件
        val kotlinFile = File(tempDir, "TestEntity.kt")
        kotlinFile.writeText("""
            package com.example
            
            /**
             * 这是一个测试实体类
             * 用于演示 Kotlin 解析功能
             */
            class TestEntity(
                /**
                 * 唯一标识符
                 */
                private val id: Long
            ) {
                
                /**
                 * 获取ID
                 * @return ID值
                 */
                fun getId(): Long {
                    return id
                }
            }
        """.trimIndent())
        
        // 解析文件
        val result = parser.parseSourceFile(kotlinFile, "com.example.TestEntity")
        
        // 打印结果用于调试
        println("Kotlin parsing result:")
        println("Class: ${result?.className}")
        println("Class doc: ${result?.classDocumentation}")
        println("Field docs: ${result?.fieldDocumentations}")
        println("Method docs: ${result?.methodDocumentations}")
        
        // 基本验证
        assert(result != null)
        assert(result?.className == "com.example.TestEntity")
    }
}