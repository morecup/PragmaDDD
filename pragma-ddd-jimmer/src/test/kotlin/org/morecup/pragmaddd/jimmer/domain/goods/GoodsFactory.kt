package org.morecup.pragmaddd.jimmer.domain.goods

import org.springframework.stereotype.Service

@Service
class GoodsFactory(
    private val goodsRepository: GoodsRepository
) {
    fun create(cmd: CreateGoodsCmd): Goods {
        return Goods(cmd.name, cmd.nowAddress,null, mutableListOf())
    }
}