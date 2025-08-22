package org.morecup.pragmaddd.analyzer.writer

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class ResourceWriterTest {
    
    private lateinit var resourceWriter: ResourceWriterImpl
    
    @TempDir
    lateinit var tempDir: Path
    
    @BeforeEach
    fun setUp() {
        resourceWriter = ResourceWriterImpl()
    }
    
    @AfterEach
    fun tearDown() {
        // 清理临时文件
        tempDir.toFile().deleteRecursively()
    }
    
    @Test
    fun `should create META-INF directory structure`() {
        // Given
        val outputDirectory = tempDir.toString()
        
        // When
        val metaInfDir = resourceWriter.createMetaInfDirectory(outputDirectory)
        
        // Then
        assertTrue(metaInfDir.toFile().exists())
        assertTrue(metaInfDir.toFile().isDirectory)
        assertEquals("META-INF", metaInfDir.fileName.toString())
    }
    
    @Test
    fun `should write main sources JSON to META-INF directory`() {
        // Given
        val outputDirectory = tempDir.toString()
        val json = """{"test": "data"}"""
        val fileName = "test-main.json"
        
        // When
        resourceWriter.writeMainSourcesJson(json, outputDirectory, fileName)
        
        // Then
        val expectedPath = resourceWriter.getResourceFilePath(outputDirectory, "ddd-analysis/$fileName")
        val file = expectedPath.toFile()
        
        assertTrue(file.exists())
        assertEquals(json, file.readText())
    }
    
    @Test
    fun `should write test sources JSON to META-INF directory`() {
        // Given
        val outputDirectory = tempDir.toString()
        val json = """{"test": "data"}"""
        val fileName = "test-test.json"
        
        // When
        resourceWriter.writeTestSourcesJson(json, outputDirectory, fileName)
        
        // Then
        val expectedPath = resourceWriter.getResourceFilePath(outputDirectory, "ddd-analysis/$fileName")
        val file = expectedPath.toFile()
        
        assertTrue(file.exists())
        assertEquals(json, file.readText())
    }
    
    @Test
    fun `should write JSON to custom resource path`() {
        // Given
        val outputDirectory = tempDir.toString()
        val json = """{"custom": "data"}"""
        val resourcePath = "custom/path/data.json"
        
        // When
        resourceWriter.writeJsonToResource(json, resourcePath, outputDirectory)
        
        // Then
        val expectedPath = resourceWriter.getResourceFilePath(outputDirectory, resourcePath)
        val file = expectedPath.toFile()
        
        assertTrue(file.exists())
        assertEquals(json, file.readText())
    }
    
    @Test
    fun `should create nested directories automatically`() {
        // Given
        val outputDirectory = tempDir.toString()
        val json = """{"nested": "data"}"""
        val resourcePath = "deep/nested/structure/data.json"
        
        // When
        resourceWriter.writeJsonToResource(json, resourcePath, outputDirectory)
        
        // Then
        val expectedPath = resourceWriter.getResourceFilePath(outputDirectory, resourcePath)
        val file = expectedPath.toFile()
        
        assertTrue(file.exists())
        assertTrue(file.parentFile.exists())
        assertEquals(json, file.readText())
    }
    
    @Test
    fun `should validate output directory is writable`() {
        // Given
        val writableDirectory = tempDir.toString()
        
        // When & Then
        assertTrue(resourceWriter.isOutputDirectoryWritable(writableDirectory))
    }
    
    @Test
    fun `should create output directory if it does not exist`() {
        // Given
        val nonExistentDirectory = tempDir.resolve("new-directory").toString()
        
        // When
        val isWritable = resourceWriter.isOutputDirectoryWritable(nonExistentDirectory)
        
        // Then
        assertTrue(isWritable)
        assertTrue(File(nonExistentDirectory).exists())
    }
    
    @Test
    fun `should get correct resource file path`() {
        // Given
        val outputDirectory = tempDir.toString()
        val resourcePath = "test/path/file.json"
        
        // When
        val filePath = resourceWriter.getResourceFilePath(outputDirectory, resourcePath)
        
        // Then
        val expectedPath = File(outputDirectory).resolve("META-INF").resolve(resourcePath).toPath()
        assertEquals(expectedPath, filePath)
    }
    
    @Test
    fun `should use default file names for main and test sources`() {
        // Given
        val outputDirectory = tempDir.toString()
        val json = """{"default": "names"}"""
        
        // When
        resourceWriter.writeMainSourcesJson(json, outputDirectory)
        resourceWriter.writeTestSourcesJson(json, outputDirectory)
        
        // Then
        val mainFile = resourceWriter.getResourceFilePath(outputDirectory, "ddd-analysis/ddd-analysis-main.json").toFile()
        val testFile = resourceWriter.getResourceFilePath(outputDirectory, "ddd-analysis/ddd-analysis-test.json").toFile()
        
        assertTrue(mainFile.exists())
        assertTrue(testFile.exists())
        assertEquals(json, mainFile.readText())
        assertEquals(json, testFile.readText())
    }
    
    @Test
    fun `should cleanup old JSON files`() {
        // Given
        val outputDirectory = tempDir.toString()
        val json = """{"old": "data"}"""
        
        // 创建一些旧文件
        resourceWriter.writeMainSourcesJson(json, outputDirectory, "old-main.json")
        resourceWriter.writeTestSourcesJson(json, outputDirectory, "old-test.json")
        
        // When
        resourceWriter.cleanupOldJsonFiles(outputDirectory)
        
        // Then
        val jsonFiles = resourceWriter.listJsonFiles(outputDirectory)
        assertTrue(jsonFiles.isEmpty())
    }
    
    @Test
    fun `should list JSON files in META-INF directory`() {
        // Given
        val outputDirectory = tempDir.toString()
        val json = """{"list": "test"}"""
        
        resourceWriter.writeMainSourcesJson(json, outputDirectory, "file1.json")
        resourceWriter.writeTestSourcesJson(json, outputDirectory, "file2.json")
        
        // When
        val jsonFiles = resourceWriter.listJsonFiles(outputDirectory)
        
        // Then
        assertEquals(2, jsonFiles.size)
        assertTrue(jsonFiles.any { it.name == "file1.json" })
        assertTrue(jsonFiles.any { it.name == "file2.json" })
    }
    
    @Test
    fun `should verify JSON file was written successfully`() {
        // Given
        val outputDirectory = tempDir.toString()
        val json = """{"verify": "test"}"""
        val resourcePath = "ddd-analysis/verify-test.json"
        
        // When
        resourceWriter.writeJsonToResource(json, resourcePath, outputDirectory)
        
        // Then
        assertTrue(resourceWriter.verifyJsonFileWritten(outputDirectory, resourcePath))
    }
    
    @Test
    fun `should return false when verifying non-existent file`() {
        // Given
        val outputDirectory = tempDir.toString()
        val resourcePath = "ddd-analysis/non-existent.json"
        
        // When & Then
        assertFalse(resourceWriter.verifyJsonFileWritten(outputDirectory, resourcePath))
    }
    
    @Test
    fun `should handle empty JSON content`() {
        // Given
        val outputDirectory = tempDir.toString()
        val emptyJson = ""
        val fileName = "empty.json"
        
        // When
        resourceWriter.writeMainSourcesJson(emptyJson, outputDirectory, fileName)
        
        // Then
        val expectedPath = resourceWriter.getResourceFilePath(outputDirectory, "ddd-analysis/$fileName")
        val file = expectedPath.toFile()
        
        assertTrue(file.exists())
        assertEquals(emptyJson, file.readText())
    }
    
    @Test
    fun `should handle large JSON content`() {
        // Given
        val outputDirectory = tempDir.toString()
        val largeJson = """{"data": "${(1..1000).joinToString(",") { "item$it" }}"}"""
        val fileName = "large.json"
        
        // When
        resourceWriter.writeMainSourcesJson(largeJson, outputDirectory, fileName)
        
        // Then
        val expectedPath = resourceWriter.getResourceFilePath(outputDirectory, "ddd-analysis/$fileName")
        val file = expectedPath.toFile()
        
        assertTrue(file.exists())
        assertEquals(largeJson, file.readText())
        assertTrue(file.length() > 1000)
    }
}