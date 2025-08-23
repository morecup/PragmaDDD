package com.example.demo.domain

import org.morecup.pragmaddd.core.annotation.AggregateRoot

/**
 * 简单的属性访问测试类
 */
@AggregateRoot
class PropertyAccessTest(
    private var name: String,
    private var value: Int
) {
    
    /**
     * 简单的属性读取测试
     */
    fun getName(): String {
        return name  // 应该检测到 name 属性的读取
    }
    
    /**
     * 简单的属性写入测试
     */
    fun setName(newName: String) {
        name = newName  // 应该检测到 name 属性的写入
    }
    
    /**
     * 混合属性访问测试
     */
    fun updateValue(newValue: Int) {
        if (value != newValue) {  // 应该检测到 value 属性的读取
            value = newValue      // 应该检测到 value 属性的写入
        }
    }
    
    /**
     * 复杂条件测试
     */
    fun complexTest(): Boolean {
        return name.isNotEmpty() && value > 0  // 应该检测到 name 和 value 的读取
    }
}