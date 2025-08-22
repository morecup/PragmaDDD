package com.example.demo.domain

import org.morecup.pragmaddd.core.annotation.AggregateRoot

/**
 * 测试用户聚合根 - 使用内部注解
 */
@AggregateRoot
class TestUser(
    private var id: String,
    private var name: String,
    private var email: String
) {
    
    fun getId(): String = id
    
    fun getName(): String = name
    
    fun getEmail(): String = email
    
    /**
     * 更新用户信息
     */
    fun updateProfile(newName: String, newEmail: String) {
        if (newName.isNotBlank()) {
            this.name = newName
        }
        
        if (newEmail.contains("@")) {
            this.email = newEmail
        }
    }
    
    /**
     * 获取用户摘要
     */
    fun getSummary(): String {
        return "TestUser(id=$id, name=$name, email=$email)"
    }
}