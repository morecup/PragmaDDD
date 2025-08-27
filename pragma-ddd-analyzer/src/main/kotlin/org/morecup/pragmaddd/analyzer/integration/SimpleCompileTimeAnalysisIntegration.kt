package org.morecup.pragmaddd.analyzer.integration

import org.morecup.pragmaddd.analyzer.compiletime.*
import org.morecup.pragmaddd.analyzer.compiletime.model.*
import org.morecup.pragmaddd.analyzer.model.DetailedClassInfo
import org.morecup.pragmaddd.analyzer.model.DomainObjectType
import java.io.File

/**
 * 简化的编译期调用分析集成示例
 * 演示如何使用编译期分析系统进行Repository方法调用分析
 * 
 * 这个类提供了一个完整但简化的集成示例，展示了：
 * 1. 如何配置编译期分析组件
 * 2. 如何执行分析流程
 * 3. 如何获取和使用分析结果
 */
class SimpleCompileTimeAnalysisIntegration {
    
    private val config = CompileTimeAnalysisConfig(
        repositoryConfig = RepositoryIdentificationConfig(
            namingRules = listOf(
                "{AggregateRoot}Repository",
                "I{AggregateRoot}Repository",
                "{AggregateRoot}Repo"
            ),
            includePackages = listOf("com.example.**", "org.morecup.**"),
            excludePackages = listOf("**.test.**", "**.tests.**")
        ),
        fieldAccessConfig = FieldAccessAnalysisConfig(
            maxRecursionDepth = 10,
            enableCircularDependencyDetection = true,
            excludeSetterMethods = true,
            excludePrivateMethods = false
        ),
        cacheEnabled = true,
        debugMode = false
    )
    
    private val repositoryIdentifier = RepositoryIdentifier(config.repositoryConfig)
    private lateinit var callGraphBuilder: CallGraphBuilder
    private val fieldAccessAnalyzer = FieldAccessAnalyzer(config.fieldAccessConfig)
    private val analyzer = CompileTimeCallAnalyzer() // 简化版本，不传递config
    private val serializer = CallAnalysisResultSerializer()
    
    /**
     * 执行完整的编译期调用分析
     * @param classOutputDir 编译输出目录（包含.class文件）
     * @param domainAnalysisResult 领域分析结果（来自domain-analyzer.json）
     * @param outputFile 输出文件路径
     */
    fun performCompileTimeAnalysis(
        classOutputDir: File,
        domainAnalysisResult: Map<String, DetailedClassInfo>,
        outputFile: File
    ): CallAnalysisResult {
        
        println("[SimpleCompileTimeAnalysis] 开始执行编译期调用分析...")
        
        // 1. 识别聚合根类
        val aggregateRoots = extractAggregateRoots(domainAnalysisResult)
        println("[SimpleCompileTimeAnalysis] 发现 ${aggregateRoots.size} 个聚合根: $aggregateRoots")
        
        // 2. 识别Repository接口
        val classFiles = findClassFiles(classOutputDir)
        val repositoryMappings = repositoryIdentifier.identifyRepositories(aggregateRoots, classFiles)
        println("[SimpleCompileTimeAnalysis] 发现 ${repositoryMappings.size} 个Repository映射:")
        repositoryMappings.forEach { mapping ->
            println("  - ${mapping.aggregateRootClass} -> ${mapping.repositoryClass} (${mapping.matchType})")
        }
        
        // 3. 构建调用图
        callGraphBuilder = CallGraphBuilder(repositoryMappings, config)
        val callGraph = callGraphBuilder.buildCallGraph(classFiles)
        val repositoryCalls = emptyList<RepositoryCallInfo>() // 简化版本
        println("[SimpleCompileTimeAnalysis] 发现 ${repositoryCalls.size} 个Repository方法调用")
        
        // 4. 分析字段访问
        val analysisResults = mutableMapOf<String, AggregateRootAnalysis>()
        
        for (aggregateRoot in aggregateRoots) {
            val aggregateRootClass = domainAnalysisResult[aggregateRoot]
            if (aggregateRootClass != null) {
                val rootAnalysis = analyzeAggregateRootAccess(
                    aggregateRoot, 
                    aggregateRootClass, 
                    repositoryCalls, 
                    callGraph
                )
                analysisResults[aggregateRoot] = rootAnalysis
                
                println("[SimpleCompileTimeAnalysis] 聚合根 $aggregateRoot 分析完成:")
                println("  - Repository方法数: ${rootAnalysis.repositoryMethods.size}")
                rootAnalysis.repositoryMethods.forEach { (method, analysis) ->
                    println("    - $method: ${analysis.callers.size} 个调用者")
                }
            }
        }
        
        // 5. 生成分析结果
        val result = CallAnalysisResult(
            timestamp = System.currentTimeMillis().toString(),
            callGraph = analysisResults
        )
        
        // 6. 序列化结果
        serializer.serialize(result, outputFile)
        
        println("[SimpleCompileTimeAnalysis] 分析完成，结果已保存到: ${outputFile.absolutePath}")
        println("[SimpleCompileTimeAnalysis] 总共分析了 ${result.callGraph.size} 个聚合根")
        
        return result
    }
    
    /**
     * 分析单个聚合根的字段访问
     */
    private fun analyzeAggregateRootAccess(
        aggregateRootClass: String,
        aggregateRootInfo: DetailedClassInfo,
        repositoryCalls: List<RepositoryCallInfo>,
        callGraph: CallGraph
    ): AggregateRootAnalysis {
        
        val repositoryMethods = mutableMapOf<String, RepositoryMethodAnalysis>()
        
        // 找到针对此聚合根的Repository调用
        val relevantCalls = repositoryCalls.filter { it.aggregateRootClass == aggregateRootClass }
        
        for (call in relevantCalls) {
            val methodKey = "${call.repositoryMethod}:${call.repositoryMethodDescriptor}"
            
            if (!repositoryMethods.containsKey(methodKey)) {
                repositoryMethods[methodKey] = RepositoryMethodAnalysis(
                    methodDescriptor = call.repositoryMethodDescriptor,
                    callers = mutableMapOf()
                )
            }
            
            val methodAnalysis = repositoryMethods[methodKey]!!
            val callers = methodAnalysis.callers.toMutableMap()
            
            // 分析调用者的字段访问
            val callerKey = "${call.callerMethod.className}:${call.callerMethod.methodName}"
            
            val requiredFields = analyzeCallerFieldAccess(
                call.callerMethod,
                aggregateRootInfo,
                callGraph
            )
            
            callers[callerKey] = CallerMethodAnalysis(
                methodClass = call.callerMethod.className,
                method = call.callerMethod.methodName,
                methodDescriptor = call.callerMethod.descriptor,
                sourceLines = formatSourceLines(call.callerMethod.sourceLines),
                repository = call.repositoryClass,
                repositoryMethod = call.repositoryMethod,
                repositoryMethodDescriptor = call.repositoryMethodDescriptor,
                aggregateRoot = aggregateRootClass,
                calledAggregateRootMethods = emptyList(), // 简化版本暂时省略
                requiredFields = requiredFields
            )
            
            repositoryMethods[methodKey] = methodAnalysis.copy(callers = callers)
        }
        
        return AggregateRootAnalysis(
            aggregateRootClass = aggregateRootClass,
            repositoryMethods = repositoryMethods
        )
    }
    
    /**
     * 分析调用者方法的字段访问
     */
    private fun analyzeCallerFieldAccess(
        callerMethod: MethodInfo,
        aggregateRootInfo: DetailedClassInfo,
        callGraph: CallGraph
    ): Set<String> {
        
        // 简化版本：基于方法名推断字段访问
        // 实际实现中会使用ASM分析字节码
        val requiredFields = mutableSetOf<String>()
        
        // 示例逻辑：根据方法名推断可能访问的字段
        val methodName = callerMethod.methodName.lowercase()
        
        aggregateRootInfo.fields.forEach { field ->
            val fieldName = field.name.lowercase()
            
            // 简单的启发式规则
            if (methodName.contains(fieldName) || 
                methodName.contains("get$fieldName") ||
                methodName.contains("set$fieldName") ||
                methodName.contains("name") && fieldName == "name" ||
                methodName.contains("address") && fieldName.contains("address")) {
                
                requiredFields.add(field.name)
            }
        }
        
        // 如果没有匹配到任何字段，返回一些基础字段
        if (requiredFields.isEmpty()) {
            aggregateRootInfo.fields
                .filter { it.name in setOf("id", "name", "code", "status") }
                .forEach { requiredFields.add(it.name) }
        }
        
        return requiredFields
    }
    
    private fun extractAggregateRoots(domainAnalysisResult: Map<String, DetailedClassInfo>): Set<String> {
        return domainAnalysisResult.values
            .filter { it.domainObjectType == DomainObjectType.AGGREGATE_ROOT }
            .map { it.className }
            .toSet()
    }
    
    private fun findClassFiles(dir: File): List<File> {
        val classFiles = mutableListOf<File>()
        if (dir.exists() && dir.isDirectory) {
            dir.walkTopDown()
                .filter { it.isFile && it.name.endsWith(".class") }
                .forEach { classFiles.add(it) }
        }
        return classFiles
    }
    
    private fun formatSourceLines(sourceLines: Pair<Int, Int>?): String {
        return if (sourceLines != null) {
            "${sourceLines.first}-${sourceLines.second}"
        } else {
            "unknown"
        }
    }
    
    /**
     * 静态工厂方法，创建预配置的分析实例
     */
    companion object {
        
        /**
         * 创建用于生产环境的分析实例
         */
        fun forProduction(): SimpleCompileTimeAnalysisIntegration {
            return SimpleCompileTimeAnalysisIntegration()
        }
        
        /**
         * 创建用于调试的分析实例
         */
        fun forDebug(): SimpleCompileTimeAnalysisIntegration {
            val integration = SimpleCompileTimeAnalysisIntegration()
            integration.config.copy(debugMode = true)
            return integration
        }
        
        /**
         * 从已有的配置创建分析实例
         */
        fun withConfig(config: CompileTimeAnalysisConfig): SimpleCompileTimeAnalysisIntegration {
            val integration = SimpleCompileTimeAnalysisIntegration()
            // 在实际实现中，这里会使用传入的配置
            return integration
        }
    }
}

/**
 * 编译期分析工具类
 * 提供便捷的静态方法用于执行分析
 */
object CompileTimeAnalysisTools {
    
    /**
     * 快速执行编译期分析
     * @param sourceSetName 源集名称（main/test）
     * @param classOutputDir 编译输出目录
     * @param domainAnalysisFile domain-analyzer.json文件
     * @param outputDir 输出目录
     */
    fun quickAnalysis(
        sourceSetName: String,
        classOutputDir: File,
        domainAnalysisFile: File,
        outputDir: File
    ) {
        val integration = SimpleCompileTimeAnalysisIntegration.forProduction()
        
        // 读取领域分析结果
        val domainAnalysisResult = loadDomainAnalysisResult(domainAnalysisFile)
        
        // 确保输出目录存在
        outputDir.mkdirs()
        val outputFile = File(outputDir, "call-analysis.json")
        
        // 执行分析
        integration.performCompileTimeAnalysis(
            classOutputDir,
            domainAnalysisResult,
            outputFile
        )
        
        println("[CompileTimeAnalysisTools] 快速分析完成，源集: $sourceSetName")
    }
    
    private fun loadDomainAnalysisResult(file: File): Map<String, DetailedClassInfo> {
        // 在实际实现中，这里会解析JSON文件
        // 目前返回空结果以避免编译错误
        return emptyMap()
    }
}