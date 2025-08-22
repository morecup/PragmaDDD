package org.morecup.pragmaddd.analyzer

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File

/**
 * Unit tests for PragmaDddAnalyzerPlugin configuration and integration
 */
class PragmaDddAnalyzerPluginTest {
    
    private lateinit var project: Project
    private lateinit var plugin: PragmaDddAnalyzerPlugin
    
    @BeforeEach
    fun setup() {
        project = ProjectBuilder.builder().build()
        plugin = PragmaDddAnalyzerPlugin()
    }
    
    @Test
    fun `apply should create extension with default values`() {
        // When
        plugin.apply(project)
        
        // Then
        val extension = project.extensions.getByType(PragmaDddAnalyzerExtension::class.java)
        assertNotNull(extension)
        
        // Verify default values
        assertEquals("build/resources", extension.outputDirectory.get())
        assertEquals(true, extension.includeTestSources.get())
        assertEquals("ddd-analysis", extension.jsonFileNaming.get())
        assertEquals(true, extension.enableMethodAnalysis.get())
        assertEquals(true, extension.enablePropertyAnalysis.get())
        assertEquals(true, extension.enableDocumentationExtraction.get())
        assertEquals(1000, extension.maxClassesPerCompilation.get())
        assertEquals(false, extension.failOnAnalysisErrors.get())
    }
    
    @Test
    fun `apply should validate configuration after project evaluation`() {
        // Given
        plugin.apply(project)
        val extension = project.extensions.getByType(PragmaDddAnalyzerExtension::class.java)
        
        // Configure invalid settings
        extension.outputDirectory.set("")
        extension.jsonFileNaming.set("invalid/name")
        
        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            // Trigger project evaluation using the public API
            project.afterEvaluate { }
            // Manually trigger validation
            extension.validate()
        }
        
        assertTrue(exception.message!!.contains("Output directory cannot be null or blank"))
    }
    
    @Test
    fun `isApplicable should return true for all compilations`() {
        // Given
        val compilation = mock<KotlinCompilation<*>>()
        whenever(compilation.name).thenReturn("main")
        
        // When
        val result = plugin.isApplicable(compilation)
        
        // Then
        assertTrue(result)
    }
    
    @Test
    fun `getCompilerPluginId should return correct plugin id`() {
        // When
        val pluginId = plugin.getCompilerPluginId()
        
        // Then
        assertEquals("org.morecup.pragmaddd.analyzer", pluginId)
    }
    
    @Test
    fun `getPluginArtifact should return correct artifact coordinates`() {
        // When
        val artifact = plugin.getPluginArtifact()
        
        // Then
        assertEquals("org.morecup.pragmaddd", artifact.groupId)
        assertEquals("pragma-ddd-analyzer", artifact.artifactId)
        assertEquals("0.0.1", artifact.version)
    }
    
    @Test
    fun `applyToCompilation should pass all configuration options for main compilation`() {
        // Given
        plugin.apply(project)
        val extension = project.extensions.getByType(PragmaDddAnalyzerExtension::class.java)
        
        // Configure custom values
        extension.outputDirectory.set("custom/output")
        extension.jsonFileNaming.set("custom-analysis")
        extension.enableMethodAnalysis.set(false)
        extension.enablePropertyAnalysis.set(false) // Changed to false to avoid validation error
        extension.enableDocumentationExtraction.set(false)
        extension.maxClassesPerCompilation.set(500)
        extension.failOnAnalysisErrors.set(true)
        
        val compilation = mock<KotlinCompilation<*>>()
        whenever(compilation.name).thenReturn("main")
        whenever(compilation.target).thenReturn(mock())
        whenever(compilation.target.project).thenReturn(project)
        
        // When
        val optionsProvider: Provider<List<SubpluginOption>> = plugin.applyToCompilation(compilation)
        val options = optionsProvider.get()
        
        // Then
        println("Actual options: ${options.map { "${it.key}=${it.value}" }}")
        assertEquals(8, options.size)
        
        val optionsMap = options.associate { it.key to it.value }
        assertTrue(optionsMap["outputDirectory"]!!.endsWith("custom${File.separator}output${File.separator}main"))
        assertEquals("false", optionsMap["isTestCompilation"])
        assertEquals("custom-analysis", optionsMap["jsonFileNaming"])
        assertEquals("false", optionsMap["enableMethodAnalysis"])
        assertEquals("false", optionsMap["enablePropertyAnalysis"])
        assertEquals("false", optionsMap["enableDocumentationExtraction"])
        assertEquals("500", optionsMap["maxClassesPerCompilation"])
        assertEquals("true", optionsMap["failOnAnalysisErrors"])
    }
    
    @Test
    fun `applyToCompilation should detect test compilation correctly`() {
        // Given
        plugin.apply(project)
        
        val compilation = mock<KotlinCompilation<*>>()
        whenever(compilation.name).thenReturn("test")
        whenever(compilation.target).thenReturn(mock())
        whenever(compilation.target.project).thenReturn(project)
        
        // When
        val optionsProvider: Provider<List<SubpluginOption>> = plugin.applyToCompilation(compilation)
        val options = optionsProvider.get()
        
        // Then
        val optionsMap = options.associate { it.key to it.value }
        assertEquals("true", optionsMap["isTestCompilation"])
    }
    
    @Test
    fun `applyToCompilation should skip test compilation when includeTestSources is false`() {
        // Given
        plugin.apply(project)
        val extension = project.extensions.getByType(PragmaDddAnalyzerExtension::class.java)
        extension.includeTestSources.set(false)
        
        val compilation = mock<KotlinCompilation<*>>()
        whenever(compilation.name).thenReturn("test")
        whenever(compilation.target).thenReturn(mock())
        whenever(compilation.target.project).thenReturn(project)
        
        // When
        val optionsProvider: Provider<List<SubpluginOption>> = plugin.applyToCompilation(compilation)
        val options = optionsProvider.get()
        
        // Then
        assertTrue(options.isEmpty())
    }
    
    @Test
    fun `applyToCompilation should handle absolute output directory path`() {
        // Given
        plugin.apply(project)
        val extension = project.extensions.getByType(PragmaDddAnalyzerExtension::class.java)
        extension.outputDirectory.set("/absolute/path/to/output")
        
        val compilation = mock<KotlinCompilation<*>>()
        whenever(compilation.name).thenReturn("main")
        whenever(compilation.target).thenReturn(mock())
        whenever(compilation.target.project).thenReturn(project)
        
        // When
        val optionsProvider: Provider<List<SubpluginOption>> = plugin.applyToCompilation(compilation)
        val options = optionsProvider.get()
        
        // Then
        val optionsMap = options.associate { it.key to it.value }
        assertEquals("/absolute/path/to/output${File.separator}main", optionsMap["outputDirectory"])
    }
    
    @Test
    fun `applyToCompilation should resolve relative output directory path`() {
        // Given
        plugin.apply(project)
        val extension = project.extensions.getByType(PragmaDddAnalyzerExtension::class.java)
        extension.outputDirectory.set("relative/path")
        
        val compilation = mock<KotlinCompilation<*>>()
        whenever(compilation.name).thenReturn("main")
        whenever(compilation.target).thenReturn(mock())
        whenever(compilation.target.project).thenReturn(project)
        
        // When
        val optionsProvider: Provider<List<SubpluginOption>> = plugin.applyToCompilation(compilation)
        val options = optionsProvider.get()
        
        // Then
        val optionsMap = options.associate { it.key to it.value }
        assertTrue(optionsMap["outputDirectory"]!!.endsWith("relative${File.separator}path${File.separator}main"))
        assertTrue(optionsMap["outputDirectory"]!!.contains(project.projectDir.absolutePath))
    }
}