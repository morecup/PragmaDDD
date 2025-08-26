package com.example.demo.repository

import com.example.demo.domain.Product

/**
 * 产品Repository - 使用命名约定识别
 */
interface ProductRepository {
    
    /**
     * 根据ID查找产品，如果不存在则抛出异常
     */
    fun findByIdOrErr(id: String): Product
    
    /**
     * 保存产品
     */
    fun save(product: Product)
}