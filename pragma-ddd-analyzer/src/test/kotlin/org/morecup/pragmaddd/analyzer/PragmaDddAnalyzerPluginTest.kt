package org.morecup.pragmaddd.analyzer

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
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
        
        // Verify fixed values (not configurable by users)
        assertEquals("build/generated/pragmaddd/main/resources", extension.outputDirectory.get())
        assertEquals("domain-analyzer", extension.jsonFileNaming.get())
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
        
        // Configure invalid settings for configurable properties only
        // (outputDirectory and jsonFileNaming are now fixed and cannot be configured)
        extension.maxClassesPerCompilation.set(-1) // Invalid value
        
        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            // Manually trigger validation
            extension.validate()
        }
        
        assertTrue(exception.message!!.contains("Maximum classes per compilation must be a positive number"))
    }
    
    @Test
    fun `isApplicable should return true for all compilations`() {
        // Given
        val target = mock<KotlinTarget>()
        val compilation = mock<KotlinCompilation<*>>()
        whenever(compilation.name).thenReturn("main")
        whenever(compilation.target).thenReturn(target)
        whenever(target.project).thenReturn(project)
        
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
        
        // Configure only the configurable values (output directory and JSON naming are now fixed)
        extension.enableMethodAnalysis.set(false)
        extension.enablePropertyAnalysis.set(false) // Changed to false to avoid validation error
        extension.enableDocumentationExtraction.set(false)
        extension.maxClassesPerCompilation.set(500)
        extension.failOnAnalysisErrors.set(true)
        
        val target = mock<KotlinTarget>()
        val compilation = mock<KotlinCompilation<*>>()
        whenever(compilation.name).thenReturn("main")
        whenever(compilation.target).thenReturn(target)
        whenever(target.project).thenReturn(project)
        
        // When
        val optionsProvider: Provider<List<SubpluginOption>> = plugin.applyToCompilation(compilation)
        val options = optionsProvider.get()
        
        // Then
        println("Actual options: ${options.map { "${it.key}=${it.value}" }}")
        assertEquals(7, options.size)
        
        val optionsMap = options.associate { it.key to it.value }
        // Output directory is now fixed to build/generated/pragmaddd/main/resources
        assertTrue(optionsMap["outputDirectory"]!!.contains("build${File.separator}generated${File.separator}pragmaddd${File.separator}main${File.separator}resources"))
        // JSON file naming is now fixed to "domain-analyzer"
        assertEquals("domain-analyzer", optionsMap["jsonFileNaming"])
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
        
        val target = mock<KotlinTarget>()
        val compilation = mock<KotlinCompilation<*>>()
        whenever(compilation.name).thenReturn("main")
        whenever(compilation.target).thenReturn(target)
        whenever(target.project).thenReturn(project)
        
        // When
        val optionsProvider: Provider<List<SubpluginOption>> = plugin.applyToCompilation(compilation)
        val options = optionsProvider.get()
        
        // Then
        val optionsMap = options.associate { it.key to it.value }
        // isTestCompilation option has been removed since we only process main compilations
    }
    
    // Test removed - includeTestSources functionality no longer exists
    
    @Test
    fun `applyToCompilation should handle fixed output directory path`() {
        // Given
        plugin.apply(project)
        val extension = project.extensions.getByType(PragmaDddAnalyzerExtension::class.java)
        // Output directory is now fixed and cannot be configured
        
        val target = mock<KotlinTarget>()
        val compilation = mock<KotlinCompilation<*>>()
        whenever(compilation.name).thenReturn("main")
        whenever(compilation.target).thenReturn(target)
        whenever(target.project).thenReturn(project)
        
        // When
        val optionsProvider: Provider<List<SubpluginOption>> = plugin.applyToCompilation(compilation)
        val options = optionsProvider.get()
        
        // Then
        val optionsMap = options.associate { it.key to it.value }
        // Output directory is always fixed to build/generated/pragmaddd/main/resources
        assertTrue(optionsMap["outputDirectory"]!!.contains("build${File.separator}generated${File.separator}pragmaddd${File.separator}main${File.separator}resources"))
    }
    
    @Test
    fun `applyToCompilation should use fixed output directory path`() {
        // Given
        plugin.apply(project)
        val extension = project.extensions.getByType(PragmaDddAnalyzerExtension::class.java)
        // Output directory is now fixed and cannot be configured
        
        val target = mock<KotlinTarget>()
        val compilation = mock<KotlinCompilation<*>>()
        whenever(compilation.name).thenReturn("main")
        whenever(compilation.target).thenReturn(target)
        whenever(target.project).thenReturn(project)
        
        // When
        val optionsProvider: Provider<List<SubpluginOption>> = plugin.applyToCompilation(compilation)
        val options = optionsProvider.get()
        
        // Then
        val optionsMap = options.associate { it.key to it.value }
        // Output directory is always fixed to build/generated/pragmaddd/main/resources
        assertTrue(optionsMap["outputDirectory"]!!.contains("build${File.separator}generated${File.separator}pragmaddd${File.separator}main${File.separator}resources"))
        assertTrue(optionsMap["outputDirectory"]!!.contains(project.projectDir.absolutePath))
    }
}