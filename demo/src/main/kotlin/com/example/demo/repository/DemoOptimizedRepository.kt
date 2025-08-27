package com.example.demo.repository

import com.example.demo.domain.Order
import org.morecup.pragmaddd.core.repository.CompileTimeAnalysisUtils

/**
 * 演示如何使用编译期分析结果优化Repository
 */
class DemoOptimizedRepository {
    
    /**
     * 优化后的查找方法
     * 使用编译期分析结果来确定需要加载的字段
     */
    fun findByIdOptimized(id: String): Order? {
        return try {
            // 获取编译期分析的必需字段
            val requiredFields = CompileTimeAnalysisUtils.getRequiredFields(Order::class.java)
            
            println("[DemoOptimizedRepository] 根据编译期分析，需要加载的字段: $requiredFields")
            
            // 在实际实现中，这里可以基于requiredFields构建优化的查询
            // 例如：
            // - 使用JPA的@EntityGraph
            // - 使用Hibernate的FetchProfile  
            // - 使用MyBatis的ResultMap
            // - 使用Jimmer的Fetcher
            
            buildOptimizedQuery(requiredFields)
            
            // 模拟返回结果
            null // 实际实现中返回查询结果
            
        } catch (e: Exception) {
            println("[DemoOptimizedRepository] 编译期分析结果不可用，使用默认查询: ${e.message}")
            
            // 回退到传统的查询方式
            findByIdTraditional(id)
        }
    }
    
    /**
     * 传统的查找方法（作为对比和回退方案）
     */
    fun findByIdTraditional(id: String): Order? {
        println("[DemoOptimizedRepository] 使用传统方式查询，加载所有字段")
        
        // 传统方式：加载所有字段
        // return entityManager.find(Order::class.java, id)
        
        return null // 模拟实现
    }
    
    /**
     * 基于必需字段构建优化查询
     */
    private fun buildOptimizedQuery(requiredFields: List<String>): String {
        if (requiredFields.isEmpty()) {
            return "SELECT * FROM orders WHERE id = ?"
        }
        
        // 构建只包含必需字段的查询
        val fieldList = requiredFields.joinToString(", ")
        val optimizedQuery = "SELECT id, $fieldList FROM orders WHERE id = ?"
        
        println("[DemoOptimizedRepository] 优化后的查询: $optimizedQuery")
        
        return optimizedQuery
    }
    
    /**
     * 展示Repository字段映射的使用
     */
    fun getFieldMappingExample() {
        val fieldMapping = CompileTimeAnalysisUtils.getRepositoryFieldMapping(
            DemoOptimizedRepository::class.java,
            "findByIdOptimized"
        )
        
        println("[DemoOptimizedRepository] 字段映射:")
        fieldMapping.forEach { (caller, fields) ->
            println("  调用方: $caller -> 需要字段: $fields")
        }
    }
}