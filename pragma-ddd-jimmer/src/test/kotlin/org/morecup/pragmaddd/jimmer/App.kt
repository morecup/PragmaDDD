package org.morecup.pragmaddd.jimmer

import org.babyfish.jimmer.client.EnableImplicitApi
import org.morecup.pragmaddd.core.proxy.OrmEntityConstructorConfig
import org.morecup.pragmaddd.core.proxy.OrmEntityOperatorConfig
import org.morecup.pragmaddd.core.proxy.aggregateRootToOrmEntityClassCache
import org.morecup.pragmaddd.jimmer.proxy.JimmerEntityConstructor
import org.morecup.pragmaddd.jimmer.proxy.JimmerEntityOperator
import org.morecup.pragmaddd.jimmer.admin.goods.AddressEntity
import org.morecup.pragmaddd.jimmer.admin.goods.BeijingAddressEntity
import org.morecup.pragmaddd.jimmer.admin.goods.GoodsEntity
import org.morecup.pragmaddd.jimmer.admin.goods.HubeiAddressEntity
import org.morecup.pragmaddd.jimmer.domain.goods.BeijingAddress
import org.morecup.pragmaddd.jimmer.domain.goods.Goods
import org.morecup.pragmaddd.jimmer.domain.goods.HubeiAddress
import org.springframework.boot.autoconfigure.SpringBootApplication
import javax.annotation.PostConstruct
import kotlin.jvm.java

@EnableImplicitApi
@SpringBootApplication
open class App {
    @PostConstruct
    fun init() {
        OrmEntityOperatorConfig.operator = JimmerEntityOperator()
        OrmEntityConstructorConfig.constructor = JimmerEntityConstructor()
        aggregateRootToOrmEntityClassCache.put(Goods::class.java, listOf(GoodsEntity::class.java))
        aggregateRootToOrmEntityClassCache.put(BeijingAddress::class.java, listOf(AddressEntity::class.java,BeijingAddressEntity::class.java))
        aggregateRootToOrmEntityClassCache.put(HubeiAddress::class.java, listOf(AddressEntity::class.java,HubeiAddressEntity::class.java))

    }
}