package org.morecup.pragmaddd.analyzer.compiler

import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey

@OptIn(ExperimentalCompilerApi::class)
class DddAnalysisCommandLineProcessor : CommandLineProcessor {
    
    // Override the deprecated method to delegate to the new one
    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun processOption(
        option: CliOption,
        value: String,
        configuration: CompilerConfiguration
    ) {
        processOption(option as AbstractCliOption, value, configuration)
    }
    
    companion object {
        const val PLUGIN_ID = "org.morecup.pragmaddd.analyzer"
        
        val OUTPUT_DIRECTORY_KEY = CompilerConfigurationKey<String>("output.directory")
        val JSON_FILE_NAMING_KEY = CompilerConfigurationKey<String>("json.file.naming")
        val ENABLE_METHOD_ANALYSIS_KEY = CompilerConfigurationKey<Boolean>("enable.method.analysis")
        val ENABLE_PROPERTY_ANALYSIS_KEY = CompilerConfigurationKey<Boolean>("enable.property.analysis")
        val ENABLE_DOCUMENTATION_EXTRACTION_KEY = CompilerConfigurationKey<Boolean>("enable.documentation.extraction")
        val MAX_CLASSES_PER_COMPILATION_KEY = CompilerConfigurationKey<Int>("max.classes.per.compilation")
        val FAIL_ON_ANALYSIS_ERRORS_KEY = CompilerConfigurationKey<Boolean>("fail.on.analysis.errors")
        
        const val OUTPUT_DIRECTORY_OPTION = "outputDirectory"
        const val JSON_FILE_NAMING_OPTION = "jsonFileNaming"
        const val ENABLE_METHOD_ANALYSIS_OPTION = "enableMethodAnalysis"
        const val ENABLE_PROPERTY_ANALYSIS_OPTION = "enablePropertyAnalysis"
        const val ENABLE_DOCUMENTATION_EXTRACTION_OPTION = "enableDocumentationExtraction"
        const val MAX_CLASSES_PER_COMPILATION_OPTION = "maxClassesPerCompilation"
        const val FAIL_ON_ANALYSIS_ERRORS_OPTION = "failOnAnalysisErrors"
    }
    
    override val pluginId: String = PLUGIN_ID
    
    override val pluginOptions: Collection<CliOption> = listOf(
        CliOption(
            optionName = OUTPUT_DIRECTORY_OPTION,
            valueDescription = "path",
            description = "Output directory for generated JSON files",
            required = false
        ),
        CliOption(
            optionName = JSON_FILE_NAMING_OPTION,
            valueDescription = "string",
            description = "JSON file naming convention",
            required = false
        ),
        CliOption(
            optionName = ENABLE_METHOD_ANALYSIS_OPTION,
            valueDescription = "true|false",
            description = "Enable method analysis",
            required = false
        ),
        CliOption(
            optionName = ENABLE_PROPERTY_ANALYSIS_OPTION,
            valueDescription = "true|false",
            description = "Enable property analysis",
            required = false
        ),
        CliOption(
            optionName = ENABLE_DOCUMENTATION_EXTRACTION_OPTION,
            valueDescription = "true|false",
            description = "Enable documentation extraction from KDoc",
            required = false
        ),
        CliOption(
            optionName = MAX_CLASSES_PER_COMPILATION_OPTION,
            valueDescription = "number",
            description = "Maximum number of classes to analyze per compilation",
            required = false
        ),
        CliOption(
            optionName = FAIL_ON_ANALYSIS_ERRORS_OPTION,
            valueDescription = "true|false",
            description = "Whether to fail the build on analysis errors",
            required = false
        )
    )
    
    override fun processOption(
        option: AbstractCliOption,
        value: String,
        configuration: CompilerConfiguration
    ) {
        when (option.optionName) {
            OUTPUT_DIRECTORY_OPTION -> {
                configuration.put(OUTPUT_DIRECTORY_KEY, value)
            }
            JSON_FILE_NAMING_OPTION -> {
                configuration.put(JSON_FILE_NAMING_KEY, value)
            }
            ENABLE_METHOD_ANALYSIS_OPTION -> {
                configuration.put(ENABLE_METHOD_ANALYSIS_KEY, value.toBoolean())
            }
            ENABLE_PROPERTY_ANALYSIS_OPTION -> {
                configuration.put(ENABLE_PROPERTY_ANALYSIS_KEY, value.toBoolean())
            }
            ENABLE_DOCUMENTATION_EXTRACTION_OPTION -> {
                configuration.put(ENABLE_DOCUMENTATION_EXTRACTION_KEY, value.toBoolean())
            }
            MAX_CLASSES_PER_COMPILATION_OPTION -> {
                configuration.put(MAX_CLASSES_PER_COMPILATION_KEY, value.toInt())
            }
            FAIL_ON_ANALYSIS_ERRORS_OPTION -> {
                configuration.put(FAIL_ON_ANALYSIS_ERRORS_KEY, value.toBoolean())
            }
        }
    }
}