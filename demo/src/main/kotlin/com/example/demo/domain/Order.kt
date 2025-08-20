package com.example.demo.domain

import org.morecup.pragmaddd.core.annotation.AggregateRoot
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 订单聚合根示例
 */
@AggregateRoot
class Order(
    private var orderId: String,
    private var customerId: String,
    private var totalAmount: BigDecimal,
    private var status: OrderStatus = OrderStatus.PENDING,
    private var createdAt: LocalDateTime = LocalDateTime.now(),
    private var items: MutableList<OrderItem> = mutableListOf()
) {
    
    enum class OrderStatus {
        PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED
    }
    
    data class OrderItem(
        val productId: String,
        val quantity: Int,
        val unitPrice: BigDecimal
    )
    
    fun getOrderId(): String = orderId
    
    fun getCustomerId(): String = customerId
    
    fun getTotalAmount(): BigDecimal = totalAmount
    
    fun getStatus(): OrderStatus = status
    
    fun getCreatedAt(): LocalDateTime = createdAt
    
    fun getItems(): List<OrderItem> = items.toList()
    
    /**
     * 添加订单项 - 修改items和totalAmount
     */
    fun addItem(productId: String, quantity: Int, unitPrice: BigDecimal) {
        val item = OrderItem(productId, quantity, unitPrice)
        items.add(item)
        
        // 重新计算总金额
        recalculateTotal()
    }
    
    /**
     * 移除订单项 - 修改items和totalAmount
     */
    fun removeItem(productId: String) {
        items.removeIf { it.productId == productId }
        recalculateTotal()
    }
    
    /**
     * 确认订单 - 访问和修改状态
     */
    fun confirm() {
        if (status == OrderStatus.PENDING && items.isNotEmpty()) {
            status = OrderStatus.CONFIRMED
        }
    }
    
    /**
     * 发货 - 访问和修改状态
     */
    fun ship() {
        if (status == OrderStatus.CONFIRMED) {
            status = OrderStatus.SHIPPED
        }
    }
    
    /**
     * 取消订单 - 访问和修改状态
     */
    fun cancel() {
        if (status == OrderStatus.PENDING || status == OrderStatus.CONFIRMED) {
            status = OrderStatus.CANCELLED
        }
    }
    
    /**
     * 重新计算总金额 - 访问items，修改totalAmount
     */
    private fun recalculateTotal() {
        totalAmount = items.sumOf { it.unitPrice.multiply(BigDecimal(it.quantity)) }
    }
    
    /**
     * 检查是否可以修改 - 只访问状态属性
     */
    fun canModify(): Boolean {
        return status == OrderStatus.PENDING
    }
    
    /**
     * 获取订单摘要 - 访问多个属性
     */
    fun getSummary(): String {
        return "Order(id=$orderId, customer=$customerId, total=$totalAmount, status=$status, itemCount=${items.size})"
    }
}