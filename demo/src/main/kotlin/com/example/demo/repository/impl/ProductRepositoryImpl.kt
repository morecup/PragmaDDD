package com.example.demo.repository.impl

import com.example.demo.domain.Product
import com.example.demo.repository.ProductRepository
import org.morecup.pragmaddd.core.callanalysis.CompileTimeAnalysisUtils

/**
 * 产品Repository实现 - 演示编译期调用分析的使用
 */
class ProductRepositoryImpl : ProductRepository {
    
    override fun findByIdOrErr(id: String): Product {
        // 使用编译期分析结果获取需要的字段
        val requiredFields = CompileTimeAnalysisUtils.getRequiredFields(
            aggregateRootClassName = Product::class.java.name
        )
        
        println("需要加载的Product字段: $requiredFields")
        
        // 基于 requiredFields 构建优化的 fetcher
        // 这里只是示例，实际实现会根据ORM框架构建相应的查询
        return buildOptimizedProduct(id, requiredFields)
    }
    
    override fun save(product: Product) {
        // 保存逻辑
        println("保存Product: ${product.productId}")
    }
    
    /**
     * 根据需要的字段构建优化的Product对象
     */
    private fun buildOptimizedProduct(id: String, requiredFields: Set<String>): Product {
        // 模拟根据字段需求优化数据库查询
        println("优化查询 - 只加载字段: $requiredFields")
        
        // 这里是模拟数据，实际应该从数据库加载
        return Product(
            productId = id,
            name = "示例产品",
            price = java.math.BigDecimal("99.99"),
            stock = 100,
            category = "电子产品"
        )
    }
}