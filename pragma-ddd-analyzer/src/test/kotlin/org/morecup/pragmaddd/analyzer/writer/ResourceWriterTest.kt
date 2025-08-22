package org.morecup.pragmaddd.analyzer.writer

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ResourceWriterTest {
    
    @TempDir
    lateinit var tempDir: File
    
    private lateinit var resourceWriter: ResourceWriter
    
    @BeforeEach
    fun setUp() {
        resourceWriter = ResourceWriterImpl()
    }
    
    @Test
    fun `writeMainSourcesJson should create file in correct location`() {
        // Given
        val json = """{"test": "data"}"""
        val outputDirectory = tempDir.toString()
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
    fun `writeJsonToResource should create file with custom resource path`() {
        // Given
        val json = """{"custom": "data"}"""
        val outputDirectory = tempDir.toString()
        val resourcePath = "custom/path/file.json"
        
        // When
        resourceWriter.writeJsonToResource(json, resourcePath, outputDirectory)
        
        // Then
        val expectedPath = resourceWriter.getResourceFilePath(outputDirectory, resourcePath)
        val file = expectedPath.toFile()
        
        assertTrue(file.exists())
        assertEquals(json, file.readText())
    }
    
    @Test
    fun `createMetaInfDirectory should create META-INF directory`() {
        // Given
        val outputDirectory = tempDir.toString()
        
        // When
        val metaInfPath = resourceWriter.createMetaInfDirectory(outputDirectory)
        
        // Then
        val metaInfDir = metaInfPath.toFile()
        assertTrue(metaInfDir.exists())
        assertTrue(metaInfDir.isDirectory)
        assertEquals("META-INF", metaInfDir.name)
    }
    
    @Test
    fun `isOutputDirectoryWritable should return true for writable directory`() {
        // Given
        val outputDirectory = tempDir.toString()
        
        // When
        val isWritable = resourceWriter.isOutputDirectoryWritable(outputDirectory)
        
        // Then
        assertTrue(isWritable)
    }
    
    @Test
    fun `getResourceFilePath should return correct path`() {
        // Given
        val outputDirectory = tempDir.toString()
        val resourcePath = "test/file.json"
        
        // When
        val filePath = resourceWriter.getResourceFilePath(outputDirectory, resourcePath)
        
        // Then
        val expectedPath = File(tempDir, "META-INF/test/file.json").toPath()
        assertEquals(expectedPath, filePath)
    }
    
    @Test
    fun `writeMainSourcesJson should create parent directories`() {
        // Given
        val json = """{"nested": "data"}"""
        val outputDirectory = tempDir.toString()
        val fileName = "nested-main.json"
        
        // When
        resourceWriter.writeMainSourcesJson(json, outputDirectory, fileName)
        
        // Then
        val expectedPath = resourceWriter.getResourceFilePath(outputDirectory, "ddd-analysis/$fileName")
        val file = expectedPath.toFile()
        
        assertTrue(file.exists())
        assertTrue(file.parentFile.exists())
        assertEquals(json, file.readText())
    }
    
    @Test
    fun `multiple writeMainSourcesJson calls should work correctly`() {
        // Given
        val json1 = """{"file1": "data"}"""
        val json2 = """{"file2": "data"}"""
        val outputDirectory = tempDir.toString()
        
        // When
        resourceWriter.writeMainSourcesJson(json1, outputDirectory, "file1.json")
        resourceWriter.writeMainSourcesJson(json2, outputDirectory, "file2.json")
        
        // Then
        val file1 = resourceWriter.getResourceFilePath(outputDirectory, "ddd-analysis/file1.json").toFile()
        val file2 = resourceWriter.getResourceFilePath(outputDirectory, "ddd-analysis/file2.json").toFile()
        
        assertTrue(file1.exists())
        assertTrue(file2.exists())
        assertEquals(json1, file1.readText())
        assertEquals(json2, file2.readText())
    }
    
    @Test
    fun `writeJsonToResource should handle nested paths`() {
        // Given
        val json = """{"deep": "nested"}"""
        val outputDirectory = tempDir.toString()
        val resourcePath = "deep/nested/path/file.json"
        
        // When
        resourceWriter.writeJsonToResource(json, resourcePath, outputDirectory)
        
        // Then
        val expectedPath = resourceWriter.getResourceFilePath(outputDirectory, resourcePath)
        val file = expectedPath.toFile()
        
        assertTrue(file.exists())
        assertEquals(json, file.readText())
    }
    
    @Test
    fun `isOutputDirectoryWritable should create directory if it doesn't exist`() {
        // Given
        val nonExistentDir = File(tempDir, "new-directory").toString()
        
        // When
        val isWritable = resourceWriter.isOutputDirectoryWritable(nonExistentDir)
        
        // Then
        assertTrue(isWritable)
        assertTrue(File(nonExistentDir).exists())
    }
    
    @Test
    fun `writeMainSourcesJson should overwrite existing files`() {
        // Given
        val json1 = """{"version": 1}"""
        val json2 = """{"version": 2}"""
        val outputDirectory = tempDir.toString()
        val fileName = "overwrite-test.json"
        
        // When
        resourceWriter.writeMainSourcesJson(json1, outputDirectory, fileName)
        resourceWriter.writeMainSourcesJson(json2, outputDirectory, fileName)
        
        // Then
        val expectedPath = resourceWriter.getResourceFilePath(outputDirectory, "ddd-analysis/$fileName")
        val file = expectedPath.toFile()
        
        assertTrue(file.exists())
        assertEquals(json2, file.readText()) // Should contain the second version
    }
}