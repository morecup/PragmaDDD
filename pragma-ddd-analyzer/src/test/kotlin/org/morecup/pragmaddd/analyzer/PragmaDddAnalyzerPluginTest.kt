package org.morecup.pragmaddd.analyzer

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.assertj.core.api.Assertions.assertThat

class PragmaDddAnalyzerPluginTest {
    
    private lateinit var project: Project
    
    @BeforeEach
    fun setup() {
        project = ProjectBuilder.builder().build()
        project.plugins.apply(JavaPlugin::class.java)
        project.plugins.apply(PragmaDddAnalyzerPlugin::class.java)
    }
    
    @Test
    fun `should apply plugin successfully`() {
        // 验证插件已应用
        assertThat(project.plugins.hasPlugin(PragmaDddAnalyzerPlugin::class.java)).isTrue()
        
        // 验证扩展已创建
        val extension = project.extensions.findByType(PragmaDddAnalyzerExtension::class.java)
        assertThat(extension).isNotNull()
    }
    
    @Test
    fun `should configure main source set compilation tasks`() {
        // 验证main源集的编译任务存在
        val compileJavaTask = project.tasks.findByName("compileJava")
        assertThat(compileJavaTask).isNotNull()
        
        // 验证任务已被增强（通过检查扩展是否存在）
        val analysisAction = compileJavaTask?.extensions?.findByName("pragmaDddAnalysis")
        assertThat(analysisAction).isNotNull()
        assertThat(analysisAction).isInstanceOf(DddAnalysisAction::class.java)
        
        val action = analysisAction as DddAnalysisAction
        assertThat(action.sourceSetName).isEqualTo("main")
    }
    
    @Test
    fun `should configure test source set compilation tasks`() {
        // 验证test源集的编译任务存在
        val compileTestJavaTask = project.tasks.findByName("compileTestJava")
        assertThat(compileTestJavaTask).isNotNull()
        
        // 验证任务已被增强（通过检查扩展是否存在）
        val analysisAction = compileTestJavaTask?.extensions?.findByName("pragmaDddAnalysis")
        assertThat(analysisAction).isNotNull()
        assertThat(analysisAction).isInstanceOf(DddAnalysisAction::class.java)
        
        val action = analysisAction as DddAnalysisAction
        assertThat(action.sourceSetName).isEqualTo("test")
    }
    
    @Test
    fun `should configure correct output paths for different source sets`() {
        val compileJavaTask = project.tasks.findByName("compileJava")
        val compileTestJavaTask = project.tasks.findByName("compileTestJava")
        
        val mainAnalysisAction = compileJavaTask?.extensions?.findByName("pragmaDddAnalysis") as? DddAnalysisAction
        val testAnalysisAction = compileTestJavaTask?.extensions?.findByName("pragmaDddAnalysis") as? DddAnalysisAction
        
        assertThat(mainAnalysisAction).isNotNull()
        assertThat(testAnalysisAction).isNotNull()
        
        // 验证输出路径
        val mainOutputFile = mainAnalysisAction!!.getOutputFile(compileJavaTask!!)
        val testOutputFile = testAnalysisAction!!.getOutputFile(compileTestJavaTask!!)
        
        assertThat(mainOutputFile.path).contains("build${java.io.File.separator}generated${java.io.File.separator}pragmaddd${java.io.File.separator}main${java.io.File.separator}resources${java.io.File.separator}META-INF${java.io.File.separator}pragma-ddd-analyzer${java.io.File.separator}domain-analyzer.json")
        assertThat(testOutputFile.path).contains("build${java.io.File.separator}generated${java.io.File.separator}pragmaddd${java.io.File.separator}test${java.io.File.separator}resources${java.io.File.separator}META-INF${java.io.File.separator}pragma-ddd-analyzer${java.io.File.separator}domain-analyzer.json")
    }
    
    @Test
    fun `should configure process resources tasks dependencies`() {
        val processResourcesTask = project.tasks.findByName("processResources")
        val processTestResourcesTask = project.tasks.findByName("processTestResources")
        
        assertThat(processResourcesTask).isNotNull()
        assertThat(processTestResourcesTask).isNotNull()
        
        // 验证依赖关系
        val mainDependencies = processResourcesTask!!.dependsOn
        val testDependencies = processTestResourcesTask!!.dependsOn
        
        assertThat(mainDependencies.map { it.toString() }).contains("compileJava")
        assertThat(testDependencies.map { it.toString() }).contains("compileTestJava")
    }
}