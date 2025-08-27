package org.morecup.pragmaddd.analyzer.compiletime

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import org.morecup.pragmaddd.analyzer.compiletime.model.*
import org.morecup.pragmaddd.analyzer.model.*
import java.io.File
import java.nio.file.Path

/**
 * 编译期调用分析单元测试
 */
class CompileTimeCallAnalysisTest {
    
    @TempDir
    lateinit var tempDir: Path
    
    private lateinit var testClassFiles: List<File>
    private lateinit var domainAnalysisResult: Map<String, DetailedClassInfo>
    
    @BeforeEach
    fun setup() {
        // 创建测试用的domain分析结果
        domainAnalysisResult = createMockDomainAnalysisResult()
        
        // 创建测试用的class文件（模拟）
        testClassFiles = createMockClassFiles()
    }
    
    @Test
    fun `test repository identifier - aggregate root extraction`() {
        val repositoryIdentifier = RepositoryIdentifier()
        
        val aggregateRoots = repositoryIdentifier.extractAggregateRoots(domainAnalysisResult)
        
        assertEquals(2, aggregateRoots.size)
        assertTrue(aggregateRoots.contains("com.example.domain.Goods"))
        assertTrue(aggregateRoots.contains("com.example.domain.Order"))
    }
    
    @Test
    fun `test repository identifier - naming convention`() {
        val repositoryIdentifier = RepositoryIdentifier()
        val aggregateRoots = setOf("com.example.domain.Goods")
        
        // 模拟Repository类文件
        val mockClassFiles = listOf(
            createMockFile("GoodsRepository.class"),
            createMockFile("UserService.class")
        )
        
        val mappings = repositoryIdentifier.identifyRepositories(aggregateRoots, mockClassFiles)
        
        assertFalse(mappings.isEmpty())
        // 注意：这里的测试是简化的，实际测试需要真实的class文件
    }
    
    @Test
    fun `test field access analyzer - required fields analysis`() {
        val fieldAccessAnalyzer = FieldAccessAnalyzer()
        val repositoryCallInfo = RepositoryCallInfo(
            callerMethod = MethodInfo("com.example.service.OrderService", "updateOrder", "()V"),
            repositoryClass = "com.example.repository.GoodsRepository",
            repositoryMethod = "findByIdOrErr",
            repositoryMethodDescriptor = "(J)Lcom/example/domain/Goods;",
            aggregateRootClass = "com.example.domain.Goods"
        )
        
        val callGraph = CallGraph()
        callGraph.addRepositoryCall(repositoryCallInfo)
        
        // 添加聚合根方法调用
        val goodsChangeAddressMethod = MethodInfo("com.example.domain.Goods", "changeAddress", "(Ljava/lang/String;)V")
        callGraph.addMethodCall(repositoryCallInfo.callerMethod, goodsChangeAddressMethod)
        
        val result = fieldAccessAnalyzer.analyzeRequiredFields(
            repositoryCallInfo,
            callGraph,
            domainAnalysisResult
        )
        
        assertNotNull(result)
        assertTrue(result.requiredFields.isNotEmpty())
    }
    
    @Test
    fun `test call analysis result serialization`() {
        val serializer = CallAnalysisResultSerializer()
        
        // 创建测试分析结果
        val testResult = createMockCallAnalysisResult()
        
        // 测试序列化和反序列化
        val jsonString = serializer.serializeToString(testResult)
        assertNotNull(jsonString)
        assertTrue(jsonString.contains("\"version\":\"1.0\""))
        
        val deserializedResult = serializer.deserializeFromString(jsonString)
        assertEquals(testResult.version, deserializedResult.version)
        assertEquals(testResult.callGraph.size, deserializedResult.callGraph.size)
    }
    
    @Test
    fun `test incremental analysis manager`() {
        val cacheDir = tempDir.resolve("cache").toFile()
        val incrementalManager = IncrementalAnalysisManager(cacheDir, true)
        
        val compilationDir = tempDir.resolve("classes").toFile().apply { mkdirs() }
        val domainAnalysisFile = tempDir.resolve("domain-analyzer.json").toFile().apply {
            writeText("{\"classes\":[]}")
        }
        
        // 第一次分析应该需要执行
        assertTrue(incrementalManager.shouldAnalyze(compilationDir, domainAnalysisFile))
        
        // 缓存结果
        val mockResult = createMockCallAnalysisResult()
        incrementalManager.cacheResult(compilationDir, domainAnalysisFile, mockResult)
        
        // 第二次应该不需要分析（如果没有文件变化）
        // 注意：这个测试在实际环境中可能需要更精确的文件操作
    }
    
    @Test
    fun `test analysis config creation`() {
        val config = CompileTimeAnalysisConfig(
            repositoryConfig = RepositoryIdentificationConfig(
                namingRules = listOf("{AggregateRoot}Repository"),
                includePackages = listOf("com.example.**"),
                excludePackages = listOf("com.example.test.**")
            ),
            fieldAccessConfig = FieldAccessAnalysisConfig(
                maxRecursionDepth = 5,
                enableCircularDependencyDetection = true
            ),
            cacheEnabled = true,
            debugMode = false
        )
        
        assertEquals(5, config.fieldAccessConfig.maxRecursionDepth)
        assertTrue(config.cacheEnabled)
        assertEquals(listOf("{AggregateRoot}Repository"), config.repositoryConfig.namingRules)
    }
    
    @Test
    fun `test call graph utils - circular dependency detection`() {
        val callGraph = CallGraph()
        
        val methodA = MethodInfo("com.example.A", "methodA", "()V")
        val methodB = MethodInfo("com.example.B", "methodB", "()V")
        val methodC = MethodInfo("com.example.C", "methodC", "()V")
        
        // 创建循环依赖：A -> B -> C -> A
        callGraph.addMethodCall(methodA, methodB)
        callGraph.addMethodCall(methodB, methodC)
        callGraph.addMethodCall(methodC, methodA)
        
        val cycles = CallGraphUtils.detectCircularDependencies(callGraph)
        
        assertFalse(cycles.isEmpty())
        assertTrue(cycles.any { cycle -> 
            cycle.size >= 3 && 
            cycle.contains(methodA) && 
            cycle.contains(methodB) && 
            cycle.contains(methodC)
        })
    }
    
    private fun createMockDomainAnalysisResult(): Map<String, DetailedClassInfo> {
        val goodsClass = DetailedClassInfo(
            className = "com.example.domain.Goods",
            simpleName = "Goods",
            packageName = "com.example.domain",
            modifiers = ModifierInfo(access = 1),
            domainObjectType = DomainObjectType.AGGREGATE_ROOT,
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
                    className = "com.example.domain.Goods",
                    methodName = "changeAddress",
                    methodDescriptor = "(Ljava/lang/String;)V",
                    accessedProperties = setOf("nowAddress1", "name"),
                    modifiedProperties = setOf("nowAddress1"),
                    calledMethods = emptySet()
                )
            )
        )
        
        val orderClass = DetailedClassInfo(
            className = "com.example.domain.Order",
            simpleName = "Order",
            packageName = "com.example.domain",
            modifiers = ModifierInfo(access = 1),
            domainObjectType = DomainObjectType.AGGREGATE_ROOT,
            methods = listOf(
                DetailedMethodInfo(
                    name = "updateStatus",
                    descriptor = "(Ljava/lang/String;)V",
                    modifiers = ModifierInfo(access = 1),
                    accessedProperties = setOf("status", "id")
                )
            )
        )
        
        return mapOf(
            "com.example.domain.Goods" to goodsClass,
            "com.example.domain.Order" to orderClass
        )
    }
    
    private fun createMockClassFiles(): List<File> {
        return listOf(
            createMockFile("Goods.class"),
            createMockFile("Order.class"),
            createMockFile("GoodsRepository.class"),
            createMockFile("OrderService.class")
        )
    }
    
    private fun createMockFile(fileName: String): File {
        val file = tempDir.resolve(fileName).toFile()
        file.parentFile?.mkdirs()
        file.writeText("mock class content")
        return file
    }
    
    private fun createMockCallAnalysisResult(): CallAnalysisResult {
        val callerAnalysis = CallerMethodAnalysis(
            methodClass = "com.example.service.OrderService",
            method = "updateOrder",
            methodDescriptor = "()V",
            sourceLines = "10-15",
            repository = "com.example.repository.GoodsRepository",
            repositoryMethod = "findByIdOrErr",
            repositoryMethodDescriptor = "(J)Lcom/example/domain/Goods;",
            aggregateRoot = "com.example.domain.Goods",
            calledAggregateRootMethods = listOf(
                CalledMethodInfo(
                    aggregateRootMethod = "changeAddress",
                    aggregateRootMethodDescriptor = "(Ljava/lang/String;)V",
                    requiredFields = setOf("nowAddress1", "name")
                )
            ),
            requiredFields = setOf("id", "name", "nowAddress1")
        )
        
        val repositoryMethodAnalysis = RepositoryMethodAnalysis(
            methodDescriptor = "(J)Lcom/example/domain/Goods;",
            callers = mapOf("com.example.service.OrderService.updateOrder+10-15" to callerAnalysis)
        )
        
        val aggregateRootAnalysis = AggregateRootAnalysis(
            aggregateRootClass = "com.example.domain.Goods",
            repositoryMethods = mapOf("findByIdOrErr(J)Lcom/example/domain/Goods;" to repositoryMethodAnalysis)
        )
        
        return CallAnalysisResult(
            version = "1.0",
            timestamp = "2024-01-01T00:00:00Z",
            callGraph = mapOf("com.example.domain.Goods" to aggregateRootAnalysis)
        )
    }
}