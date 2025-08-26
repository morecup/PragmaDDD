package com.example.demo.repository

import com.example.demo.domain.Order
import org.morecup.pragmaddd.core.repository.DomainRepository

/**
 * 订单Repository - 继承DomainRepository接口
 */
interface OrderRepository : DomainRepository<Order> {
    
    /**
     * 根据ID查找订单，如果不存在则抛出异常
     */
    fun findByIdOrErr(id: String): Order
    
    /**
     * 保存订单
     */
    fun save(order: Order)
}