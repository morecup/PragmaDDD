package com.example.demo.domain

import org.morecup.pragmaddd.core.annotation.AggregateRoot
import java.math.BigDecimal

/**
 * 产品聚合根示例 - 演示简单的属性访问模式
 */
@AggregateRoot
class Product(
    @field: JvmField
    var productId: String,
    var name: String,
    private var price: BigDecimal,
    private var stock: Int,
    private var category: String
) {
    
    /**
     * 更新价格 - 简单的属性修改
     */
    fun updatePrice(newPrice: BigDecimal) {
        this.price = newPrice
    }
    
    /**
     * 减少库存 - 访问和修改库存
     */
    fun reduceStock(quantity: Int) {
        if (this.stock >= quantity) {
            this.stock = this.stock - quantity
        }
    }
    
    /**
     * 增加库存 - 访问和修改库存
     */
    fun addStock(quantity: Int) {
        this.stock = this.stock + quantity
    }
    
    /**
     * 检查是否有库存 - 只访问库存属性
     */
    fun hasStock(): Boolean {
        return this.stock > 0
    }
    
    /**
     * 检查库存是否充足 - 只访问库存属性
     */
    fun hasEnoughStock(requiredQuantity: Int): Boolean {
        return this.stock >= requiredQuantity
    }
}