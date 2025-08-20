package org.morecup.pragmaddd.jimmer.domain.goods

//@DomainEntity
//@OrmObject(["address"])
abstract class Address(
    open var name:String,
    open var detail:String
) {
    // 为noarg插件添加无参构造函数
    constructor() : this("", "")
}