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
        
        // Fixed paths that cannot be configured by users
        const val FIXED_OUTPUT_DIRECTORY = "build/generated/pragmaddd/main/resources"
        const val FIXED_JSON_FILENAME = "domain-analyzer"
        const val FIXED_META_INF_PATH = "META-INF/pragma-ddd-analyzer"
        const val FIXED_COMPLETE_JSON_PATH = "$FIXED_OUTPUT_DIRECTORY/$FIXED_META_INF_PATH/$FIXED_JSON_FILENAME.json"
    }
    
    override fun apply(target: Project) {
        // Create extension configuration
        val extension = target.extensions.create(
            EXTENSION_NAME,
            PragmaDddAnalyzerExtension::class.java
        )
        
        // Set fixed values - these are NOT configurable by users
        // Fixed path: build/generated/pragmaddd/main/resources/META-INF/pragma-ddd-analyzer/domain-analyzer.json
        // These values are hardcoded and cannot be changed by users
        extension.outputDirectory.set(FIXED_OUTPUT_DIRECTORY)
        extension.jsonFileNaming.set(FIXED_JSON_FILENAME)
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
        }
    }
    
    /**
     * Configures proper task dependencies for JSON generation
     */
    private fun configureTaskDependencies(project: Project, extension: PragmaDddAnalyzerExtension) {
        // Find all Kotlin compilation tasks
        project.tasks.withType(KotlinCompile::class.java) { compileTask ->
            val isTestTask = compileTask.name.contains("test", ignoreCase = true)
            
            if (!isTestTask) {
                // Only configure main compilation tasks
                // Ensure FIXED output directory exists before compilation
                // Fixed path: build/generated/pragmaddd/main/resources/META-INF/pragma-ddd-analyzer/domain-analyzer.json
                compileTask.doFirst {
                    val fixedOutputDir = File(project.projectDir, FIXED_OUTPUT_DIRECTORY)
                    val metaInfDir = File(fixedOutputDir, FIXED_META_INF_PATH)
                    if (!metaInfDir.exists()) {
                        metaInfDir.mkdirs()
                        project.logger.info("DDD Analyzer: Created FIXED output directory: ${metaInfDir.absolutePath}")
                    }
                }
                
                // Log completion of analysis after compilation
                compileTask.doLast {
                    val fixedOutputDir = File(project.projectDir, FIXED_OUTPUT_DIRECTORY)
                    val fixedJsonFile = File(fixedOutputDir, "$FIXED_META_INF_PATH/$FIXED_JSON_FILENAME.json")
                    
                    if (fixedJsonFile.exists()) {
                        project.logger.info("DDD Analyzer: Generated main source analysis at FIXED path: ${fixedJsonFile.absolutePath}")
                    } else {
                        project.logger.warn("DDD Analyzer: Expected JSON file not found at FIXED path: ${fixedJsonFile.absolutePath}")
                    }
                }
            } else {
                // For test tasks, just log that we're not processing them
                compileTask.doLast {
                    val fixedOutputDir = File(project.projectDir, FIXED_OUTPUT_DIRECTORY)
                    val fixedJsonFile = File(fixedOutputDir, "$FIXED_META_INF_PATH/$FIXED_JSON_FILENAME.json")
                    
                    if (fixedJsonFile.exists()) {
                        project.logger.info("DDD Analyzer: Main source analysis available at FIXED path: ${fixedJsonFile.absolutePath}")
                    } else {
                        project.logger.info("DDD Analyzer: No main source analysis found (test compilation does not generate analysis)")
                    }
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
            // Only configure incremental compilation for main compilation tasks, not test tasks
            val isTestTask = compileTask.name.contains("test", ignoreCase = true)
            if (!isTestTask) {
                // Add FIXED output directory as task output for incremental compilation
                val fixedOutputDir = File(project.projectDir, FIXED_OUTPUT_DIRECTORY)
                val metaInfDir = File(fixedOutputDir, FIXED_META_INF_PATH)
                
                // Register the META-INF directory as output for incremental compilation
                compileTask.outputs.dir(metaInfDir)
                
                // Mark as incremental compilation compatible
                compileTask.outputs.upToDateWhen {
                    // Check if the FIXED JSON file exists and is recent
                    val fixedOutputDir = File(project.projectDir, FIXED_OUTPUT_DIRECTORY)
                    val fixedJsonFile = File(fixedOutputDir, "$FIXED_META_INF_PATH/$FIXED_JSON_FILENAME.json")
                    if (!fixedJsonFile.exists()) {
                        false // No output file exists, need to run
                    } else {
                        val outputLastModified = fixedJsonFile.lastModified()
                        // Simple check - if output file is newer than a reasonable time, consider up-to-date
                        outputLastModified > (System.currentTimeMillis() - 60000) // 1 minute threshold
                    }
                }
            }
        }
    }
    
    /**
     * Configures resource generation for JAR packaging
     */
    private fun configureResourceGeneration(project: Project, extension: PragmaDddAnalyzerExtension) {
        // Ensure generated JSON files are included in JAR resources from FIXED path
        project.tasks.findByName("jar")?.let { jarTask ->
            jarTask.doFirst {
                val fixedOutputDir = File(project.projectDir, FIXED_OUTPUT_DIRECTORY)
                val fixedJsonFile = File(fixedOutputDir, "$FIXED_META_INF_PATH/$FIXED_JSON_FILENAME.json")
                if (fixedJsonFile.exists()) {
                    project.logger.info("DDD Analyzer: Including JSON file from FIXED path ${fixedJsonFile.absolutePath} in JAR")
                } else {
                    project.logger.warn("DDD Analyzer: Expected JSON file not found at FIXED path ${fixedJsonFile.absolutePath}")
                }
            }
        }
        
        // Note: Test JAR packaging is handled separately to avoid circular dependencies
    }
    

    
    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {
        // Only apply to main compilations, skip test compilations
        val isTestCompilation = kotlinCompilation.name.contains("test", ignoreCase = true)
        
        // Skip applying the plugin to its own module to prevent self-analysis
        val projectName = kotlinCompilation.target.project.name
        val isOwnModule = projectName == "pragma-ddd-analyzer"
        
        println("DDD Analyzer: isApplicable() called for compilation: ${kotlinCompilation.name}, project: $projectName, isTest: $isTestCompilation, isOwnModule: $isOwnModule")
        
        // Don't apply to test compilations or to the plugin's own module
        return !isTestCompilation && !isOwnModule
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
            
            // Use fixed output directory - NOT configurable by users
            // Fixed path: build/generated/pragmaddd/main/resources/META-INF/pragma-ddd-analyzer/domain-analyzer.json
            val resolvedOutputDir = project.layout.projectDirectory.dir(FIXED_OUTPUT_DIRECTORY).asFile.absolutePath
            options.add(SubpluginOption(key = "outputDirectory", value = resolvedOutputDir))
            

            
            // Pass JSON file naming configuration
            options.add(SubpluginOption(key = "jsonFileNaming", value = extension.jsonFileNaming.get()))
            
            // Pass analysis feature flags
            options.add(SubpluginOption(key = "enableMethodAnalysis", value = extension.enableMethodAnalysis.get().toString()))
            options.add(SubpluginOption(key = "enablePropertyAnalysis", value = extension.enablePropertyAnalysis.get().toString()))
            options.add(SubpluginOption(key = "enableDocumentationExtraction", value = extension.enableDocumentationExtraction.get().toString()))
            
            // Pass additional configuration options
            options.add(SubpluginOption(key = "maxClassesPerCompilation", value = extension.maxClassesPerCompilation.get().toString()))
            options.add(SubpluginOption(key = "failOnAnalysisErrors", value = extension.failOnAnalysisErrors.get().toString()))
            
            project.logger.info("DDD Analyzer: Configuring compiler plugin for main compilation '${kotlinCompilation.name}' with fixed output directory: $resolvedOutputDir")
            project.logger.debug("DDD Analyzer: Configuration options: ${options.map { "${it.key}=${it.value}" }.joinToString(", ")}")
            
            // Add debug information about the plugin artifact
            val pluginArtifact = getPluginArtifact()
            project.logger.info("DDD Analyzer: Plugin artifact - groupId: ${pluginArtifact.groupId}, artifactId: ${pluginArtifact.artifactId}, version: ${pluginArtifact.version}")
            
            options
        }
    }
}