package org.morecup.pragmaddd.core.repository

/**
 * 领域仓储基础接口
 * 泛型参数T为聚合根类型
 * 
 * @param T 聚合根类型
 */
interface DomainRepository<T : Any> {
    
    /**
     * 根据ID查找聚合根
     * @param id 聚合根ID
     * @return 聚合根实例，如果不存在返回null
     */
    fun findById(id: Any): T?
    
    /**
     * 根据ID查找聚合根，如果不存在抛出异常
     * @param id 聚合根ID
     * @return 聚合根实例
     * @throws NoSuchElementException 如果聚合根不存在
     */
    fun findByIdOrThrow(id: Any): T {
        return findById(id) ?: throw NoSuchElementException("聚合根不存在: id=$id")
    }
    
    /**
     * 保存聚合根
     * @param aggregate 聚合根实例
     * @return 保存后的聚合根实例
     */
    fun save(aggregate: T): T
    
    /**
     * 删除聚合根
     * @param aggregate 聚合根实例
     */
    fun delete(aggregate: T)
    
    /**
     * 根据ID删除聚合根
     * @param id 聚合根ID
     */
    fun deleteById(id: Any)
}