package org.morecup.pragmaddd.jimmer.domain.goods

import org.morecup.pragmaddd.core.annotation.DomainEntity
import org.morecup.pragmaddd.core.annotation.OrmField
import org.morecup.pragmaddd.core.annotation.OrmObject

@DomainEntity
@OrmObject(["address","localAddress"])
class BeijingAddress(
    override var name: String,
    override var detail: String,
    @field: OrmField("localAddress:beijingAddressCode")
    var beijingAddressCode: String
) : Address(name,detail) {
    fun changeName(newName: String) {
        this.name = newName
    }
}