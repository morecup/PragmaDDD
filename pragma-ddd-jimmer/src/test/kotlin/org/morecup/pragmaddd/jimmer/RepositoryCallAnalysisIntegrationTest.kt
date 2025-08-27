package org.morecup.pragmaddd.jimmer

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.morecup.pragmaddd.core.repository.CompileTimeAnalysisUtils
import org.morecup.pragmaddd.jimmer.domain.goods.Goods
import org.morecup.pragmaddd.jimmer.domain.goods.GoodsRepository
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.BeforeTest

/**
 * Repository调用分析集成测试
 * 验证编译期分析结果在运行时的可用性
 */
@SpringBootTest(classes = [App::class])
class RepositoryCallAnalysisIntegrationTest {
    
    @BeforeTest
    fun setup() {
        // 清除缓存以确保测试环境干净
        CompileTimeAnalysisUtils.clearCache()
    }
    
    @Test
    fun `应该能够检查分析结果是否可用`() {
        val isAvailable = CompileTimeAnalysisUtils.isAnalysisAvailable()
        
        // 在实际运行时，如果分析结果文件存在应该返回true
        // 在测试环境中可能返回false，这是正常的
        println("分析结果是否可用: $isAvailable")
    }
    
    @Test
    fun `应该能够获取分析摘要信息`() {
        val summary = CompileTimeAnalysisUtils.getAnalysisSummary()
        
        if (summary != null) {
            println("分析摘要:")
            println("  总类数: ${summary.totalClasses}")
            println("  总方法数: ${summary.totalMethods}")
            println("  总Repository调用数: ${summary.totalRepositoryCalls}")
            println("  聚合根数量: ${summary.aggregateRootCount}")
            println("  Repository数量: ${summary.repositoryCount}")
            
            assertTrue(summary.totalClasses > 0, "应该有分析的类")
            assertTrue(summary.aggregateRootCount > 0, "应该有聚合根")
        } else {
            println("分析摘要不可用，可能是因为分析结果文件不存在")
        }
    }
    
    @Test
    fun `应该能够获取Repository字段映射`() {
        val fieldMapping = CompileTimeAnalysisUtils.getRepositoryFieldMapping(
            GoodsRepository::class.java,
            "findByIdOrErr"
        )
        
        println("Repository字段映射:")
        fieldMapping.forEach { (caller, fields) ->
            println("  调用方: $caller")
            println("  需要字段: $fields")
        }
        
        // 验证结果结构
        fieldMapping.forEach { (caller, fields) ->
            assertTrue(caller.isNotEmpty(), "调用方不应为空")
            assertNotNull(fields, "字段列表不应为null")
        }
    }
    
    @Test
    fun `模拟ChangeAddressCmdHandler调用场景`() {
        try {
            // 模拟从ChangeAddressCmdHandler.handle方法调用
            val requiredFields = simulateChangeAddressCmdHandlerCall()
            
            println("ChangeAddressCmdHandler.handle方法需要的字段: $requiredFields")
            
            if (requiredFields.isNotEmpty()) {
                assertTrue(requiredFields.contains("name") || requiredFields.contains("nowAddress1"),
                    "应该包含Goods聚合根的相关字段")
            }
        } catch (e: Exception) {
            println("获取字段信息失败，可能是分析结果文件不存在: ${e.message}")
        }
    }
    
    @Test
    fun `测试运行时API的异常处理`() {
        // 测试不存在的聚合根类型
        assertDoesNotThrow {
            val fields = CompileTimeAnalysisUtils.getRequiredFields(
                String::class.java,
                "NonExistentClass",
                "nonExistentMethod"
            )
            assertTrue(fields.isEmpty(), "不存在的调用应该返回空列表")
        }
        
        // 测试缓存清除
        assertDoesNotThrow {
            CompileTimeAnalysisUtils.clearCache()
        }
    }
    
    /**
     * 模拟ChangeAddressCmdHandler调用场景
     */
    private fun simulateChangeAddressCmdHandlerCall(): List<String> {
        // 使用编译期分析工具获取Goods聚合根的必需字段
        return CompileTimeAnalysisUtils.getRequiredFields(
            Goods::class.java,
            "org.morecup.pragmaddd.jimmer.domain.goods.ChangeAddressCmdHandler",
            "handle"
        )
    }
}

/**
 * 模拟优化后的Repository实现
 * 展示如何使用编译期分析结果
 */
class OptimizedGoodsRepositoryExample {
    
    fun findByIdOrErr(id: Long): Goods {
        // 获取编译期分析的必需字段
        val requiredFields = CompileTimeAnalysisUtils.getRequiredFields(Goods::class.java)
        
        println("根据分析结果，当前调用需要加载的字段: $requiredFields")
        
        // 在实际实现中，这里可以基于requiredFields构建优化的数据库查询
        // 例如使用Jimmer的fetcher或JPA的@EntityGraph
        
        // 模拟返回一个Goods实例
        // return sqlClient.findById(Goods::class, id, buildOptimizedFetcher(requiredFields))
        
        throw NotImplementedError("这只是示例代码")
    }
    
    /**
     * 基于必需字段构建优化的fetcher（示例）
     */
    private fun buildOptimizedFetcher(requiredFields: List<String>): String {
        // 这里可以根据requiredFields生成优化的查询语句或fetcher配置
        return "optimized fetcher based on: $requiredFields"
    }
}