package com.example.demo.domain

import org.morecup.pragmaddd.core.annotation.AggregateRoot

/**
 * 用户聚合根示例
 * 
 * 负责管理用户的基本信息和状态，包括用户的身份验证、
 * 个人资料管理以及账户状态控制等核心业务逻辑。
 * 
 * 作为聚合根，User类确保了用户数据的一致性和完整性，
 * 并提供了统一的业务操作接口。
 * 
 * @author PragmaDDD Team
 * @version 1.0
 * @since 2024-01-01
 */
@AggregateRoot
class User(
    /**
     * 用户唯一标识符
     * 
     * 用于在系统中唯一标识一个用户实例，
     * 通常由系统自动生成或从外部系统导入。
     */
    private var id: String,
    
    /**
     * 用户姓名
     * 
     * 用户的真实姓名或显示名称，用于界面展示
     * 和业务流程中的用户识别。
     */
    private var name: String,
    
    /**
     * 用户邮箱地址
     * 
     * 用于用户登录、密码重置、系统通知等功能。
     * 必须符合邮箱格式规范且在系统中保持唯一。
     */
    private var email: String,
    
    /**
     * 用户年龄
     * 
     * 用户的当前年龄，用于年龄相关的业务逻辑判断，
     * 如权限控制、产品推荐等。
     */
    private var age: Int,
    
    /**
     * 用户激活状态
     * 
     * 标识用户账户是否处于激活状态。
     * true表示账户正常可用，false表示账户已被停用。
     */
    private var isActive: Boolean = true
) {
    
    fun getId(): String = id
    
    fun getName(): String = name
    
    fun getEmail(): String = email
    
    fun getAge(): Int = age
    
    fun isActive(): Boolean = isActive
    
    /**
     * 更新用户个人资料
     * 
     * 允许用户更新其姓名和邮箱地址。该方法会验证新值与当前值的差异，
     * 只有在值确实发生变化时才进行更新，以避免不必要的数据修改。
     * 
     * @param newName 新的用户姓名，不能为空
     * @param newEmail 新的邮箱地址，必须符合邮箱格式
     * @throws IllegalArgumentException 如果参数不符合要求
     */
    fun updateProfile(newName: String, newEmail: String) {
        require(newName.isNotBlank()) { "用户姓名不能为空" }
        require(newEmail.contains("@")) { "邮箱格式不正确" }
        
        // 访问当前属性进行验证
        if (this.name != newName) {
            this.name = newName
        }
        
        if (this.email != newEmail) {
            this.email = newEmail
        }
    }
    
    /**
     * 激活用户账户
     * 
     * 将用户账户状态设置为激活状态，激活后的用户可以正常使用系统功能。
     * 该操作通常在用户完成邮箱验证或管理员审核通过后执行。
     */
    fun activate() {
        this.isActive = true
    }
    
    /**
     * 停用用户账户
     * 
     * 将用户账户设置为停用状态。停用的账户无法登录系统，
     * 但用户数据会被保留。只有当前处于激活状态的账户才能被停用。
     * 
     * @return true如果成功停用，false如果账户已经是停用状态
     */
    fun deactivate(): Boolean {
        return if (this.isActive) {
            this.isActive = false
            true
        } else {
            false
        }
    }
    
    /**
     * 增加用户年龄
     * 
     * 将用户年龄增加1岁，通常在用户生日时调用。
     * 该方法会检查当前年龄值，确保增加后的年龄仍在合理范围内。
     * 
     * @throws IllegalStateException 如果年龄增加后超出合理范围
     */
    fun incrementAge() {
        require(this.age < 150) { "年龄不能超过150岁" }
        this.age = this.age + 1
    }
    
    /**
     * 获取用户信息摘要
     * 
     * 返回包含用户所有关键信息的字符串表示，用于日志记录、
     * 调试输出或简单的信息展示。
     * 
     * @return 用户信息的字符串表示
     */
    fun getSummary(): String {
        return "User(id=$id, name=$name, email=$email, age=$age, active=$isActive)"
    }
    
    /**
     * 验证用户数据完整性
     * 
     * 检查用户的所有必要字段是否符合业务规则要求。
     * 该方法用于确保用户数据的有效性和完整性。
     * 
     * @return true如果所有数据都有效，false如果存在无效数据
     */
    fun validate(): Boolean {
        return name.isNotBlank() && 
               email.contains("@") && 
               age >= 0 && 
               id.isNotBlank()
    }
}