package com.example.demo

import com.example.demo.repository.impl.OrderRepositoryImpl
import com.example.demo.repository.impl.ProductRepositoryImpl
import com.example.demo.service.OrderService

/**
 * 演示应用 - 测试编译期调用分析功能
 */
fun main() {
    println("=== 编译期调用分析演示 ===")
    
    // 创建Repository实现
    val orderRepository = OrderRepositoryImpl()
    val productRepository = ProductRepositoryImpl()
    
    // 创建服务
    val orderService = OrderService(orderRepository, productRepository)
    
    println("\n1. 测试processOrder方法:")
    orderService.processOrder("ORDER-001")
    
    println("\n2. 测试updateProductPrice方法:")
    orderService.updateProductPrice("PRODUCT-001", java.math.BigDecimal("129.99"))
    
    println("\n3. 测试checkStock方法:")
    val hasStock = orderService.checkStock("PRODUCT-002")
    println("库存检查结果: $hasStock")
    
    println("\n4. 测试complexBusinessLogic方法:")
    orderService.complexBusinessLogic("ORDER-002", "PRODUCT-003")
    
    println("\n=== 演示完成 ===")
}