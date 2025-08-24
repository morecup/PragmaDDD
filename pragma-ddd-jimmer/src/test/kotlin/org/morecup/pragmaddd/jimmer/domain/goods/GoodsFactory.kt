package org.morecup.pragmaddd.jimmer.domain.goods

import org.morecup.pragmaddd.core.proxy.DomainAggregateRoot
import org.morecup.pragmaddd.core.proxy.OrmEntityConstructorConfig
import org.morecup.pragmaddd.core.proxy.aggregateRootToOrmEntityClassCache
import org.springframework.stereotype.Service

@Service
class GoodsFactory(
    private val goodsRepository: GoodsRepository
) {
    fun create(cmd: CreateGoodsCmd): Goods {
        // 获取Goods对应的ORM实体类
        val ormEntityClasses = aggregateRootToOrmEntityClassCache.get(Goods::class.java)
            ?: throw IllegalStateException("aggregateRootToOrmEntityClassCache not found for Goods")
        
        // 创建ORM实体实例
        val ormEntityList = OrmEntityConstructorConfig.constructor.createInstanceList(ormEntityClasses)
        
        // 使用DomainAggregateRoot.build创建Goods实例，确保代理系统正确初始化
        val goods = DomainAggregateRoot.build(Goods::class.java, *ormEntityList.toTypedArray())
        
        // 设置属性值
        goods.name = cmd.name
        // 注意：nowAddress1是private的，我们需要通过反射或其他方式设置
        val nowAddressField = Goods::class.java.getDeclaredField("nowAddress1")
        nowAddressField.isAccessible = true
        nowAddressField.set(goods, cmd.nowAddress)
        
        return goods
    }
}