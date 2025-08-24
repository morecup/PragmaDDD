package com.example.demo.domain;


import org.morecup.pragmaddd.core.annotation.AggregateRoot;
import org.morecup.pragmaddd.core.annotation.OrmField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.List;

/**
 * Java订单聚合根示例
 *
 * 用于测试Java类的注解处理功能，包括：
 * - DDD注解处理
 * - 字段注解处理
 * - 可空性注解处理
 * - 注解参数收集
 *
 * @author Pragma DDD Team
 * @since 1.0.0
 */
@AggregateRoot
@SuppressWarnings("unused")
public class JavaOrder {
    
    /**
     * 订单ID列表
     * 
     * 包含所有相关的订单标识符
     */
    @NotNull
    @OrmField(columnName = "order_ids")
    private List<String> orderIds;
    
    /**
     * 客户ID
     *
     * 订单所属客户的唯一标识符
     */
    @NotNull
    private String customerId;
    
    /**
     * 订单总金额
     * 
     * 可能为空的金额字段，用于测试可空性处理
     */
    @Nullable
    @OrmField(columnName = "java_total_amount")
    private BigDecimal totalAmount;
    
    /**
     * 订单状态
     * 
     * 没有可空性注解的字段，用于测试未知状态
     */
    private String status;
    
    /**
     * 构造函数
     */
    public JavaOrder(@NotNull List<String> orderIds, 
                     @NotNull String customerId, 
                     @Nullable BigDecimal totalAmount,
                     String status) {
        this.orderIds = orderIds;
        this.customerId = customerId;
        this.totalAmount = totalAmount;
        this.status = status;
    }
    
    /**
     * 获取订单ID列表
     * 
     * @return 订单ID列表
     */
    @NotNull
    public List<String> getOrderIds() {
        return orderIds;
    }
    
    /**
     * 获取客户ID
     * 
     * @return 客户ID
     */
    @NotNull
    public String getCustomerId() {
        return customerId;
    }
    
    /**
     * 获取订单总金额
     * 
     * @return 订单总金额，可能为null
     */
    @Nullable
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
    
    /**
     * 设置订单总金额
     * 
     * @param totalAmount 新的总金额
     */
    public void setTotalAmount(@Nullable BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
    
    /**
     * 获取订单状态
     * 
     * @return 订单状态
     */
    public String getStatus() {
        return status;
    }
    
    /**
     * 计算订单总价
     * 
     * 演示方法级别的属性访问分析
     * 
     * @return 计算后的总价
     */
    public BigDecimal calculateTotal() {
        if (totalAmount == null) {
            return BigDecimal.ZERO;
        }
        return totalAmount.multiply(new BigDecimal("1.1")); // 加10%税费
    }
}
