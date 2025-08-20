package org.morecup.pragmaddd.jimmer.domain.goods

import org.morecup.pragmaddd.core.annotation.DomainEntity
import org.morecup.pragmaddd.core.annotation.OrmField
import org.morecup.pragmaddd.core.annotation.OrmObject

@DomainEntity
@OrmObject(["address","localAddress"])
class HubeiAddress(
    override var name: String,
    override var detail: String,
    @field: OrmField("localAddress:hubeiAddressCode")
    var hubeiAddressCode: String
) : Address(name, detail) {
}