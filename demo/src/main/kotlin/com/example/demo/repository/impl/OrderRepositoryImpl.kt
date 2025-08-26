package com.example.demo.repository.impl

import com.example.demo.domain.Order
import com.example.demo.domain.Product
import com.example.demo.repository.OrderRepository
import org.morecup.pragmaddd.core.callanalysis.CompileTimeAnalysisUtils

/**
 * 订单Repository实现 - 演示编译期调用分析的使用
 */
class OrderRepositoryImpl : OrderRepository {
    
    override fun findByIdOrErr(id: String): Order {
        // 使用编译期分析结果获取需要的字段，提供调用方信息以获得更精确的结果
        val stackTrace = Thread.currentThread().stackTrace
        val callerClass = if (stackTrace.size > 2) stackTrace[2].className else null
        val callerMethod = if (stackTrace.size > 2) stackTrace[2].methodName else null
        
        val requiredFields = CompileTimeAnalysisUtils.getRequiredFields(
            aggregateRootClassName = Order::class.java.name,
            callerClassName = callerClass,
            callerMethodName = callerMethod
        )
        
        println("调用方: $callerClass.$callerMethod")
        println("需要加载的Order字段: $requiredFields")
        
        // 基于 requiredFields 构建优化的 fetcher
        return buildOptimizedOrder(id, requiredFields)
    }
    
    override fun save(order: Order) {
        // 保存逻辑
        println("保存Order: ${order.customerId}")
    }
    
    /**
     * 根据需要的字段构建优化的Order对象
     */
    private fun buildOptimizedOrder(id: String, requiredFields: Set<String>): Order {
        // 模拟根据字段需求优化数据库查询
        println("优化查询 - 只加载字段: $requiredFields")
        
        // 这里是模拟数据，实际应该从数据库加载
        val product = Product(
            productId = "P001",
            name = "示例产品",
            price = java.math.BigDecimal("99.99"),
            stock = 100,
            category = "电子产品"
        )
        
        return Order(
            orderIds = mutableListOf("O001", "O002"),
            customerId = "C001",
            totalAmount = java.math.BigDecimal("199.98"),
            product = product
        )
    }
}