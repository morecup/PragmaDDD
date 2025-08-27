package org.morecup.pragmaddd.analyzer.compiletime

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import org.morecup.pragmaddd.analyzer.compiletime.model.*
import org.morecup.pragmaddd.analyzer.model.*
import org.morecup.pragmaddd.analyzer.runtime.CompileTimeAnalysisUtils
import java.io.File
import java.nio.file.Path

/**
 * 编译期调用分析集成测试
 * 验证整个分析流程的端到端功能
 */
class CallAnalysisIntegrationTest {
    
    @TempDir
    lateinit var tempDir: Path
    
    private lateinit var compilationOutputDir: File
    private lateinit var domainAnalysisFile: File
    
    @BeforeEach
    fun setup() {
        compilationOutputDir = tempDir.resolve("classes").toFile().apply { mkdirs() }
        domainAnalysisFile = tempDir.resolve("domain-analyzer.json").toFile()
        
        // 创建模拟的domain-analyzer.json文件
        createMockDomainAnalysisFile()
        
        // 创建模拟的class文件
        createMockClassFiles()
    }
    
    @Test
    fun `test end-to-end call analysis workflow`() {
        // 1. 创建分析器配置
        val config = CompileTimeAnalysisConfig(
            repositoryConfig = RepositoryIdentificationConfig(
                namingRules = listOf("{AggregateRoot}Repository", "I{AggregateRoot}Repository"),
                includePackages = listOf("org.morecup.pragmaddd.jimmer.**"),
                excludePackages = listOf("org.morecup.pragmaddd.jimmer.admin.**")
            ),
            fieldAccessConfig = FieldAccessAnalysisConfig(
                maxRecursionDepth = 10,
                enableCircularDependencyDetection = true,
                excludeSetterMethods = true
            ),
            cacheEnabled = true,
            debugMode = true
        )
        
        // 2. 执行完整的分析流程
        val analyzer = CompileTimeCallAnalyzer(config)
        val result = analyzer.analyze(compilationOutputDir, domainAnalysisFile)
        
        // 3. 验证分析结果
        assertNotNull(result)
        assertEquals("1.0", result.version)
        assertFalse(result.callGraph.isEmpty())
        
        // 4. 验证聚合根分析
        assertTrue(result.callGraph.containsKey("org.morecup.pragmaddd.jimmer.domain.goods.Goods"))
        
        val goodsAnalysis = result.callGraph["org.morecup.pragmaddd.jimmer.domain.goods.Goods"]!!
        assertFalse(goodsAnalysis.repositoryMethods.isEmpty())
        
        // 5. 验证Repository方法分析
        val findByIdMethod = goodsAnalysis.repositoryMethods.values.first()
        assertFalse(findByIdMethod.callers.isEmpty())
        
        val callerAnalysis = findByIdMethod.callers.values.first()
        assertEquals("org.morecup.pragmaddd.jimmer.domain.goods.Goods", callerAnalysis.aggregateRoot)
        assertFalse(callerAnalysis.requiredFields.isEmpty())
        
        // 6. 验证结果序列化
        val serializer = CallAnalysisResultSerializer()
        val outputFile = tempDir.resolve("call-analysis.json").toFile()
        serializer.serialize(result, outputFile)
        
        assertTrue(outputFile.exists())
        val deserializedResult = serializer.deserialize(outputFile)
        assertEquals(result.version, deserializedResult.version)
        assertEquals(result.callGraph.size, deserializedResult.callGraph.size)
        
        // 7. 验证运行时API
        CompileTimeAnalysisUtils.loadAnalysisResults(result)
        
        val requiredFields = CompileTimeAnalysisUtils.getRequiredFieldsByMethod(
            "org.morecup.pragmaddd.jimmer.domain.goods.Goods",
            "org.morecup.pragmaddd.jimmer.domain.goods.ChangeAddressCmdHandler",
            "handle"
        )
        
        assertFalse(requiredFields.isEmpty())
        
        println("✅ 集成测试通过：")
        println("  - 分析结果包含 ${result.callGraph.size} 个聚合根")
        println("  - 总共分析了 ${result.callGraph.values.sumOf { it.repositoryMethods.size }} 个Repository方法")
        println("  - 总共分析了 ${result.callGraph.values.flatMap { it.repositoryMethods.values }.sumOf { it.callers.size }} 个调用者")
        println("  - 运行时API正常工作")
    }
    
    @Test
    fun `test incremental analysis performance`() {
        val cacheDir = tempDir.resolve("cache").toFile()
        val incrementalManager = IncrementalAnalysisManager(cacheDir, true)
        
        val config = CompileTimeAnalysisConfig(debugMode = true)
        val analyzer = CompileTimeCallAnalyzer(config)
        
        // 第一次分析（完整分析）
        val startTime1 = System.currentTimeMillis()
        assertTrue(incrementalManager.shouldAnalyze(compilationOutputDir, domainAnalysisFile))
        
        val result1 = incrementalManager.performIncrementalAnalysis(
            compilationOutputDir,
            domainAnalysisFile,
            analyzer
        )
        val duration1 = System.currentTimeMillis() - startTime1
        
        assertNotNull(result1)
        
        // 第二次分析（增量分析，应该使用缓存）
        val startTime2 = System.currentTimeMillis()
        val shouldAnalyze2 = incrementalManager.shouldAnalyze(compilationOutputDir, domainAnalysisFile)
        
        val result2 = if (shouldAnalyze2) {
            incrementalManager.performIncrementalAnalysis(
                compilationOutputDir,
                domainAnalysisFile,
                analyzer
            )
        } else {
            incrementalManager.getCachedResult(compilationOutputDir, domainAnalysisFile)!!
        }
        val duration2 = System.currentTimeMillis() - startTime2
        
        assertNotNull(result2)
        
        println("📊 性能测试结果：")
        println("  - 第一次分析耗时：${duration1}ms")
        println("  - 第二次分析耗时：${duration2}ms")
        println("  - 性能提升：${if (duration1 > 0) String.format("%.1f", (duration1 - duration2) * 100.0 / duration1) else "N/A"}%")
        
        // 验证结果一致性
        assertEquals(result1.callGraph.size, result2.callGraph.size)
    }
    
    @Test
    fun `test jimmer repository pattern recognition`() {
        // 测试Jimmer项目中的Repository识别模式
        val repositoryIdentifier = RepositoryIdentifier(
            RepositoryIdentificationConfig(
                namingRules = listOf("{AggregateRoot}Repository"),
                includePackages = listOf("org.morecup.pragmaddd.jimmer.domain.**"),
                excludePackages = listOf("org.morecup.pragmaddd.jimmer.admin.**")
            )
        )
        
        // 从模拟的domain分析结果中提取聚合根
        val domainAnalysisResult = loadDomainAnalysisResult()
        val aggregateRoots = repositoryIdentifier.extractAggregateRoots(domainAnalysisResult)
        
        assertTrue(aggregateRoots.contains("org.morecup.pragmaddd.jimmer.domain.goods.Goods"))
        
        // 创建模拟的Repository类文件
        val repositoryClassFiles = listOf(
            createMockRepositoryClass("GoodsRepository.class"),
            createMockRepositoryClass("OrderRepository.class")
        )
        
        val mappings = repositoryIdentifier.identifyRepositories(aggregateRoots, repositoryClassFiles)
        
        // 验证Repository映射
        assertFalse(mappings.isEmpty())
        println("🔍 Repository识别结果：")
        mappings.forEach { mapping ->
            println("  - ${mapping.aggregateRootClass} -> ${mapping.repositoryClass} (${mapping.matchType})")
        }
    }
    
    @Test
    fun `test field access pattern analysis`() {
        val fieldAccessAnalyzer = FieldAccessAnalyzer(
            FieldAccessAnalysisConfig(
                maxRecursionDepth = 5,
                enableCircularDependencyDetection = true,
                excludeSetterMethods = true
            )
        )
        
        // 创建模拟的调用图
        val callGraph = CallGraph()
        val repositoryCall = RepositoryCallInfo(
            callerMethod = MethodInfo(
                "org.morecup.pragmaddd.jimmer.domain.goods.ChangeAddressCmdHandler",
                "handle",
                "(Lorg/morecup/pragmaddd/jimmer/domain/goods/ChangeAddressCmd;)V",
                Pair(14, 17)
            ),
            repositoryClass = "org.morecup.pragmaddd.jimmer.domain.goods.GoodsRepository",
            repositoryMethod = "findByIdOrErr",
            repositoryMethodDescriptor = "(J)Lorg/morecup/pragmaddd/jimmer/domain/goods/Goods;",
            aggregateRootClass = "org.morecup.pragmaddd.jimmer.domain.goods.Goods"
        )
        
        callGraph.addRepositoryCall(repositoryCall)
        
        // 添加聚合根方法调用
        val changeAddressMethod = MethodInfo(
            "org.morecup.pragmaddd.jimmer.domain.goods.Goods",
            "changeAddress",
            "(Ljava/lang/String;)V"
        )
        callGraph.addMethodCall(repositoryCall.callerMethod, changeAddressMethod)
        
        // 执行字段访问分析
        val domainAnalysisResult = loadDomainAnalysisResult()
        val result = fieldAccessAnalyzer.analyzeRequiredFields(
            repositoryCall,
            callGraph,
            domainAnalysisResult
        )
        
        assertNotNull(result)
        assertFalse(result.requiredFields.isEmpty())
        
        println("🔍 字段访问分析结果：")
        println("  - 需要的字段：${result.requiredFields}")
        println("  - 调用的聚合根方法：${result.calledAggregateRootMethods.size}")
        
        result.calledAggregateRootMethods.forEach { methodInfo ->
            println("    - ${methodInfo.aggregateRootMethod}: ${methodInfo.requiredFields}")
        }
    }
    
    private fun createMockDomainAnalysisFile() {
        val domainAnalysisResult = DetailedAnalysisResult(
            sourceSetName = "main",
            classes = listOf(createMockGoodsClassInfo()),
            summary = AnalysisSummary(
                totalClasses = 1,
                aggregateRootCount = 1,
                domainEntityCount = 0,
                valueObjectCount = 0
            )
        )
        
        val objectMapper = com.fasterxml.jackson.databind.ObjectMapper()
            .registerModule(com.fasterxml.jackson.module.kotlin.kotlinModule())
        
        objectMapper.writerWithDefaultPrettyPrinter()
            .writeValue(domainAnalysisFile, domainAnalysisResult)
    }
    
    private fun createMockClassFiles() {
        // 创建模拟的Goods类文件
        val goodsClassFile = File(compilationOutputDir, "org/morecup/pragmaddd/jimmer/domain/goods/Goods.class")
        goodsClassFile.parentFile.mkdirs()
        goodsClassFile.writeText("mock Goods class content")
        
        // 创建模拟的ChangeAddressCmdHandler类文件
        val handlerClassFile = File(compilationOutputDir, "org/morecup/pragmaddd/jimmer/domain/goods/ChangeAddressCmdHandler.class")
        handlerClassFile.parentFile.mkdirs()
        handlerClassFile.writeText("mock handler class content")
        
        // 创建模拟的GoodsRepository类文件
        val repositoryClassFile = File(compilationOutputDir, "org/morecup/pragmaddd/jimmer/domain/goods/GoodsRepository.class")
        repositoryClassFile.parentFile.mkdirs()
        repositoryClassFile.writeText("mock repository class content")
    }
    
    private fun createMockRepositoryClass(fileName: String): File {
        val file = File(compilationOutputDir, "org/morecup/pragmaddd/jimmer/domain/goods/$fileName")
        file.parentFile.mkdirs()
        file.writeText("mock repository class content")
        return file
    }
    
    private fun loadDomainAnalysisResult(): Map<String, DetailedClassInfo> {
        val objectMapper = com.fasterxml.jackson.databind.ObjectMapper()
            .registerModule(com.fasterxml.jackson.module.kotlin.kotlinModule())
        
        val result: DetailedAnalysisResult = objectMapper.readValue(domainAnalysisFile, DetailedAnalysisResult::class.java)
        return result.classes.associateBy { it.className }
    }
    
    private fun createMockGoodsClassInfo(): DetailedClassInfo {
        return DetailedClassInfo(
            className = "org.morecup.pragmaddd.jimmer.domain.goods.Goods",
            simpleName = "Goods",
            packageName = "org.morecup.pragmaddd.jimmer.domain.goods",
            modifiers = ModifierInfo(access = 1),
            domainObjectType = DomainObjectType.AGGREGATE_ROOT,
            fields = listOf(
                DetailedFieldInfo(
                    name = "name",
                    descriptor = "Ljava/lang/String;",
                    modifiers = ModifierInfo(access = 1)
                ),
                DetailedFieldInfo(
                    name = "nowAddress1",
                    descriptor = "Ljava/lang/String;",
                    modifiers = ModifierInfo(access = 2) // private
                ),
                DetailedFieldInfo(
                    name = "id",
                    descriptor = "Ljava/lang/Long;",
                    modifiers = ModifierInfo(access = 1)
                )
            ),
            methods = listOf(
                DetailedMethodInfo(
                    name = "changeAddress",
                    descriptor = "(Ljava/lang/String;)V",
                    modifiers = ModifierInfo(access = 1),
                    accessedProperties = setOf("nowAddress1", "name")
                )
            ),
            propertyAccessAnalysis = listOf(
                PropertyAccessInfo(
                    className = "org.morecup.pragmaddd.jimmer.domain.goods.Goods",
                    methodName = "changeAddress",
                    methodDescriptor = "(Ljava/lang/String;)V",
                    accessedProperties = setOf("nowAddress1", "name"),
                    modifiedProperties = setOf("nowAddress1"),
                    calledMethods = emptySet()
                )
            )
        )
    }
}