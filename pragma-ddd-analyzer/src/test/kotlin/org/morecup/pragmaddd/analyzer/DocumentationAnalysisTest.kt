package org.morecup.pragmaddd.analyzer

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DocumentationAnalysisTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `should analyze class with documentation`() {
        // 创建测试源文件
        val sourceDir = File(tempDir, "src")
        sourceDir.mkdirs()
        
        val testSourceFile = File(sourceDir, "TestEntity.kt")
        testSourceFile.writeText("""
            package com.example
            
            import org.morecup.pragmaddd.core.annotation.DomainEntity
            
            /**
             * 这是一个测试实体类
             * 用于演示文档分析功能
             */
            @DomainEntity
            class TestEntity {
                
                /**
                 * 实体的唯一标识符
                 */
                private val id: Long = 0
                
                /**
                 * 实体的名称
                 */
                var name: String = ""
                
                /**
                 * 获取实体信息
                 * @return 实体的字符串表示
                 */
                fun getInfo(): String {
                    return "TestEntity(id=${'$'}id, name=${'$'}name)"
                }
            }
        """.trimIndent())
        
        // 创建编译输出目录和类文件（模拟编译后的字节码）
        val classDir = File(tempDir, "classes")
        classDir.mkdirs()
        
        // 这里我们需要实际的字节码文件来测试，但为了简化测试，我们可以测试源文件映射功能
        val analyzer = DomainDocumentationAnalyzer()
        
        // 测试源文件映射构建
        val sourceFileMapMethod = analyzer.javaClass.getDeclaredMethod("buildSourceFileMap", File::class.java)
        sourceFileMapMethod.isAccessible = true
        val sourceFileMap = sourceFileMapMethod.invoke(analyzer, sourceDir) as Map<String, File>
        
        // 验证源文件映射
        assertTrue(sourceFileMap.containsKey("com.example.TestEntity"))
        assertEquals(testSourceFile, sourceFileMap["com.example.TestEntity"])
    }
    
    @Test
    fun `should merge documentation results correctly`() {
        val merger = DocumentationResultMerger()
        
        // 创建测试数据
        val mainResult = SourceSetDocumentationResult(
            sourceSet = "main",
            classes = listOf(
                ClassDocumentationInfo(
                    className = "com.example.MainEntity",
                    packageName = "com.example",
                    access = 1,
                    documentation = "Main entity documentation",
                    annotations = listOf(
                        AnnotationInfo("DomainEntity", "Lorg/morecup/pragmaddd/core/annotation/DomainEntity;")
                    ),
                    domainObjectType = DomainObjectType.DOMAIN_ENTITY,
                    sourceSet = "main"
                )
            )
        )
        
        val testResult = SourceSetDocumentationResult(
            sourceSet = "test",
            classes = listOf(
                ClassDocumentationInfo(
                    className = "com.example.TestEntity",
                    packageName = "com.example",
                    access = 1,
                    documentation = "Test entity documentation",
                    annotations = listOf(
                        AnnotationInfo("DomainEntity", "Lorg/morecup/pragmaddd/core/annotation/DomainEntity;")
                    ),
                    domainObjectType = DomainObjectType.DOMAIN_ENTITY,
                    sourceSet = "test"
                )
            )
        )
        
        // 创建临时文件
        val mainFile = File(tempDir, "main-result.json")
        val testFile = File(tempDir, "test-result.json")
        val outputFile = File(tempDir, "merged-result.json")
        
        val mapper = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper().apply {
            enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT)
        }
        
        // 写入测试数据
        mainFile.writeText(mapper.writeValueAsString(DocumentationAnalysisResult(main = mainResult)))
        testFile.writeText(mapper.writeValueAsString(DocumentationAnalysisResult(test = testResult)))
        
        // 执行合并
        merger.mergeResults(mainFile, testFile, outputFile)
        
        // 验证结果
        assertTrue(outputFile.exists())
        
        val mergedResult = mapper.readValue(outputFile, DocumentationAnalysisResult::class.java)
        assertNotNull(mergedResult.main)
        assertNotNull(mergedResult.test)
        assertEquals("main", mergedResult.main?.sourceSet)
        assertEquals("test", mergedResult.test?.sourceSet)
        assertEquals(1, mergedResult.main?.classes?.size)
        assertEquals(1, mergedResult.test?.classes?.size)
    }
    
    @Test
    fun `should extract class name from source file correctly`() {
        val sourceFile = File(tempDir, "TestClass.kt")
        sourceFile.writeText("""
            package com.example.domain
            
            import org.morecup.pragmaddd.core.annotation.AggregateRoot
            
            @AggregateRoot
            class TestClass {
                // class content
            }
        """.trimIndent())
        
        val analyzer = DomainDocumentationAnalyzer()
        val extractMethod = analyzer.javaClass.getDeclaredMethod("extractClassNameFromSourceFile", File::class.java, File::class.java)
        extractMethod.isAccessible = true
        
        val className = extractMethod.invoke(analyzer, sourceFile, tempDir) as String?
        
        assertEquals("com.example.domain.TestClass", className)
    }
}