package com.example.demo.domain

import org.morecup.pragmaddd.core.annotation.AggregateRoot

/**
 * 测试用户聚合根示例 - 用于测试test源集的分析功能
 */
@AggregateRoot
class TestUser(
    private var id: String,
    private var username: String,
    private var password: String,
    private var role: String = "USER"
) {
    
    fun getId(): String = id
    
    fun getUsername(): String = username
    
    fun getRole(): String = role
    
    /**
     * 更改密码 - 访问和修改密码属性
     */
    fun changePassword(oldPassword: String, newPassword: String): Boolean {
        return if (this.password == oldPassword) {
            this.password = newPassword
            true
        } else {
            false
        }
    }
    
    /**
     * 升级角色 - 访问和修改角色属性
     */
    fun promoteToAdmin() {
        if (this.role == "USER") {
            this.role = "ADMIN"
        }
    }
    
    /**
     * 验证凭据 - 只访问属性，不修改
     */
    fun validateCredentials(inputUsername: String, inputPassword: String): Boolean {
        return this.username == inputUsername && this.password == inputPassword
    }
    
    /**
     * 重置用户 - 修改多个属性
     */
    fun reset() {
        this.password = "default"
        this.role = "USER"
    }
}