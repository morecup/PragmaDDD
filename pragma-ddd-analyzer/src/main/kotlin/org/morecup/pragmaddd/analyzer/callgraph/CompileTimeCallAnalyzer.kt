package org.morecup.pragmaddd.analyzer.callgraph

import java.io.File

/**
 * 编译期调用关系分析器主类
 * 
 * 整合Repository识别、调用关系分析、字段访问分析和缓存生成功能
 */
class CompileTimeCallAnalyzer(
    private val includePackages: List<String> = emptyList(),
    private val excludePackages: List<String> = emptyList(),
    private val repositoryNamingRules: List<String> = emptyList(),
    private val debugMode: Boolean = false
) {
    
    private val repositoryIdentifier = RepositoryIdentifier(repositoryNamingRules)
    private val callGraphAnalyzer = CallGraphAnalyzer(includePackages, excludePackages, debugMode)
    private val fieldAccessAnalyzer = FieldAccessAnalyzer(debugMode)
    private val cacheGenerator = CacheGenerator(debugMode)
    
    /**
     * 分析结果
     */
    data class AnalysisResult(
        val repositories: List<RepositoryIdentifier.RepositoryInfo>,
        val callGraph: CallGraphAnalyzer.CallGraphResult,
        val fieldAccess: FieldAccessAnalyzer.FieldAccessResult,
        val cacheGenerated: Boolean
    )
    
    /**
     * 执行完整的编译期调用关系分析
     */
    fun analyze(
        classDirectory: File,
        domainAnalysisFile: File,
        outputCacheFile: File
    ): AnalysisResult {
        if (debugMode) {
            println("[CompileTimeCallAnalyzer] 开始编译期调用关系分析")
            println("[CompileTimeCallAnalyzer] 类目录: ${classDirectory.absolutePath}")
            println("[CompileTimeCallAnalyzer] Domain分析文件: ${domainAnalysisFile.absolutePath}")
            println("[CompileTimeCallAnalyzer] 输出缓存文件: ${outputCacheFile.absolutePath}")
        }
        
        // 第一步：识别Repository
        if (debugMode) {
            println("[CompileTimeCallAnalyzer] 步骤1: 识别Repository")
        }
        val repositories = repositoryIdentifier.identifyRepositories(classDirectory)
        
        if (repositories.isEmpty()) {
            if (debugMode) {
                println("[CompileTimeCallAnalyzer] 未发现任何Repository，跳过分析")
            }
            return AnalysisResult(
                repositories = repositories,
                callGraph = CallGraphAnalyzer.CallGraphResult(emptyList(), emptyMap()),
                fieldAccess = FieldAccessAnalyzer.FieldAccessResult(emptyMap()),
                cacheGenerated = false
            )
        }
        
        if (debugMode) {
            println("[CompileTimeCallAnalyzer] 发现Repository数量: ${repositories.size}")
            repositories.forEach { repo ->
                println("[CompileTimeCallAnalyzer] - ${repo.className} -> ${repo.aggregateRootClass} (${repo.identificationMethod})")
            }
        }
        
        // 第二步：分析调用关系
        if (debugMode) {
            println("[CompileTimeCallAnalyzer] 步骤2: 分析调用关系")
        }
        val callGraph = callGraphAnalyzer.analyzeCallGraph(classDirectory, repositories)
        
        if (callGraph.repositoryCalls.isEmpty()) {
            if (debugMode) {
                println("[CompileTimeCallAnalyzer] 未发现任何Repository调用，跳过后续分析")
            }
            return AnalysisResult(
                repositories = repositories,
                callGraph = callGraph,
                fieldAccess = FieldAccessAnalyzer.FieldAccessResult(emptyMap()),
                cacheGenerated = false
            )
        }
        
        if (debugMode) {
            println("[CompileTimeCallAnalyzer] 发现Repository调用数量: ${callGraph.repositoryCalls.size}")
        }
        
        // 第三步：分析字段访问
        if (debugMode) {
            println("[CompileTimeCallAnalyzer] 步骤3: 分析字段访问")
        }
        val fieldAccess = fieldAccessAnalyzer.analyzeFieldAccess(callGraph, domainAnalysisFile)
        
        // 第四步：生成缓存
        if (debugMode) {
            println("[CompileTimeCallAnalyzer] 步骤4: 生成缓存")
        }
        var cacheGenerated = false
        try {
            cacheGenerator.generateCache(fieldAccess, outputCacheFile)
            cacheGenerated = true
        } catch (e: Exception) {
            if (debugMode) {
                println("[CompileTimeCallAnalyzer] 缓存生成失败: ${e.message}")
            }
        }
        
        if (debugMode) {
            println("[CompileTimeCallAnalyzer] 编译期调用关系分析完成")
        }
        
        return AnalysisResult(
            repositories = repositories,
            callGraph = callGraph,
            fieldAccess = fieldAccess,
            cacheGenerated = cacheGenerated
        )
    }
    
    /**
     * 检查是否需要重新分析
     */
    fun shouldReanalyze(
        classDirectory: File,
        domainAnalysisFile: File,
        outputCacheFile: File
    ): Boolean {
        // 收集所有相关的源文件
        val sourceFiles = mutableListOf<File>()
        
        // 添加类文件
        classDirectory.walkTopDown()
            .filter { it.isFile && it.extension == "class" }
            .forEach { sourceFiles.add(it) }
        
        // 添加domain分析文件
        if (domainAnalysisFile.exists()) {
            sourceFiles.add(domainAnalysisFile)
        }
        
        return cacheGenerator.shouldUpdateCache(outputCacheFile, sourceFiles)
    }
    
    /**
     * 清理缓存
     */
    fun cleanCache(outputCacheFile: File) {
        cacheGenerator.cleanupCache(outputCacheFile)
    }
    
    /**
     * 读取缓存
     */
    fun readCache(cacheFile: File): CacheGenerator.CallAnalysisCache? {
        return cacheGenerator.readCache(cacheFile)
    }
    
    /**
     * 获取统计信息
     */
    fun getStatistics(result: AnalysisResult): Map<String, Any> {
        return mapOf(
            "repositoryCount" to result.repositories.size,
            "repositoryCallCount" to result.callGraph.repositoryCalls.size,
            "aggregateRootMethodCallCount" to result.callGraph.aggregateRootMethodCalls.values.sumOf { it.size },
            "repositoryMethodsWithCalls" to result.fieldAccess.repositoryMethodCalls.size,
            "totalCallerMethods" to result.fieldAccess.repositoryMethodCalls.values.sumOf { it.callerMethods.size },
            "cacheGenerated" to result.cacheGenerated,
            "repositories" to result.repositories.map { repo ->
                mapOf(
                    "className" to repo.className,
                    "aggregateRoot" to repo.aggregateRootClass,
                    "identificationMethod" to repo.identificationMethod.name,
                    "methodCount" to repo.methods.size
                )
            }
        )
    }
}
