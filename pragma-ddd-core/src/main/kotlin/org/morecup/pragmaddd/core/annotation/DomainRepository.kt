package org.morecup.pragmaddd.core.annotation

import kotlin.reflect.KClass

/**
 * 标识领域仓储的注解
 * 用于当Repository不继承DomainRepository接口时，通过注解方式指定目标聚合根类型
 * 
 * @param targetType 目标聚合根类型
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class DomainRepository(
    /**
     * 目标聚合根类型
     */
    val targetType: KClass<*> = Any::class
)