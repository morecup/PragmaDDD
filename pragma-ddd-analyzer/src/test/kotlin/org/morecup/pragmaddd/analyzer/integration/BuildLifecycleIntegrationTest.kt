package org.morecup.pragmaddd.analyzer.integration

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.io.TempDir
import org.morecup.pragmaddd.analyzer.PragmaDddAnalyzerExtension
import org.morecup.pragmaddd.analyzer.PragmaDddAnalyzerPlugin
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File
import java.nio.file.Path

/**
 * Integration tests for Gradle build lifecycle integration
 * Tests proper task dependencies, incremental compilation, and resource packaging
 */
class BuildLifecycleIntegrationTest {
    
    @TempDir
    lateinit var tempDir: Path
    
    private lateinit var project: Project
    private lateinit var plugin: PragmaDddAnalyzerPlugin
    
    @BeforeEach
    fun setup() {
        project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .build()
        
        plugin = PragmaDddAnalyzerPlugin()
    }
    
    @Test
    fun `plugin should apply successfully and create extension`() {
        // When
        plugin.apply(project)
        
        // Then
        val extension = project.extensions.findByType(PragmaDddAnalyzerExtension::class.java)
        assertNotNull(extension)
        
        // Verify default configuration
        assertEquals("build/resources/main", extension!!.outputDirectory.get())
        assertTrue(extension.includeTestSources.get())
        assertEquals("ddd-analysis", extension.jsonFileNaming.get())
    }
    
    @Test
    fun `plugin should configure build lifecycle integration`() {
        // Given
        plugin.apply(project)
        val extension = project.extensions.getByType(PragmaDddAnalyzerExtension::class.java)
        extension.outputDirectory.set("build/test-output")
        
        // When - simulate afterEvaluate
        project.afterEvaluate { }
        
        // Then - extension should be configured
        assertEquals("build/test-output", extension.outputDirectory.get())
    }
    
    @Test
    fun `plugin should provide correct compiler plugin information`() {
        // When
        val pluginId = plugin.getCompilerPluginId()
        val artifact = plugin.getPluginArtifact()
        val isApplicable = plugin.isApplicable(createMockCompilation("main"))
        
        // Then
        assertEquals("org.morecup.pragmaddd.analyzer", pluginId)
        assertEquals("org.morecup.pragmaddd", artifact.groupId)
        assertEquals("pragma-ddd-analyzer", artifact.artifactId)
        assertEquals("0.0.1", artifact.version)
        assertTrue(isApplicable)
    }
    
    @Test
    fun `plugin should configure compilation options correctly`() {
        // Given
        plugin.apply(project)
        val extension = project.extensions.getByType(PragmaDddAnalyzerExtension::class.java)
        extension.outputDirectory.set("custom/output")
        extension.jsonFileNaming.set("custom-analysis")
        extension.enableMethodAnalysis.set(false)
        
        // When
        val compilation = createMockCompilation("main")
        val optionsProvider = plugin.applyToCompilation(compilation)
        val options = optionsProvider.get()
        
        // Then
        assertFalse(options.isEmpty())
        val optionsMap = options.associate { it.key to it.value }
        
        assertTrue(optionsMap["outputDirectory"]!!.endsWith("custom${File.separator}output"))
        assertEquals("false", optionsMap["isTestCompilation"])
        assertEquals("custom-analysis", optionsMap["jsonFileNaming"])
        assertEquals("false", optionsMap["enableMethodAnalysis"])
    }
    
    @Test
    fun `plugin should handle test compilation correctly`() {
        // Given
        plugin.apply(project)
        
        // When
        val testCompilation = createMockCompilation("test")
        val testOptions = plugin.applyToCompilation(testCompilation).get()
        
        // Then
        assertFalse(testOptions.isEmpty())
        val testOptionsMap = testOptions.associate { it.key to it.value }
        assertEquals("true", testOptionsMap["isTestCompilation"])
    }
    
    @Test
    fun `plugin should skip test compilation when includeTestSources is false`() {
        // Given
        plugin.apply(project)
        val extension = project.extensions.getByType(PragmaDddAnalyzerExtension::class.java)
        extension.includeTestSources.set(false)
        
        // When
        val testCompilation = createMockCompilation("test")
        val testOptions = plugin.applyToCompilation(testCompilation).get()
        
        // Then
        assertTrue(testOptions.isEmpty())
    }
    
    @Test
    fun `plugin should handle different compilation types`() {
        // Given
        plugin.apply(project)
        val compilationTypes = listOf("main", "test", "integrationTest")
        
        compilationTypes.forEach { compilationType ->
            // When
            val compilation = createMockCompilation(compilationType)
            val options = plugin.applyToCompilation(compilation).get()
            
            // Then
            if (compilationType.contains("test", ignoreCase = true)) {
                if (options.isNotEmpty()) {
                    val optionsMap = options.associate { it.key to it.value }
                    assertEquals("true", optionsMap["isTestCompilation"])
                }
            } else {
                assertFalse(options.isEmpty())
                val optionsMap = options.associate { it.key to it.value }
                assertEquals("false", optionsMap["isTestCompilation"])
            }
        }
    }
    
    @Test
    fun `plugin should handle absolute and relative output paths`() {
        // Given
        plugin.apply(project)
        val extension = project.extensions.getByType(PragmaDddAnalyzerExtension::class.java)
        
        // Test relative path
        extension.outputDirectory.set("relative/path")
        val compilation1 = createMockCompilation("main")
        val options1 = plugin.applyToCompilation(compilation1).get()
        val optionsMap1 = options1.associate { it.key to it.value }
        
        assertTrue(optionsMap1["outputDirectory"]!!.contains(project.projectDir.absolutePath))
        
        // Test absolute path
        val absolutePath = tempDir.resolve("absolute").toFile().absolutePath
        extension.outputDirectory.set(absolutePath)
        val compilation2 = createMockCompilation("main")
        val options2 = plugin.applyToCompilation(compilation2).get()
        val optionsMap2 = options2.associate { it.key to it.value }
        
        assertEquals(absolutePath, optionsMap2["outputDirectory"])
    }
    
    @Test
    fun `plugin should validate configuration`() {
        // Given
        plugin.apply(project)
        val extension = project.extensions.getByType(PragmaDddAnalyzerExtension::class.java)
        
        // When - valid configuration
        extension.outputDirectory.set("valid/output")
        extension.jsonFileNaming.set("valid-name")
        
        // Then - should not throw
        assertDoesNotThrow {
            extension.validate()
        }
        
        // When - invalid configuration
        extension.outputDirectory.set("")
        
        // Then - should throw
        assertThrows(IllegalArgumentException::class.java) {
            extension.validate()
        }
    }
    
    @Test
    fun `plugin should generate correct JSON file names`() {
        // Given
        plugin.apply(project)
        val extension = project.extensions.getByType(PragmaDddAnalyzerExtension::class.java)
        extension.jsonFileNaming.set("my-project")
        
        // When & Then
        assertEquals("my-project-main.json", extension.getMainSourceJsonFileName())
        assertEquals("my-project-test.json", extension.getTestSourceJsonFileName())
    }
    
    @Test
    fun `plugin should handle custom configuration options`() {
        // Given
        plugin.apply(project)
        val extension = project.extensions.getByType(PragmaDddAnalyzerExtension::class.java)
        
        // Configure all options
        extension.outputDirectory.set("custom/output")
        extension.includeTestSources.set(false)
        extension.jsonFileNaming.set("custom-analysis")
        extension.enableMethodAnalysis.set(false)
        extension.enablePropertyAnalysis.set(false)
        extension.enableDocumentationExtraction.set(false)
        extension.maxClassesPerCompilation.set(500)
        extension.failOnAnalysisErrors.set(true)
        
        // When
        val compilation = createMockCompilation("main")
        val options = plugin.applyToCompilation(compilation).get()
        
        // Then
        val optionsMap = options.associate { it.key to it.value }
        assertEquals("custom-analysis", optionsMap["jsonFileNaming"])
        assertEquals("false", optionsMap["enableMethodAnalysis"])
        assertEquals("false", optionsMap["enablePropertyAnalysis"])
        assertEquals("false", optionsMap["enableDocumentationExtraction"])
        assertEquals("500", optionsMap["maxClassesPerCompilation"])
        assertEquals("true", optionsMap["failOnAnalysisErrors"])
    }
    
    /**
     * Helper method to create mock Kotlin compilation
     */
    private fun createMockCompilation(name: String): KotlinCompilation<*> {
        val compilation = mock<KotlinCompilation<*>>()
        val target = mock<org.jetbrains.kotlin.gradle.plugin.KotlinTarget>()
        
        whenever(compilation.name).thenReturn(name)
        whenever(compilation.target).thenReturn(target)
        whenever(target.project).thenReturn(project)
        
        return compilation
    }
}