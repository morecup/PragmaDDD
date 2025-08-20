package org.morecup.pragmaddd.jimmer.admin.goods

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.OneToMany
import org.babyfish.jimmer.sql.Table
import org.morecup.pragmaddd.jimmer.admin.BaseEntity

@Entity
@Table(name = "goods")
interface GoodsEntity : BaseEntity {
    val name: String
    val nowAddress: String

    @OneToMany(mappedBy = "goodsEntity")
    val addressEntity:List<AddressEntity>
}