package com.example.demo.domain

import org.morecup.pragmaddd.core.annotation.AggregateRoot

/**
 * 简单测试类，用于验证属性访问检测
 */
@AggregateRoot
class SimpleTest(
    private var name: String,
    private var value: Int
) {
    
    /**
     * 简单的 getter - 应该检测到属性读取
     */
    fun getName(): String {
        return name  // 应该检测到对 name 属性的 GET 访问
    }
    
    /**
     * 简单的 setter - 应该检测到属性写入
     */
    fun setName(newName: String) {
        name = newName  // 应该检测到对 name 属性的 SET 访问
    }
    
    /**
     * 同时读取和写入属性的方法
     */
    fun updateValue(newValue: Int) {
        val oldValue = value  // 应该检测到对 value 属性的 GET 访问
        value = newValue      // 应该检测到对 value 属性的 SET 访问
    }
    
    /**
     * 读取多个属性的方法
     */
    fun getInfo(): String {
        return "Name: $name, Value: $value"  // 应该检测到对 name 和 value 属性的 GET 访问
    }
}