package com.example.demo.domain

import org.morecup.pragmaddd.core.annotation.ValueObject

/**
 * 地址值对象示例
 * 
 * 地址作为值对象，具有不可变性和值相等性的特征。
 * 它封装了地址相关的数据和验证逻辑，确保地址信息的完整性。
 * 
 * 值对象的特点：
 * - 不可变性：一旦创建就不能修改
 * - 值相等性：基于属性值进行相等性比较
 * - 无标识：不需要唯一标识符
 * 
 * @author Pragma DDD Team
 * @since 1.0.0
 */
@ValueObject
data class Address(
    /**
     * 国家代码
     * 
     * 使用ISO 3166-1 alpha-2标准的两位国家代码。
     * 例如：CN（中国）、US（美国）、JP（日本）
     */
    val countryCode: String,
    
    /**
     * 省份或州
     * 
     * 地址中的一级行政区划名称。
     * 例如：北京市、广东省、California
     */
    val province: String,
    
    /**
     * 城市
     * 
     * 地址中的城市名称。
     * 例如：北京、深圳、Los Angeles
     */
    val city: String,
    
    /**
     * 区县
     * 
     * 地址中的区县级行政区划。
     * 例如：朝阳区、南山区、Manhattan
     */
    val district: String? = null,
    
    /**
     * 详细地址
     * 
     * 具体的街道地址信息，包括街道、门牌号等。
     * 例如：中关村大街1号、123 Main Street
     */
    val street: String,
    
    /**
     * 邮政编码
     * 
     * 地址对应的邮政编码。
     * 例如：100000、90210
     */
    val postalCode: String
) {
    
    init {
        // 值对象的不变性约束验证
        require(countryCode.isNotBlank()) { "国家代码不能为空" }
        require(countryCode.length == 2) { "国家代码必须是2位字符" }
        require(province.isNotBlank()) { "省份不能为空" }
        require(city.isNotBlank()) { "城市不能为空" }
        require(street.isNotBlank()) { "详细地址不能为空" }
        require(postalCode.isNotBlank()) { "邮政编码不能为空" }
        require(postalCode.matches(Regex("\\d{5,6}"))) { "邮政编码格式不正确" }
    }
    
    /**
     * 获取完整的格式化地址
     * 
     * 将地址各部分组合成易读的完整地址字符串。
     * 
     * @return 格式化的完整地址
     */
    fun getFullAddress(): String {
        val districtPart = district?.let { "$it, " } ?: ""
        return "$countryCode $province $city ${districtPart}$street ($postalCode)"
    }
    
    /**
     * 检查是否为国内地址
     * 
     * @return 是否为中国大陆地址
     */
    fun isDomestic(): Boolean {
        return countryCode == "CN"
    }
    
    /**
     * 获取地址摘要
     * 
     * 返回地址的简化版本，通常用于列表显示。
     * 
     * @return 地址摘要字符串
     */
    fun getSummary(): String {
        return "$province $city"
    }
    
    /**
     * 检查地址是否在同一城市
     * 
     * @param other 另一个地址
     * @return 是否在同一城市
     */
    fun isSameCity(other: Address): Boolean {
        return this.countryCode == other.countryCode &&
               this.province == other.province &&
               this.city == other.city
    }
    
    /**
     * 创建地址的副本，但使用新的详细地址
     * 
     * 由于值对象的不可变性，需要通过创建新实例来"修改"地址。
     * 
     * @param newStreet 新的详细地址
     * @param newPostalCode 新的邮政编码（可选）
     * @return 新的地址实例
     */
    fun withNewStreet(newStreet: String, newPostalCode: String = this.postalCode): Address {
        return copy(street = newStreet, postalCode = newPostalCode)
    }
}
