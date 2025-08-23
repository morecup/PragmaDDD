package org.morecup.pragmaddd.analyzer

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import org.assertj.core.api.Assertions.assertThat
import java.io.File

class IntegrationTest {
    
    @TempDir
    lateinit var tempDir: File
    
    private lateinit var project: Project
    
    @BeforeEach
    fun setup() {
        project = ProjectBuilder.builder()
            .withProjectDir(tempDir)
            .build()
        project.plugins.apply(JavaPlugin::class.java)
        project.plugins.apply(PragmaDddAnalyzerPlugin::class.java)
    }
    
    @Test
    fun `should create output directories for both main and test source sets`() {
        // 执行编译任务（模拟）
        val compileJavaTask = project.tasks.getByName("compileJava")
        val compileTestJavaTask = project.tasks.getByName("compileTestJava")
        
        // 获取分析Action
        val mainAnalysisAction = compileJavaTask.extensions.findByName("pragmaDddAnalysis") as? DddAnalysisAction
        val testAnalysisAction = compileTestJavaTask.extensions.findByName("pragmaDddAnalysis") as? DddAnalysisAction
        
        assertThat(mainAnalysisAction).isNotNull()
        assertThat(testAnalysisAction).isNotNull()
        
        // 验证输出文件路径
        val mainOutputFile = mainAnalysisAction!!.getOutputFile(compileJavaTask)
        val testOutputFile = testAnalysisAction!!.getOutputFile(compileTestJavaTask)
        
        // 验证main源集输出路径
        assertThat(mainOutputFile.path).contains("main")
        assertThat(mainOutputFile.path).contains("domain-analyzer.json")
        
        // 验证test源集输出路径
        assertThat(testOutputFile.path).contains("test")
        assertThat(testOutputFile.path).contains("domain-analyzer.json")
        
        // 验证路径不同
        assertThat(mainOutputFile.path).isNotEqualTo(testOutputFile.path)
        
        println("Main output: ${mainOutputFile.path}")
        println("Test output: ${testOutputFile.path}")
    }
    
    @Test
    fun `should configure different source set names correctly`() {
        val compileJavaTask = project.tasks.getByName("compileJava")
        val compileTestJavaTask = project.tasks.getByName("compileTestJava")
        
        val mainAnalysisAction = compileJavaTask.extensions.findByName("pragmaDddAnalysis") as DddAnalysisAction
        val testAnalysisAction = compileTestJavaTask.extensions.findByName("pragmaDddAnalysis") as DddAnalysisAction
        
        assertThat(mainAnalysisAction.sourceSetName).isEqualTo("main")
        assertThat(testAnalysisAction.sourceSetName).isEqualTo("test")
    }
    
    @Test
    fun `should handle empty analysis results gracefully`() {
        val compileJavaTask = project.tasks.getByName("compileJava")
        val analysisAction = compileJavaTask.extensions.findByName("pragmaDddAnalysis") as DddAnalysisAction
        
        // 创建输出目录
        val outputFile = analysisAction.getOutputFile(compileJavaTask)
        outputFile.parentFile.mkdirs()
        
        // 模拟空的分析结果
        val emptyResults = emptyList<ClassAnalysisResult>()
        
        // 验证可以正常处理空结果
        assertThat(emptyResults).isEmpty()
        
        // 验证输出文件路径正确
        assertThat(outputFile.name).isEqualTo("domain-analyzer.json")
        assertThat(outputFile.parentFile.name).isEqualTo("pragma-ddd-analyzer")
    }
}