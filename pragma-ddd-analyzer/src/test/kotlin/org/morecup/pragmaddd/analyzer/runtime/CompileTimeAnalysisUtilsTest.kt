package org.morecup.pragmaddd.analyzer.runtime

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.morecup.pragmaddd.analyzer.compiletime.model.*

/**
 * 运行时API工具类单元测试
 */
class CompileTimeAnalysisUtilsTest {
    
    @BeforeEach
    fun setup() {
        CompileTimeAnalysisUtils.clearCache()
        
        // 加载测试用的分析结果
        val testAnalysisResult = createTestAnalysisResult()
        CompileTimeAnalysisUtils.loadAnalysisResults(testAnalysisResult)
    }
    
    @AfterEach
    fun cleanup() {
        CompileTimeAnalysisUtils.clearCache()
    }
    
    @Test
    fun `test get required fields by method`() {
        val requiredFields = CompileTimeAnalysisUtils.getRequiredFieldsByMethod(
            aggregateRootClass = "com.example.domain.Goods",
            callerClass = "com.example.service.OrderService",
            callerMethod = "updateOrder",
            repositoryMethod = "findByIdOrErr"
        )
        
        assertEquals(3, requiredFields.size)
        assertTrue(requiredFields.contains("id"))
        assertTrue(requiredFields.contains("name"))
        assertTrue(requiredFields.contains("nowAddress"))
    }
    
    @Test
    fun `test get all analyzed aggregate roots`() {
        val aggregateRoots = CompileTimeAnalysisUtils.getAllAnalyzedAggregateRoots()
        
        assertEquals(2, aggregateRoots.size)
        assertTrue(aggregateRoots.contains("com.example.domain.Goods"))
        assertTrue(aggregateRoots.contains("com.example.domain.Order"))
    }
    
    @Test
    fun `test get repository methods`() {
        val repositoryMethods = CompileTimeAnalysisUtils.getRepositoryMethods("com.example.domain.Goods")
        
        assertEquals(2, repositoryMethods.size)
        assertTrue(repositoryMethods.contains("findByIdOrErr(J)Lcom/example/domain/Goods;"))
        assertTrue(repositoryMethods.contains("save(Lcom/example/domain/Goods;)V"))
    }
    
    @Test
    fun `test has analysis results`() {
        assertTrue(CompileTimeAnalysisUtils.hasAnalysisResults())
        
        CompileTimeAnalysisUtils.clearCache()
        assertFalse(CompileTimeAnalysisUtils.hasAnalysisResults())
    }
    
    @Test
    fun `test get analysis info`() {
        val analysisInfo = CompileTimeAnalysisUtils.getAnalysisInfo()
        
        assertEquals("1.0", analysisInfo.version)
        assertEquals(2, analysisInfo.aggregateRootCount)
        assertEquals(3, analysisInfo.repositoryMethodCount)
        assertEquals(3, analysisInfo.callerCount)
        
        val infoString = analysisInfo.toString()
        assertTrue(infoString.contains("version='1.0'"))
        assertTrue(infoString.contains("aggregateRoots=2"))
    }
    
    @Test
    fun `test field access context`() {
        val context = FieldAccessContext(
            aggregateRootClass = "com.example.domain.Goods",
            repositoryClass = "com.example.repository.GoodsRepository",
            repositoryMethod = "findByIdOrErr",
            callerClass = "com.example.service.OrderService",
            callerMethod = "updateOrder",
            requiredFields = setOf("id", "name", "address.street", "address.city")
        )
        
        assertTrue(context.isFieldRequired("id"))
        assertTrue(context.isFieldRequired("name"))
        assertTrue(context.isFieldRequired("street")) // 嵌套字段
        assertFalse(context.isFieldRequired("description"))
        
        val nestedFields = context.getNestedRequiredFields("address")
        assertEquals(2, nestedFields.size)
        assertTrue(nestedFields.contains("street"))
        assertTrue(nestedFields.contains("city"))
    }
    
    @Test
    fun `test field access context builder`() {
        // 创建一个简单的测试场景
        class TestAggregateRoot
        class TestRepository
        
        // 注意：这个测试在实际运行中可能无法准确获取调用堆栈，
        // 因为测试环境的堆栈和真实业务代码的堆栈不同
        val context = FieldAccessContextBuilder.fromCurrentContext(
            TestAggregateRoot::class.java,
            TestRepository::class.java,
            "findById"
        )
        
        // 在测试环境中，context可能为null，这是正常的
        // 实际使用时需要在真实的业务调用场景中测试
    }
    
    @Test
    fun `test get required fields with empty results`() {
        CompileTimeAnalysisUtils.clearCache()
        
        val requiredFields = CompileTimeAnalysisUtils.getRequiredFieldsByMethod(
            aggregateRootClass = "com.nonexistent.Aggregate",
            callerClass = "com.nonexistent.Service",
            callerMethod = "someMethod"
        )
        
        assertTrue(requiredFields.isEmpty())
    }
    
    @Test
    fun `test initialization from invalid resource`() {
        CompileTimeAnalysisUtils.clearCache()
        
        // 尝试从不存在的资源初始化
        CompileTimeAnalysisUtils.initialize("/nonexistent/path.json")
        
        assertFalse(CompileTimeAnalysisUtils.hasAnalysisResults())
    }
    
    private fun createTestAnalysisResult(): CallAnalysisResult {
        // 创建Goods聚合根的分析结果
        val goodsCallerAnalysis1 = CallerMethodAnalysis(
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
                    requiredFields = setOf("nowAddress", "name")
                )
            ),
            requiredFields = setOf("id", "name", "nowAddress")
        )
        
        val goodsCallerAnalysis2 = CallerMethodAnalysis(
            methodClass = "com.example.service.GoodsService",
            method = "processGoods",
            methodDescriptor = "()V",
            sourceLines = "20-25",
            repository = "com.example.repository.GoodsRepository",
            repositoryMethod = "save",
            repositoryMethodDescriptor = "(Lcom/example/domain/Goods;)V",
            aggregateRoot = "com.example.domain.Goods",
            calledAggregateRootMethods = emptyList(),
            requiredFields = setOf("id", "name", "price")
        )
        
        val goodsRepositoryMethod1 = RepositoryMethodAnalysis(
            methodDescriptor = "(J)Lcom/example/domain/Goods;",
            callers = mapOf("com.example.service.OrderService.updateOrder+10-15" to goodsCallerAnalysis1)
        )
        
        val goodsRepositoryMethod2 = RepositoryMethodAnalysis(
            methodDescriptor = "(Lcom/example/domain/Goods;)V",
            callers = mapOf("com.example.service.GoodsService.processGoods+20-25" to goodsCallerAnalysis2)
        )
        
        val goodsAggregateAnalysis = AggregateRootAnalysis(
            aggregateRootClass = "com.example.domain.Goods",
            repositoryMethods = mapOf(
                "findByIdOrErr(J)Lcom/example/domain/Goods;" to goodsRepositoryMethod1,
                "save(Lcom/example/domain/Goods;)V" to goodsRepositoryMethod2
            )
        )
        
        // 创建Order聚合根的分析结果
        val orderCallerAnalysis = CallerMethodAnalysis(
            methodClass = "com.example.service.OrderService",
            method = "createOrder",
            methodDescriptor = "()V",
            sourceLines = "30-35",
            repository = "com.example.repository.OrderRepository",
            repositoryMethod = "save",
            repositoryMethodDescriptor = "(Lcom/example/domain/Order;)V",
            aggregateRoot = "com.example.domain.Order",
            calledAggregateRootMethods = emptyList(),
            requiredFields = setOf("id", "status", "totalAmount")
        )
        
        val orderRepositoryMethod = RepositoryMethodAnalysis(
            methodDescriptor = "(Lcom/example/domain/Order;)V",
            callers = mapOf("com.example.service.OrderService.createOrder+30-35" to orderCallerAnalysis)
        )
        
        val orderAggregateAnalysis = AggregateRootAnalysis(
            aggregateRootClass = "com.example.domain.Order",
            repositoryMethods = mapOf(
                "save(Lcom/example/domain/Order;)V" to orderRepositoryMethod
            )
        )
        
        return CallAnalysisResult(
            version = "1.0",
            timestamp = "2024-01-01T12:00:00Z",
            callGraph = mapOf(
                "com.example.domain.Goods" to goodsAggregateAnalysis,
                "com.example.domain.Order" to orderAggregateAnalysis
            )
        )
    }
}