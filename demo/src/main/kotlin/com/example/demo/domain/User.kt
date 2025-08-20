package com.example.demo.domain

import org.morecup.pragmaddd.core.annotation.AggregateRoot

/**
 * 用户聚合根示例
 */
@AggregateRoot
class User(
    private var id: String,
    private var name: String,
    private var email: String,
    private var age: Int,
    private var isActive: Boolean = true
) {
    
    fun getId(): String = id
    
    fun getName(): String = name
    
    fun getEmail(): String = email
    
    fun getAge(): Int = age
    
    fun isActive(): Boolean = isActive
    
    /**
     * 更新用户信息 - 会访问和修改多个属性
     */
    fun updateProfile(newName: String, newEmail: String) {
        // 访问当前属性进行验证
        if (this.name != newName) {
            this.name = newName
        }
        
        if (this.email != newEmail) {
            this.email = newEmail
        }
    }
    
    /**
     * 激活用户 - 只修改状态属性
     */
    fun activate() {
        this.isActive = true
    }
    
    /**
     * 停用用户 - 访问和修改状态属性
     */
    fun deactivate() {
        if (this.isActive) {
            this.isActive = false
        }
    }
    
    /**
     * 增加年龄 - 访问和修改年龄属性
     */
    fun incrementAge() {
        this.age = this.age + 1
    }
    
    /**
     * 获取用户摘要 - 只访问属性，不修改
     */
    fun getSummary(): String {
        return "User(id=$id, name=$name, email=$email, age=$age, active=$isActive)"
    }
    
    /**
     * 验证用户数据 - 访问多个属性进行验证
     */
    fun validate(): Boolean {
        return name.isNotBlank() && 
               email.contains("@") && 
               age >= 0 && 
               id.isNotBlank()
    }
}