package org.morecup.pragmaddd.analyzer.integration

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.kotlin.*
import org.morecup.pragmaddd.analyzer.PragmaDddAnalyzerExtension
import org.morecup.pragmaddd.analyzer.PragmaDddAnalyzerPlugin
import java.io.File

class BuildLifecycleIntegrationTest {
    
    private lateinit var project: Project
    private lateinit var plugin: PragmaDddAnalyzerPlugin
    
    @BeforeEach
    fun setUp() {
        project = ProjectBuilder.builder().build()
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
        assertEquals("build/generated/resources", extension!!.outputDirectory.get())
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
    fun `plugin should generate correct compiler options for main compilation`() {
        // Given
        plugin.apply(project)
        val extension = project.extensions.getByType(PragmaDddAnalyzerExtension::class.java)
        extension.outputDirectory.set("build/custom-output")
        extension.jsonFileNaming.set("custom-analysis")
        extension.enableMethodAnalysis.set(false)
        
        val compilation = createMockCompilation("main")
        
        // When
        val optionsProvider = plugin.applyToCompilation(compilation)
        val options = optionsProvider.get()
        
        // Then
        assertTrue(options.isNotEmpty())
        
        val optionsMap = options.associate { it.key to it.value }
        assertTrue(optionsMap["outputDirectory"]!!.endsWith("build${File.separator}custom-output${File.separator}main"))
        assertEquals("custom-analysis", optionsMap["jsonFileNaming"])
        assertEquals("false", optionsMap["enableMethodAnalysis"])
    }
    
    @Test
    fun `plugin should handle compilation configuration correctly`() {
        // Given
        plugin.apply(project)
        val extension = project.extensions.getByType(PragmaDddAnalyzerExtension::class.java)
        
        // Configure with various settings
        extension.outputDirectory.set("/absolute/path")
        extension.enablePropertyAnalysis.set(true)
        extension.enableDocumentationExtraction.set(false)
        extension.maxClassesPerCompilation.set(500)
        extension.failOnAnalysisErrors.set(true)
        
        val compilation = createMockCompilation("main")
        
        // When
        val optionsProvider = plugin.applyToCompilation(compilation)
        val options = optionsProvider.get()
        
        // Then
        val optionsMap = options.associate { it.key to it.value }
        assertEquals("/absolute/path${File.separator}main", optionsMap["outputDirectory"])
        assertEquals("true", optionsMap["enablePropertyAnalysis"])
        assertEquals("false", optionsMap["enableDocumentationExtraction"])
        assertEquals("500", optionsMap["maxClassesPerCompilation"])
        assertEquals("true", optionsMap["failOnAnalysisErrors"])
    }
    
    @Test
    fun `plugin should validate extension configuration`() {
        // Given
        plugin.apply(project)
        val extension = project.extensions.getByType(PragmaDddAnalyzerExtension::class.java)
        
        // When & Then - should not throw with valid configuration
        assertDoesNotThrow {
            extension.validate()
        }
    }
    
    @Test
    fun `plugin should handle invalid extension configuration`() {
        // Given
        plugin.apply(project)
        val extension = project.extensions.getByType(PragmaDddAnalyzerExtension::class.java)
        extension.outputDirectory.set("")
        
        // When & Then - should throw with invalid configuration
        assertThrows(IllegalArgumentException::class.java) {
            extension.validate()
        }
    }
    
    @Test
    fun `plugin should provide configuration summary`() {
        // Given
        plugin.apply(project)
        val extension = project.extensions.getByType(PragmaDddAnalyzerExtension::class.java)
        
        // When
        val summary = extension.getConfigurationSummary()
        
        // Then
        assertNotNull(summary)
        assertTrue(summary.contains("Pragma DDD Analyzer Configuration"))
        assertTrue(summary.contains("Output Directory"))
        assertTrue(summary.contains("JSON File Naming"))
    }
    
    private fun createMockCompilation(name: String): KotlinCompilation<*> {
        val compilation = mock<KotlinCompilation<*>>()
        whenever(compilation.name).thenReturn(name)
        whenever(compilation.target).thenReturn(mock())
        whenever(compilation.target.project).thenReturn(project)
        return compilation
    }
}