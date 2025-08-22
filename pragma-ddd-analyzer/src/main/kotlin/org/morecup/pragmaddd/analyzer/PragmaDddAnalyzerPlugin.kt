package org.morecup.pragmaddd.analyzer

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File

/**
 * Pragma DDD Analyzer Gradle Plugin
 * Integrates the Kotlin compiler plugin for DDD analysis
 */
class PragmaDddAnalyzerPlugin : KotlinCompilerPluginSupportPlugin {
    
    companion object {
        const val PLUGIN_ID = "org.morecup.pragmaddd.analyzer"
        const val EXTENSION_NAME = "pragmaDddAnalyzer"
    }
    
    override fun apply(target: Project) {
        // Create extension configuration
        val extension = target.extensions.create(
            EXTENSION_NAME,
            PragmaDddAnalyzerExtension::class.java
        )
        
        // Set default values - use resources directory for JAR packaging
        extension.outputDirectory.convention("build/resources")
        extension.includeTestSources.convention(true)
        extension.jsonFileNaming.convention("ddd-analysis")
        extension.enableMethodAnalysis.convention(true)
        extension.enablePropertyAnalysis.convention(true)
        extension.enableDocumentationExtraction.convention(true)
        extension.maxClassesPerCompilation.convention(1000)
        extension.failOnAnalysisErrors.convention(false)
        
        // Configure build lifecycle integration
        configureBuildLifecycle(target, extension)
        
        // Validate configuration after project evaluation
        target.afterEvaluate { project ->
            try {
                extension.validate()
                project.logger.info("Pragma DDD Analyzer configuration validated successfully")
                project.logger.info(extension.getConfigurationSummary())
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Invalid Pragma DDD Analyzer configuration: ${e.message}", e)
            }
        }
        
        // Log plugin application
        target.logger.info("Applied Pragma DDD Analyzer plugin with Kotlin compiler plugin integration")
    }
    
    /**
     * Configures integration with Gradle build lifecycle
     */
    private fun configureBuildLifecycle(project: Project, extension: PragmaDddAnalyzerExtension) {
        // Configure task dependencies and incremental compilation support
        project.afterEvaluate { evaluatedProject ->
            configureTaskDependencies(evaluatedProject, extension)
            configureIncrementalCompilation(evaluatedProject)
            configureResourceGeneration(evaluatedProject, extension)
            configureTestJarPackaging(evaluatedProject, extension)
        }
    }
    
    /**
     * Configures proper task dependencies for JSON generation
     */
    private fun configureTaskDependencies(project: Project, extension: PragmaDddAnalyzerExtension) {
        // Find all Kotlin compilation tasks
        project.tasks.withType(KotlinCompile::class.java) { compileTask ->
            // Ensure output directory exists before compilation
            compileTask.doFirst {
                val outputDir = extension.getResolvedOutputDirectory(project.projectDir)
                if (!outputDir.exists()) {
                    outputDir.mkdirs()
                    project.logger.info("DDD Analyzer: Created output directory: ${outputDir.absolutePath}")
                }
            }
            
            // Log completion of analysis after compilation
            compileTask.doLast {
                val outputDir = extension.getResolvedOutputDirectory(project.projectDir)
                val mainJsonFile = File(outputDir, extension.getMainSourceJsonFileName())
                val testJsonFile = File(outputDir, extension.getTestSourceJsonFileName())
                
                if (mainJsonFile.exists()) {
                    project.logger.info("DDD Analyzer: Generated main source analysis: ${mainJsonFile.absolutePath}")
                }
                
                if (testJsonFile.exists() && extension.includeTestSources.get()) {
                    project.logger.info("DDD Analyzer: Generated test source analysis: ${testJsonFile.absolutePath}")
                }
            }
        }
        
        // Ensure processResources task depends on main Kotlin compilation for proper JAR packaging
        project.tasks.findByName("processResources")?.let { processResourcesTask ->
            project.tasks.findByName("compileKotlin")?.let { compileKotlinTask ->
                processResourcesTask.dependsOn(compileKotlinTask)
            }
        }
    }
    
    /**
     * Configures incremental compilation compatibility
     */
    private fun configureIncrementalCompilation(project: Project) {
        project.tasks.withType(KotlinCompile::class.java) { compileTask ->
            // Add output directory as task output for incremental compilation
            val extension = project.extensions.getByType(PragmaDddAnalyzerExtension::class.java)
            val outputDir = extension.getResolvedOutputDirectory(project.projectDir)
            compileTask.outputs.dir(outputDir)
            
            // Mark as incremental compilation compatible
            compileTask.outputs.upToDateWhen {
                // Check if any source files have changed compared to output files
                val outputFiles = outputDir.listFiles()?.filter { it.name.endsWith(".json") } ?: emptyList()
                if (outputFiles.isEmpty()) {
                    false // No output files exist, need to run
                } else {
                    val outputLastModified = outputFiles.maxOfOrNull { it.lastModified() } ?: 0L
                    // Simple check - if output directory is newer than a reasonable time, consider up-to-date
                    outputLastModified > (System.currentTimeMillis() - 60000) // 1 minute threshold
                }
            }
        }
    }
    
    /**
     * Configures resource generation for JAR packaging
     */
    private fun configureResourceGeneration(project: Project, extension: PragmaDddAnalyzerExtension) {
        // Ensure generated JSON files are included in JAR resources
        project.tasks.findByName("jar")?.let { jarTask ->
            jarTask.doFirst {
                val outputDir = extension.getResolvedOutputDirectory(project.projectDir)
                if (outputDir.exists()) {
                    project.logger.info("DDD Analyzer: Including JSON files from ${outputDir.absolutePath} in JAR")
                }
            }
        }
        
        // Note: Test JAR packaging is handled separately to avoid circular dependencies
    }
    
    /**
     * Configures test JAR packaging for test metadata
     */
    private fun configureTestJarPackaging(project: Project, extension: PragmaDddAnalyzerExtension) {
        // Create test JAR task if it doesn't exist and test sources are enabled
        if (extension.includeTestSources.get()) {
            project.afterEvaluate { evaluatedProject ->
                // Only create testJar if it doesn't exist to avoid conflicts
                if (evaluatedProject.tasks.findByName("testJar") == null) {
                    evaluatedProject.tasks.create("testJar", org.gradle.api.tasks.bundling.Jar::class.java) { jar ->
                        jar.group = "build"
                        jar.description = "Assembles a jar archive containing test DDD analysis metadata"
                        jar.archiveClassifier.set("test")
                        
                        // Only include test analysis metadata, not test classes to avoid circular dependencies
                        jar.doFirst {
                            val outputDir = extension.getResolvedOutputDirectory(evaluatedProject.projectDir)
                            val testJsonFile = File(outputDir, extension.getTestSourceJsonFileName())
                            if (testJsonFile.exists()) {
                                evaluatedProject.logger.info("DDD Analyzer: Including test JSON file in test JAR: ${testJsonFile.absolutePath}")
                                // Add the JSON file to the JAR
                                jar.from(testJsonFile.parentFile) {
                                    it.include(testJsonFile.name)
                                }
                            }
                        }
                        
                        // Make test JAR depend on test compilation but not on main classes
                        evaluatedProject.tasks.findByName("compileTestKotlin")?.let { compileTestKotlin ->
                            jar.dependsOn(compileTestKotlin)
                        }
                    }
                }
            }
        }
    }
    
    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {
        // Apply to all Kotlin compilations
        println("DDD Analyzer: isApplicable() called for compilation: ${kotlinCompilation.name}")
        return true
    }
    
    override fun getCompilerPluginId(): String {
        println("DDD Analyzer: getCompilerPluginId() called, returning: $PLUGIN_ID")
        return PLUGIN_ID
    }
    
    override fun getPluginArtifact(): SubpluginArtifact {
        println("DDD Analyzer: getPluginArtifact() called")
        return SubpluginArtifact(
            groupId = "org.morecup.pragmaddd",
            artifactId = "pragma-ddd-analyzer",
            version = "0.0.1"
        )
    }
    
    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project
        val extension = project.extensions.getByType(PragmaDddAnalyzerExtension::class.java)
        
        return project.provider {
            val options = mutableListOf<SubpluginOption>()
            
            // Determine if this is a test compilation
            val isTestCompilation = kotlinCompilation.name.contains("test", ignoreCase = true)
            
            // Pass output directory to compiler plugin - different for main and test
            val baseOutputDir = extension.outputDirectory.get()
            val sourceSetDir = if (isTestCompilation) "test" else "main"
            val fullOutputDir = "$baseOutputDir/$sourceSetDir"
            
            val resolvedOutputDir = if (baseOutputDir.startsWith("/") || (baseOutputDir.length > 1 && baseOutputDir[1] == ':')) {
                // Absolute path - append source set directory directly using File.separator
                "$baseOutputDir${File.separator}$sourceSetDir"
            } else {
                // Relative path - resolve relative to project directory
                project.layout.projectDirectory.dir(fullOutputDir).asFile.absolutePath
            }
            options.add(SubpluginOption(key = "outputDirectory", value = resolvedOutputDir))
            
            // Skip test compilation if includeTestSources is false
            if (isTestCompilation && !extension.includeTestSources.get()) {
                project.logger.info("DDD Analyzer: Skipping test compilation '${kotlinCompilation.name}' as includeTestSources is disabled")
                return@provider emptyList<SubpluginOption>()
            }
            
            options.add(SubpluginOption(key = "isTestCompilation", value = isTestCompilation.toString()))
            
            // Pass JSON file naming configuration
            options.add(SubpluginOption(key = "jsonFileNaming", value = extension.jsonFileNaming.get()))
            
            // Pass analysis feature flags
            options.add(SubpluginOption(key = "enableMethodAnalysis", value = extension.enableMethodAnalysis.get().toString()))
            options.add(SubpluginOption(key = "enablePropertyAnalysis", value = extension.enablePropertyAnalysis.get().toString()))
            options.add(SubpluginOption(key = "enableDocumentationExtraction", value = extension.enableDocumentationExtraction.get().toString()))
            
            // Pass additional configuration options
            options.add(SubpluginOption(key = "maxClassesPerCompilation", value = extension.maxClassesPerCompilation.get().toString()))
            options.add(SubpluginOption(key = "failOnAnalysisErrors", value = extension.failOnAnalysisErrors.get().toString()))
            
            project.logger.info("DDD Analyzer: Configuring compiler plugin for compilation '${kotlinCompilation.name}' with output directory: $resolvedOutputDir")
            project.logger.debug("DDD Analyzer: Configuration options: ${options.map { "${it.key}=${it.value}" }.joinToString(", ")}")
            
            options
        }
    }
}