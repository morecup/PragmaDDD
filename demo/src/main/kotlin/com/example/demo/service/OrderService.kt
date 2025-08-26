package com.example.demo.service

import com.example.demo.domain.Order
import com.example.demo.domain.Product
import com.example.demo.repository.OrderRepository
import com.example.demo.repository.ProductRepository

/**
 * 订单服务 - 用于测试编译期调用分析
 */
class OrderService(
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository
) {
    
    /**
     * 处理订单 - 测试Repository调用和聚合根方法调用
     */
    fun processOrder(orderId: String) {
        // 调用Repository方法获取订单
        val order = orderRepository.findByIdOrErr(orderId)
        
        // 调用聚合根方法
        order.testFieldDomainEntity()
        order.testLambda()
        
        // 保存订单
        orderRepository.save(order)
    }
    
    /**
     * 更新产品价格 - 测试产品聚合根的字段访问
     */
    fun updateProductPrice(productId: String, newPrice: java.math.BigDecimal) {
        val product = productRepository.findByIdOrErr(productId)
        
        // 调用聚合根方法，会访问price字段
        product.updatePrice(newPrice)
        
        productRepository.save(product)
    }
    
    /**
     * 检查库存 - 测试只读访问
     */
    fun checkStock(productId: String): Boolean {
        val product = productRepository.findByIdOrErr(productId)
        
        // 只访问stock字段
        return product.hasStock()
    }
    
    /**
     * 复杂业务逻辑 - 测试多个聚合根交互
     */
    fun complexBusinessLogic(orderId: String, productId: String) {
        val order = orderRepository.findByIdOrErr(orderId)
        val product = productRepository.findByIdOrErr(productId)
        
        // 多个方法调用，访问不同字段
        if (product.hasEnoughStock(5)) {
            product.reduceStock(5)
            order.testEmpty() // 空方法调用
        }
        
        orderRepository.save(order)
        productRepository.save(product)
    }
}