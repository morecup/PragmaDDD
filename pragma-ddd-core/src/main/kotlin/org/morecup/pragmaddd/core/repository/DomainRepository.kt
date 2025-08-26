package org.morecup.pragmaddd.core.repository

/**
 * 领域仓储基础接口
 * 
 * 提供通用的Repository接口定义，T为聚合根类型
 * 
 * 使用示例：
 * ```kotlin
 * interface GoodsRepository : DomainRepository<Goods> {
 *     fun findByIdOrErr(id: Long): Goods
 *     fun saveGoods(goods: Goods)
 * }
 * ```
 */
interface DomainRepository<T : Any> {
    
    /**
     * 根据ID查找聚合根
     * 
     * @param id 聚合根ID
     * @return 聚合根实例，如果不存在则返回null
     */
    fun findById(id: Any): T?
    
    /**
     * 保存聚合根
     * 
     * @param aggregate 要保存的聚合根
     * @return 保存后的聚合根
     */
    fun save(aggregate: T): T
    
    /**
     * 删除聚合根
     * 
     * @param aggregate 要删除的聚合根
     */
    fun delete(aggregate: T)
    
    /**
     * 根据ID删除聚合根
     * 
     * @param id 聚合根ID
     */
    fun deleteById(id: Any)
    
    /**
     * 检查聚合根是否存在
     * 
     * @param id 聚合根ID
     * @return 是否存在
     */
    fun existsById(id: Any): Boolean
}
