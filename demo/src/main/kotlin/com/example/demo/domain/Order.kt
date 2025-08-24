package com.example.demo.domain

import org.morecup.pragmaddd.core.annotation.AggregateRoot
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 订单聚合根示例
 */
@AggregateRoot
open class Order(
    private var orderIds: MutableList<String>,
    open var customerId: String,
    private var totalAmount: BigDecimal,
    private var product: Product,
) {

//    fun testLambda() {
//        println(customerId)
//        val aaa = orderIds.removeIf { c -> c == "123"&&totalAmount == BigDecimal(123) }
//        println(aaa)
//    }
//
//    fun testLambda2(){
//        orderIds.removeIf {
//            listOf("123","456").toMutableList().removeIf { t -> t == "123" }
//        }
//    }
//
//    fun testLet(){
//        product.let {
//            println(it.productId)
//            println(it.getName())
//            println(totalAmount)
//        }
//    }

    fun testFieldDomainEntity(){
        println(product.productId)
        product.name
    }
    
//    fun testEmpty(){}
}