package org.morecup.pragmaddd.core.annotation

import kotlin.reflect.KClass

/**
 * 领域仓储注解
 * 
 * 用于标注Repository接口，指定其对应的聚合根类型
 * 
 * 使用示例：
 * ```kotlin
 * @DomainRepository(aggregateRoot = Goods::class)
 * interface GoodsRepository {
 *     fun findByIdOrErr(id: Long): Goods
 *     fun saveGoods(goods: Goods)
 * }
 * ```
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class DomainRepository(
    /**
     * 对应的聚合根类型
     */
    val aggregateRoot: KClass<*> = Nothing::class
)
