package org.morecup.pragmaddd.analyzer.compiletime

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.morecup.pragmaddd.analyzer.compiletime.model.*
import org.morecup.pragmaddd.analyzer.model.DetailedAnalysisResult
import org.morecup.pragmaddd.analyzer.model.DetailedClassInfo
import java.io.File
import java.time.Instant

/**
 * 编译期调用分析器
 * 整合所有分析组件，提供完整的编译期静态分析功能
 */
class CompileTimeCallAnalyzer(
    private val config: CompileTimeAnalysisConfig = CompileTimeAnalysisConfig()
) {
    
    private val objectMapper = ObjectMapper().registerModule(kotlinModule())
    private val repositoryIdentifier = RepositoryIdentifier(config.repositoryConfig)
    private val fieldAccessAnalyzer = FieldAccessAnalyzer(config.fieldAccessConfig)
    
    /**
     * 执行完整的编译期调用分析
     * @param compilationOutputDir 编译输出目录
     * @param domainAnalysisFile domain-analyzer.json文件
     * @return 完整的调用分析结果
     */
    fun analyze(
        compilationOutputDir: File,
        domainAnalysisFile: File
    ): CallAnalysisResult {
        
        if (!domainAnalysisFile.exists()) {
            throw IllegalArgumentException("Domain analysis file not found: ${domainAnalysisFile.absolutePath}")
        }
        
        if (config.debugMode) {
            println("[CompileTimeCallAnalyzer] Starting analysis...")
            println("[CompileTimeCallAnalyzer] Compilation output: ${compilationOutputDir.absolutePath}")
            println("[CompileTimeCallAnalyzer] Domain analysis file: ${domainAnalysisFile.absolutePath}")
        }
        
        // 1. 加载domain-analyzer.json结果
        val domainAnalysisResult = loadDomainAnalysisResult(domainAnalysisFile)
        
        // 2. 提取聚合根类
        val aggregateRoots = repositoryIdentifier.extractAggregateRoots(domainAnalysisResult)
        if (config.debugMode) {
            println("[CompileTimeCallAnalyzer] Found ${aggregateRoots.size} aggregate roots: ${aggregateRoots}")
        }
        
        // 3. 收集所有class文件
        val classFiles = collectClassFiles(compilationOutputDir)
        if (config.debugMode) {
            println("[CompileTimeCallAnalyzer] Found ${classFiles.size} class files")
        }
        
        // 4. 识别Repository映射关系
        val repositoryMappings = repositoryIdentifier.identifyRepositories(aggregateRoots, classFiles)
        if (config.debugMode) {
            println("[CompileTimeCallAnalyzer] Found ${repositoryMappings.size} repository mappings:")
            repositoryMappings.forEach { mapping ->
                println("  ${mapping.aggregateRootClass} -> ${mapping.repositoryClass} (${mapping.matchType})")
            }
        }
        
        // 5. 构建调用图
        val callGraphBuilder = CallGraphBuilder(repositoryMappings, config)
        val callGraph = callGraphBuilder.buildCallGraph(classFiles)
        if (config.debugMode) {
            println("[CompileTimeCallAnalyzer] Built call graph with ${callGraph.getRepositoryCalls().size} repository calls")
        }
        
        // 6. 分析字段访问模式
        val analysisResult = analyzeFieldAccessPatterns(
            callGraph,
            repositoryMappings,
            domainAnalysisResult
        )
        
        if (config.debugMode) {
            println("[CompileTimeCallAnalyzer] Analysis completed")
        }
        
        return analysisResult
    }
    
    /**
     * 加载domain-analyzer.json分析结果
     */
    private fun loadDomainAnalysisResult(domainAnalysisFile: File): Map<String, DetailedClassInfo> {
        val detailedAnalysisResult: DetailedAnalysisResult = objectMapper.readValue(domainAnalysisFile)
        return detailedAnalysisResult.classes.associateBy { it.className }
    }
    
    /**
     * 收集编译输出目录中的所有class文件
     */
    private fun collectClassFiles(compilationOutputDir: File): List<File> {
        val classFiles = mutableListOf<File>()
        
        if (compilationOutputDir.exists() && compilationOutputDir.isDirectory) {
            compilationOutputDir.walkTopDown()
                .filter { it.isFile && it.extension == "class" }
                .forEach { classFiles.add(it) }
        }
        
        return classFiles
    }
    
    /**
     * 分析字段访问模式
     */
    private fun analyzeFieldAccessPatterns(
        callGraph: CallGraph,
        repositoryMappings: List<AggregateRootRepositoryMapping>,
        domainAnalysisResult: Map<String, DetailedClassInfo>
    ): CallAnalysisResult {
        
        val aggregateRootAnalysisMap = mutableMapOf<String, AggregateRootAnalysis>()
        
        // 按聚合根分组处理Repository调用
        val repositoryCallsByAggregateRoot = callGraph.getRepositoryCalls()
            .groupBy { it.aggregateRootClass }
        
        for ((aggregateRootClass, repositoryCalls) in repositoryCallsByAggregateRoot) {
            val repositoryMethodsMap = mutableMapOf<String, RepositoryMethodAnalysis>()
            
            // 按Repository方法分组
            val callsByRepositoryMethod = repositoryCalls.groupBy { 
                "${it.repositoryMethod}${it.repositoryMethodDescriptor}" 
            }
            
            for ((repositoryMethodKey, methodCalls) in callsByRepositoryMethod) {
                val callersMap = mutableMapOf<String, CallerMethodAnalysis>()
                
                for (repositoryCall in methodCalls) {
                    val callerMethod = repositoryCall.callerMethod
                    
                    // 分析字段访问
                    val fieldAccessResult = fieldAccessAnalyzer.analyzeRequiredFields(
                        repositoryCall,
                        callGraph,
                        domainAnalysisResult
                    )
                    
                    // 生成调用者键值
                    val callerKey = generateCallerKey(callerMethod)
                    
                    // 创建调用者分析结果
                    val callerAnalysis = CallerMethodAnalysis(
                        methodClass = callerMethod.className,
                        method = callerMethod.methodName,
                        methodDescriptor = callerMethod.descriptor,
                        sourceLines = formatSourceLines(callerMethod.sourceLines),
                        repository = repositoryCall.repositoryClass,
                        repositoryMethod = repositoryCall.repositoryMethod,
                        repositoryMethodDescriptor = repositoryCall.repositoryMethodDescriptor,
                        aggregateRoot = repositoryCall.aggregateRootClass,
                        calledAggregateRootMethods = fieldAccessResult.calledAggregateRootMethods,
                        requiredFields = fieldAccessResult.requiredFields
                    )
                    
                    callersMap[callerKey] = callerAnalysis
                }
                
                // 创建Repository方法分析结果
                val repositoryMethodAnalysis = RepositoryMethodAnalysis(
                    methodDescriptor = methodCalls.first().repositoryMethodDescriptor,
                    callers = callersMap
                )
                
                repositoryMethodsMap[repositoryMethodKey] = repositoryMethodAnalysis
            }
            
            // 创建聚合根分析结果
            val aggregateRootAnalysis = AggregateRootAnalysis(
                aggregateRootClass = aggregateRootClass,
                repositoryMethods = repositoryMethodsMap
            )
            
            aggregateRootAnalysisMap[aggregateRootClass] = aggregateRootAnalysis
        }
        
        return CallAnalysisResult(
            version = "1.0",
            timestamp = Instant.now().toString(),
            callGraph = aggregateRootAnalysisMap
        )
    }
    
    /**
     * 生成调用者键值
     */
    private fun generateCallerKey(callerMethod: MethodInfo): String {
        val sourceLines = formatSourceLines(callerMethod.sourceLines)
        return "${callerMethod.className}.${callerMethod.methodName}+$sourceLines"
    }
    
    /**
     * 格式化源码行号
     */
    private fun formatSourceLines(sourceLines: Pair<Int, Int>?): String {
        return if (sourceLines != null) {
            "${sourceLines.first}-${sourceLines.second}"
        } else {
            "unknown"
        }
    }
    
    /**
     * 写入分析结果到文件
     */
    fun writeResults(result: CallAnalysisResult, outputFile: File) {
        outputFile.parentFile?.mkdirs()
        objectMapper.writerWithDefaultPrettyPrinter()
            .writeValue(outputFile, result)
        
        if (config.debugMode) {
            println("[CompileTimeCallAnalyzer] Results written to: ${outputFile.absolutePath}")
        }
    }
    
    /**
     * 验证分析结果
     */
    fun validateResults(result: CallAnalysisResult): List<String> {
        val issues = mutableListOf<String>()
        
        // 检查是否有分析结果
        if (result.callGraph.isEmpty()) {
            issues.add("No call graph data found")
        }
        
        // 检查每个聚合根的分析结果
        for ((aggregateRoot, analysis) in result.callGraph) {
            if (analysis.repositoryMethods.isEmpty()) {
                issues.add("No repository methods found for aggregate root: $aggregateRoot")
            }
            
            for ((repositoryMethod, methodAnalysis) in analysis.repositoryMethods) {
                if (methodAnalysis.callers.isEmpty()) {
                    issues.add("No callers found for repository method: $aggregateRoot.$repositoryMethod")
                }
                
                for ((caller, callerAnalysis) in methodAnalysis.callers) {
                    if (callerAnalysis.requiredFields.isEmpty()) {
                        issues.add("No required fields found for caller: $caller")
                    }
                }
            }
        }
        
        return issues
    }
    
    /**
     * 生成分析报告
     */
    fun generateAnalysisReport(result: CallAnalysisResult): String {
        val report = StringBuilder()
        
        report.appendLine("# 编译期调用分析报告")
        report.appendLine()
        report.appendLine("**分析时间:** ${result.timestamp}")
        report.appendLine("**版本:** ${result.version}")
        report.appendLine()
        
        // 统计信息
        val totalAggregateRoots = result.callGraph.size
        val totalRepositoryMethods = result.callGraph.values.sumOf { it.repositoryMethods.size }
        val totalCallers = result.callGraph.values
            .flatMap { it.repositoryMethods.values }
            .sumOf { it.callers.size }
        
        report.appendLine("## 统计信息")
        report.appendLine("- 聚合根数量: $totalAggregateRoots")
        report.appendLine("- Repository方法数量: $totalRepositoryMethods")
        report.appendLine("- 调用方数量: $totalCallers")
        report.appendLine()
        
        // 详细分析结果
        report.appendLine("## 详细分析结果")
        for ((aggregateRoot, analysis) in result.callGraph) {
            report.appendLine("### $aggregateRoot")
            
            for ((repositoryMethod, methodAnalysis) in analysis.repositoryMethods) {
                report.appendLine("#### Repository方法: $repositoryMethod")
                
                for ((caller, callerAnalysis) in methodAnalysis.callers) {
                    report.appendLine("**调用方:** ${callerAnalysis.methodClass}.${callerAnalysis.method}")
                    report.appendLine("**源码行号:** ${callerAnalysis.sourceLines}")
                    report.appendLine("**需要字段:** ${callerAnalysis.requiredFields.joinToString(", ")}")
                    
                    if (callerAnalysis.calledAggregateRootMethods.isNotEmpty()) {
                        report.appendLine("**调用的聚合根方法:**")
                        for (calledMethod in callerAnalysis.calledAggregateRootMethods) {
                            report.appendLine("  - ${calledMethod.aggregateRootMethod}: ${calledMethod.requiredFields.joinToString(", ")}")
                        }
                    }
                    report.appendLine()
                }
            }
        }
        
        return report.toString()
    }
}